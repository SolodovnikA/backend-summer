package ru.shift.userimporter.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.shift.userimporter.core.exception.ResourceNotFoundException;
import ru.shift.userimporter.core.model.*;
import ru.shift.userimporter.core.repository.UploadedFileRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingRunner {
    private static final String NAME_PATTERN = "^[А-Я][а-я'\\- ]{2,49}$";
    private static final String EMAIL_PATTERN = "^\\w[\\w.+-]*@[\\w-]+\\.[a-zA-Z]{2,}$";
    private static final String PHONE_PATTERN  = "^7\\d{10}$";

    private final UploadedFileRepository uploadedFileRepository;
    private final FileProcessingErrorService fileProcessingErrorService;
    private final UserService userService;


    @Async
    public void runAsync(Long fileId) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл с ID " + fileId + " не найден"));

        try {
            AtomicInteger totalRows = new AtomicInteger(0);
            AtomicInteger insertedRows = new AtomicInteger(0);
            AtomicInteger updatedRows = new AtomicInteger(0);
            AtomicInteger invalidRows = new AtomicInteger(0);

            Map<String, User> usersByPhone = new HashMap<>();
            List<FileProcessingError> errorsToSave = new ArrayList<>();

            try (Stream<String> lines = Files.lines(Path.of(uploadedFile.getStoragePath()),
                    StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                    int rowNumber = totalRows.incrementAndGet();
                    String[] fields = line.split(",");

                    RowValidationResult validation = validateRow(fields);
                    if (!validation.valid()) {
                        errorsToSave.add(fileProcessingErrorService.buildError(uploadedFile, rowNumber,
                                validation.errorMessage(), validation.errorCode()));
                        invalidRows.incrementAndGet();
                        return;
                    }

                    String phone = fields[4];
                    User user = usersByPhone.get(phone);
                    boolean isUpdate;
                    if (user != null) {
                        isUpdate = true;
                    } else {
                        Optional<User> existingUser = userService.findByPhone(phone);
                        user = existingUser.orElseGet(User::new);
                        isUpdate = existingUser.isPresent();
                    }

                    if (isUpdate) {
                        updatedRows.incrementAndGet();
                    } else {
                        insertedRows.incrementAndGet();
                    }

                    user.setFirstName(fields[0]);
                    user.setLastName(fields[1]);
                    user.setMiddleName(fields[2].isEmpty() ? null : fields[2]);
                    user.setEmail(fields[3]);
                    user.setPhone(phone);
                    user.setBirthDate(validation.birthDate());

                    usersByPhone.put(phone, user);
                });
            } catch (IOException e) {
                throw new RuntimeException("Не удалось прочитать файл", e);
            }

            userService.saveUsers(usersByPhone.values());
            fileProcessingErrorService.saveErrors(errorsToSave);

            int total = totalRows.get();
            int invalid = invalidRows.get();
            int valid = total - invalid;

            uploadedFile.setTotalRows(total);
            uploadedFile.setProcessedRows(total);
            uploadedFile.setUpdatedRows(updatedRows.get());
            uploadedFile.setInsertedRows(insertedRows.get());
            uploadedFile.setInvalidRows(invalid);
            uploadedFile.setValidRows(valid);
            uploadedFile.setStatus(FileStatus.DONE);

            uploadedFileRepository.save(uploadedFile);
        } catch (Exception e) {
            log.error("Ошибка при обработке файла с ID {}", fileId, e);
            uploadedFile.setStatus(FileStatus.FAILED);
            uploadedFileRepository.save(uploadedFile);
        }
    }

    private RowValidationResult validateRow(String[] fields) {
        if (fields.length != 6) {
            return RowValidationResult.invalid(ErrorCode.INVALID_FORMAT, "Неверное количество полей в строке");
        }
        if (!fields[0].matches(NAME_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_NAME, "Неверный формат имени!");
        }
        if (!fields[1].matches(NAME_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_LAST_NAME, "Неверный формат фамилии!");
        }
        if (!fields[2].isEmpty() && !fields[2].matches(NAME_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_MIDDLE_NAME, "Неверный формат отчества!");
        }
        if (!fields[3].matches(EMAIL_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_EMAIL, "Неверный формат почты!");
        }
        if (!fields[3].endsWith("@shift.ru") && !fields[3].endsWith("@shift.com")) {
            return RowValidationResult.invalid(ErrorCode.INVALID_EMAIL, "Неверный формат почты!");
        }
        if (!fields[4].matches(PHONE_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_PHONE, "Неверный формат номера телефона!");
        }
        try {
            LocalDate birthDate = LocalDate.parse(fields[5]);
            Period period = Period.between(birthDate, LocalDate.now());
            if (period.getYears() < 18) {
                return RowValidationResult.invalid(ErrorCode.INVALID_BIRTHDATE,
                        "Пользователь младше 18-ти лет");
            }
            return RowValidationResult.valid(birthDate);
        } catch (DateTimeParseException e) {
            return RowValidationResult.invalid(ErrorCode.INVALID_BIRTHDATE,
                    "Неверный формат даты рождения!");
        }
    }
}

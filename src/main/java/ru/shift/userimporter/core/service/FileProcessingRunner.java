package ru.shift.userimporter.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.shift.userimporter.core.model.*;
import ru.shift.userimporter.core.repository.UploadedFileRepository;
import ru.shift.userimporter.core.validator.RowValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingRunner {

    private final UploadedFileRepository uploadedFileRepository;
    private final FileProcessingErrorService fileProcessingErrorService;
    private final UserService userService;

    @Lazy
    private final UploadedFileService uploadedFileService;


    @Async
    public void runAsync(Long fileId) {
        UploadedFile uploadedFile =  uploadedFileService.getOrThrow(fileId);

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

                    RowValidationResult validation = RowValidator.validateRow(fields);
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

}

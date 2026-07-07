package ru.shift.userimporter.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import ru.shift.userimporter.api.dto.FileStatistic;
import ru.shift.userimporter.core.exception.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ru.shift.userimporter.api.dto.FileIdResponse;
import ru.shift.userimporter.core.exception.ResourceNotFoundException;
import ru.shift.userimporter.core.model.*;
import ru.shift.userimporter.core.repository.FileProcessingErrorRepository;
import ru.shift.userimporter.core.repository.UploadedFileRepository;
import ru.shift.userimporter.core.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UploadedFileService {
    private static final String NAME_PATTERN = "^[А-Я][а-я'\\- ]{2,49}$";
    private static final String EMAIL_PATTERN = "^\\w[\\w.+-]*@[\\w-]+\\.[a-zA-Z]{2,}$";
    private static final String PHONE_PATTERN = "^7\\d{10}$";

    private final UploadedFileRepository uploadedFileRepository;
    private final FileProcessingErrorRepository fileProcessingErrorRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;


    public FileIdResponse uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e){
            throw new IllegalArgumentException("Алгоритм SHA-1 недоступен", e);
        }

        try(DigestInputStream digestInputStream = new DigestInputStream(file.getInputStream(),
                digest)) {
            digestInputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось получить содержимое файла", e);
        }
        String hash = HexFormat.of().formatHex(digest.digest());

        if (uploadedFileRepository.existsByHash(hash)) {
            throw new FileAlreadyExistsException(file.getOriginalFilename());
        }
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Path.of(uploadDir).resolve(fileName);
        Path uploadsAbsolute = Path.of(uploadDir).toAbsolutePath().normalize();
        Path fileAbsolute = path.toAbsolutePath().normalize();
        if (!fileAbsolute.startsWith(uploadsAbsolute)) {
            throw new IllegalArgumentException("Файл лежит не там");
        }

        try {
            Files.createDirectories(path.getParent());
            file.transferTo(path);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл", e);
        }

        UploadedFile uploadedFile = UploadedFile.builder()
                .originalFileName(file.getOriginalFilename())
                .storagePath(path.toString())
                .status(FileStatus.NEW)
                .hash(hash)
                .build();


        UploadedFile saved = uploadedFileRepository.save(uploadedFile);
        return new FileIdResponse(String.valueOf(saved.getId()));

    }

    private FileProcessingError buildError(UploadedFile uploadedFile, Integer rowNumber, String errorMessage,
                                           ErrorCode errorCode) {
        FileProcessingError error = new FileProcessingError();
        error.setUploadedFile(uploadedFile);
        error.setRowNumber(rowNumber);
        error.setErrorMessage(errorMessage);
        error.setErrorCode(errorCode);

        return error;

    }

    public void processFile(Long fileId) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(fileId).
                orElseThrow(() -> new ResourceNotFoundException("Файл с ID " + fileId + " не найден"));


        AtomicInteger totalRows = new AtomicInteger(0);
        AtomicInteger insertedRows = new AtomicInteger(0);
        AtomicInteger updatedRows = new AtomicInteger(0);
        AtomicInteger invalidRows = new AtomicInteger(0);

        Map<String, User> usersByPhone = new HashMap<>();
        List<FileProcessingError> errorsToSave = new ArrayList<>();

        try (Stream<String> lines = Files.lines(Path.of(uploadedFile.getStoragePath()), StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                int rowNumber = totalRows.incrementAndGet();
                String[] fields = line.split(",");

                RowValidationResult validation = validateRow(fields);
                if (!validation.valid()) {
                    errorsToSave.add(buildError(uploadedFile, rowNumber, validation.errorMessage(),
                            validation.errorCode()));
                    invalidRows.incrementAndGet();
                    return;
                }

                String phone = fields[4];
                User user = usersByPhone.get(phone);
                boolean isUpdate;
                if (user != null) {
                    isUpdate = true;
                } else {
                    Optional<User> existingUser = userRepository.findByPhone(phone);
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

        userRepository.saveUsers(usersByPhone.values());
        fileProcessingErrorRepository.saveErrors(errorsToSave);

        int total = totalRows.get();
        int invalid = invalidRows.get();
        int valid = total - invalid;

        uploadedFile.setTotalRows(total);
        uploadedFile.setProcessedRows(total);
        uploadedFile.setInsertedRows(insertedRows.get());
        uploadedFile.setUpdatedRows(updatedRows.get());
        uploadedFile.setInvalidRows(invalid);
        uploadedFile.setValidRows(valid);
        uploadedFile.setStatus(FileStatus.DONE);

        uploadedFileRepository.save(uploadedFile);

    }

    private RowValidationResult validateRow(String [] fields) {

        if (fields.length != 6) {
            return RowValidationResult.invalid(ErrorCode.INVALID_FORMAT,
                    "Неверное количество полей в строке");
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
            return RowValidationResult.invalid(ErrorCode.INVALID_PHONE,
                    "Неверный формат номера телефона!");
        }
        try {
            LocalDate birthDate = LocalDate.parse(fields[5]);
            Period period = Period.between(birthDate, LocalDate.now());
            if (period.getYears() < 18) {
                return RowValidationResult.invalid(ErrorCode.INVALID_BIRTHDATE, "Пользователь младше 18-ти лет");
            }
            return RowValidationResult.valid(birthDate);
        } catch (DateTimeParseException e) {
            return RowValidationResult.invalid(ErrorCode.INVALID_BIRTHDATE, "Неверный формат даты рождения!");
        }
    }

    public FileStatistic getStatistics() {
        int inserted = uploadedFileRepository.sumInsertedRows().intValue();
        int updated = uploadedFileRepository.sumUpdatedRows().intValue();
        int invalid = uploadedFileRepository.sumInvalidRows().intValue();

        return new FileStatistic(inserted, updated, invalid);
    }

}

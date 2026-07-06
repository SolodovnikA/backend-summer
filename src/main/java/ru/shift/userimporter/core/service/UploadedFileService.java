package ru.shift.userimporter.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

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


    public Long uploadFile(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(file.getBytes()));

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
        Files.createDirectories(path.getParent());
        file.transferTo(path);

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setOriginalFileName(file.getOriginalFilename());
        uploadedFile.setStoragePath(path.toString());
        uploadedFile.setStatus(FileStatus.NEW);
        uploadedFile.setHash(hash);

        UploadedFile saved = uploadedFileRepository.save(uploadedFile);
        return saved.getId();

    }

    @Transactional
    public void processFile(Long fileId) throws IOException {
        UploadedFile uploadedFile = uploadedFileRepository.findById(fileId).
                orElseThrow(() -> new ResourceNotFoundException("Файл с ID " + fileId + " не найден"));

        List<String> lines = Files.readAllLines(Path.of(uploadedFile.getStoragePath()), StandardCharsets.UTF_8);

        int totalRows = lines.size();
        int processedRows = totalRows;
        int insertedRows = 0;
        int updatedRows = 0;
        int invalidRows = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int rowNumber = i + 1;
            String[] fields = line.split(",");
            if (fields.length != 6) {
                saveError(uploadedFile, rowNumber, "Неверное количество полей в строке",
                        ErrorCode.INVALID_FORMAT);
                invalidRows++;
                continue;
            }
            if (!fields[0].matches(NAME_PATTERN)) {
                saveError(uploadedFile, rowNumber, "Неверный формат имени!",
                        ErrorCode.INVALID_NAME);
                invalidRows++;
                continue;
            }
            if (!fields[1].matches(NAME_PATTERN)) {
                saveError(uploadedFile, rowNumber, "Неверный формат фамилии!",
                        ErrorCode.INVALID_LAST_NAME);
                invalidRows++;
                continue;
            }
            if (!fields[2].isEmpty() && !fields[2].matches(NAME_PATTERN)) {
                saveError(uploadedFile, rowNumber, "Неверный формат отчества!",
                        ErrorCode.INVALID_MIDDLE_NAME);
                invalidRows++;
                continue;
            }
            if (!fields[3].matches(EMAIL_PATTERN)) {
                saveError(uploadedFile, rowNumber, "Неверный формат почты!",
                        ErrorCode.INVALID_EMAIL);
                invalidRows++;
                continue;
            }
            if (!fields[3].endsWith("@shift.ru") && !fields[3].endsWith("@shift.com")) {
                saveError(uploadedFile, rowNumber, "Неверный формат почты!",
                        ErrorCode.INVALID_EMAIL);
                invalidRows++;
                continue;
            }
            if (!fields[4].matches(PHONE_PATTERN)) {
                saveError(uploadedFile, rowNumber, "Неверный формат номера телефона!",
                        ErrorCode.INVALID_PHONE);
                invalidRows++;
                continue;
            }
            LocalDate birthDate;
            try {
                birthDate = LocalDate.parse(fields[5]);
                Period period = Period.between(birthDate, LocalDate.now());
                int age = period.getYears();
                if (age < 18) {
                    saveError(uploadedFile, rowNumber, "Пользователь младше 18-ти лет",
                            ErrorCode.INVALID_BIRTHDATE);
                    invalidRows++;
                    continue;
                }
            } catch (DateTimeParseException e) {
                saveError(uploadedFile, rowNumber, "Неверный формат даты рождения!",
                        ErrorCode.INVALID_BIRTHDATE);
                invalidRows++;
                continue;

            }

            Optional<User> existingUser  = userRepository.findByPhone(fields[4]);
            User user = existingUser.orElseGet(User::new);
            boolean isUpdate = existingUser.isPresent();
            if (isUpdate) {
                updatedRows++;
            } else {
                insertedRows++;
            }

            user.setFirstName(fields[0]);
            user.setLastName(fields[1]);
            user.setMiddleName(fields[2]);
            user.setEmail(fields[3]);
            user.setPhone(fields[4]);
            user.setBirthDate(birthDate);

            userRepository.save(user);
        }
        int validRows = totalRows - invalidRows;
        uploadedFile.setTotalRows(totalRows);
        uploadedFile.setProcessedRows(processedRows);
        uploadedFile.setInsertedRows(insertedRows);
        uploadedFile.setUpdatedRows(updatedRows);
        uploadedFile.setInvalidRows(invalidRows);
        uploadedFile.setValidRows(validRows);
        uploadedFile.setStatus(FileStatus.DONE);

        uploadedFileRepository.save(uploadedFile);

    }

    private void saveError(UploadedFile uploadedFile, Integer rowNumber, String errorMessage,
                           ErrorCode errorCode) {
        FileProcessingError error = new FileProcessingError();
        error.setUploadedFile(uploadedFile);
        error.setRowNumber(rowNumber);
        error.setErrorMessage(errorMessage);
        error.setErrorCode(errorCode);

        fileProcessingErrorRepository.save(error);

    }
}

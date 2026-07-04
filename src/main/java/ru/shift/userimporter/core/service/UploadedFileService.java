package ru.shift.userimporter.core.service;

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
import java.util.UUID;
import java.util.List;

import ru.shift.userimporter.core.exception.ResourceNotFoundException;
import ru.shift.userimporter.core.model.FileStatus;
import ru.shift.userimporter.core.model.UploadedFile;
import ru.shift.userimporter.core.repository.FileProcessingErrorRepository;
import ru.shift.userimporter.core.repository.UploadedFileRepository;

@Service
public class UploadedFileService {
    private final UploadedFileRepository uploadedFileRepository;
    private final FileProcessingErrorRepository fileProcessingErrorRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public UploadedFileService(UploadedFileRepository uploadedFileRepository,
                               FileProcessingErrorRepository fileProcessingErrorRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.fileProcessingErrorRepository = fileProcessingErrorRepository;
    }

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
        String namePattern = "^[А-Я][а-я'\\- ]{2,49}$";
        String emailPattern = "^\\w[\\w.+-]*@[\\w-]+\\.[a-zA-Z]{2,}$";
        String phonePattern = "^7\\d{10}$";
        for (String line : lines) {
            String[] fields = line.split(",");
            if (fields.length != 6) {
                System.out.println("Ошибка в строке: " + line);
                continue;
            }
            if (!fields[0].matches(namePattern)) {
                System.out.println("Ошибка в имени!");
                continue;
            }
            if (!fields[1].matches(namePattern)) {
                System.out.println("Ошибка в фамилии!");
                continue;
            }
            if (!fields[2].isEmpty() && !fields[2].matches(namePattern)) {
                System.out.println("Ошибка в отчестве!");
                continue;
            }
            if (!fields[3].matches(emailPattern)) {
                System.out.println("Ошибка в формате почты!");
                continue;
            }
            if (!fields[3].endsWith("@shift.ru") && !fields[3].endsWith("@shift.com")) {
                System.out.println("Ошибка в домене почты!");
                continue;
            }
            if (!fields[4].matches(phonePattern)) {
                System.out.println("Ошибка в номере телефона!");
                continue;
            }
            LocalDate birthDate;
            try {
                birthDate = LocalDate.parse(fields[5]);
                Period period = Period.between(birthDate, LocalDate.now());
                int age = period.getYears();
                if (age < 18) {
                    System.out.println("Возраст меньше 18-ти лет");
                    continue;
                }
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка в дате рождения!");
                continue;

            }
        }

    }
}

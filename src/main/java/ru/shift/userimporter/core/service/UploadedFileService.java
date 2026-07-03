package ru.shift.userimporter.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import ru.shift.userimporter.core.model.UploadedFile;
import ru.shift.userimporter.core.repository.UploadedFileRepository;

@Service
public class UploadedFileService {
    private final UploadedFileRepository uploadedFileRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public UploadedFileService(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    public Long uploadFile(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(file.getBytes()));

        if (uploadedFileRepository.existsByHash(hash)) {
            throw new IllegalArgumentException("Файл уже существует");
        }
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Path.of(uploadDir).resolve(fileName);
        Files.createDirectories(path.getParent());
        file.transferTo(path);

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setOriginalFileName(file.getOriginalFilename());
        uploadedFile.setStoragePath(path.toString());
        uploadedFile.setStatus("NEW");
        uploadedFile.setHash(hash);

        UploadedFile saved = uploadedFileRepository.save(uploadedFile);
        return saved.getId();

    }
}

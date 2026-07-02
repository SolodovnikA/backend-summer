package ru.shift.userimporter.core.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.shift.userimporter.core.repository.UploadedFileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import ru.shift.userimporter.core.model.UploadedFile;

@Service
public class UploadedFileService {
    private final UploadedFileRepository uploadedFileRepository;

    public UploadedFileService(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    public Long uploadFile(MultipartFile file) throws IOException {
        String storagePath = "uploads/"+ UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(storagePath);
        Files.createDirectories(path.getParent());
        file.transferTo(path);

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setOriginalFileName(file.getOriginalFilename());
        uploadedFile.setStoragePath(storagePath);
        uploadedFile.setStatus("NEW");

        UploadedFile saved = uploadedFileRepository.save(uploadedFile);
        return saved.getId();
    }
}

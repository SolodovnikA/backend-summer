package ru.shift.userimporter.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import ru.shift.userimporter.api.dto.*;
import ru.shift.userimporter.core.exception.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import ru.shift.userimporter.core.exception.ResourceNotFoundException;
import ru.shift.userimporter.core.model.*;
import ru.shift.userimporter.core.repository.FileProcessingErrorRepository;
import ru.shift.userimporter.core.repository.UploadedFileRepository;
import ru.shift.userimporter.core.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UploadedFileService {

    private final UploadedFileRepository uploadedFileRepository;
    private final FileProcessingErrorRepository fileProcessingErrorRepository;
    private final UserRepository userRepository;
    private final FileProcessingRunner fileProcessingRunner;

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


    public void processFile(Long fileId) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл с ID " + fileId + " не найден"));

        if (uploadedFile.getStatus() == FileStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Файл уже находится в обработке");
        }

        uploadedFile.setStatus(FileStatus.IN_PROGRESS);
        uploadedFileRepository.save(uploadedFile);

        fileProcessingRunner.runAsync(fileId);
    }


    private FileResponse toFileResponse(UploadedFile file) {
        FileStatistic statistic = new FileStatistic(
                file.getInsertedRows() == null ? 0 : file.getInsertedRows(),
                file.getUpdatedRows() == null ? 0 : file.getUpdatedRows(),
                file.getInvalidRows() == null ? 0 : file.getInvalidRows()
        );
        return new FileResponse(String.valueOf(file.getId()), file.getStatus().name(), statistic);
    }

    public List<FileResponse> getFiles(FileStatus status) {
        List<UploadedFile> files = status != null
                ? uploadedFileRepository.findByStatus(status)
                : uploadedFileRepository.findAll();

        return files.stream()
                .map(this :: toFileResponse)
                .toList();
    }

    private ProcessingError toProcessingError(FileProcessingError error) {
        return new ProcessingError(error.getRowNumber(), error.getErrorCode().name(), error.getErrorMessage());
    }

    public DetailedFileStatistic getDetailedStatistic(Long fileId) {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл с ID " + fileId + " не найден"));

        List<ProcessingError> errors = fileProcessingErrorRepository.findByUploadedFileId(fileId).stream()
                .map(this::toProcessingError)
                .toList();

        int inserted = file.getInsertedRows() == null ? 0 : file.getInsertedRows();
        int updated = file.getUpdatedRows() == null ? 0 : file.getUpdatedRows();

        return new DetailedFileStatistic(inserted, updated, errors);
    }



}

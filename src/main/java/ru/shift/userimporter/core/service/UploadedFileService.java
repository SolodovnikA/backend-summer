package ru.shift.userimporter.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import ru.shift.userimporter.api.dto.*;
import ru.shift.userimporter.api.mapper.FileMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import ru.shift.userimporter.core.exception.AppErrorCode;
import ru.shift.userimporter.core.exception.AppException;
import ru.shift.userimporter.core.model.*;
import ru.shift.userimporter.core.repository.FileProcessingErrorRepository;
import ru.shift.userimporter.core.repository.UploadedFileRepository;

@Service
@RequiredArgsConstructor
public class UploadedFileService {

    private final UploadedFileRepository uploadedFileRepository;
    private final FileProcessingErrorRepository fileProcessingErrorRepository;
    private final FileProcessingRunner fileProcessingRunner;

    @Value("${app.upload.dir}")
    private String uploadDir;


    public FileIdResponse uploadFile(MultipartFile file) {

       validateFile(file);
       String hash = computeHash(file);

        if (uploadedFileRepository.existsByHash(hash)) {
            throw new AppException(AppErrorCode.FILE_ALREADY_EXISTS, "Файл с таким содержимым уже существует");
        }

        Path path = storeFile(file);
        UploadedFile saved = saveFileRecord(file, path, hash);

        return new FileIdResponse(String.valueOf(saved.getId()));

    }


    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }
    }


    private String computeHash(MultipartFile file) {
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
        return HexFormat.of().formatHex(digest.digest());
    }


    public UploadedFile getOrThrow(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new AppException(AppErrorCode.RESOURCE_NOT_FOUND,
                        "Файл с ID " + fileId + " не найден"));
    }


    private Path storeFile(MultipartFile file) {
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
        return path;

    }


    private UploadedFile saveFileRecord(MultipartFile file, Path path, String hash) {
        UploadedFile uploadedFile = UploadedFile.builder()
                .originalFileName(file.getOriginalFilename())
                .storagePath(path.toString())
                .status(FileStatus.NEW)
                .hash(hash)
                .build();

        return uploadedFileRepository.save(uploadedFile);

    }


    public void processFile(Long fileId) {
        UploadedFile uploadedFile = getOrThrow(fileId);

        if (uploadedFile.getStatus() == FileStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Файл уже находится в обработке");
        }

        markAsInProgress(uploadedFile);

        fileProcessingRunner.runAsync(uploadedFile);
    }


    public List<FileResponse> getFiles(FileStatus status) {
        List<UploadedFile> files = status != null
                ? uploadedFileRepository.findByStatus(status)
                : uploadedFileRepository.findAll();

        return files.stream()
                .map(FileMapper::toFileResponse)
                .toList();
    }


    public DetailedFileStatistic getDetailedStatistic(Long fileId) {
        UploadedFile file = getOrThrow(fileId);

        List<ProcessingError> errors = fileProcessingErrorRepository.findByUploadedFileId(fileId).stream()
                .map(FileMapper::toProcessingError)
                .toList();

        int inserted = file.getInsertedRows() == null ? 0 : file.getInsertedRows();
        int updated = file.getUpdatedRows() == null ? 0 : file.getUpdatedRows();

        return new DetailedFileStatistic(inserted, updated, errors);
    }

    private void markAsInProgress(UploadedFile uploadedFile) {
        uploadedFile.setStatus(FileStatus.IN_PROGRESS);
        uploadedFileRepository.save(uploadedFile);
    }
}

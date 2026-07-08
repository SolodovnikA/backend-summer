package ru.shift.userimporter.core.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final UserService userService;
    private final FileProcessingErrorService fileProcessingErrorService;

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

        uploadedFile.setStatus(FileStatus.IN_PROGRESS);
        uploadedFileRepository.save(uploadedFile);

        fileProcessingRunner.runAsync(fileId);
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


    @Transactional
    public void completeProcessing(Long fileId, Collection<User> users, List<FileProcessingError> errors,
                                   int total, int inserted, int updated, int invalid) {

        UploadedFile uploadedFile = getOrThrow(fileId);
        userService.saveUsers(users);
        fileProcessingErrorService.saveErrors(errors);

        int valid = total - invalid;
        uploadedFile.setTotalRows(total);
        uploadedFile.setProcessedRows(total);
        uploadedFile.setUpdatedRows(updated);
        uploadedFile.setInsertedRows(inserted);
        uploadedFile.setInvalidRows(invalid);
        uploadedFile.setValidRows(valid);
        uploadedFile.setStatus(FileStatus.DONE);
        uploadedFileRepository.save(uploadedFile);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long fileId) {
        UploadedFile uploadedFile = getOrThrow(fileId);
        uploadedFile.setStatus(FileStatus.FAILED);
        uploadedFileRepository.save(uploadedFile);
    }
}

package ru.shift.userimporter.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.ArgumentMatchers.any;

import ru.shift.userimporter.api.dto.FileIdResponse;
import ru.shift.userimporter.core.exception.AppErrorCode;
import ru.shift.userimporter.core.exception.AppException;
import ru.shift.userimporter.core.model.FileStatus;
import ru.shift.userimporter.core.model.UploadedFile;
import ru.shift.userimporter.core.repository.UploadedFileRepository;
import ru.shift.userimporter.core.util.FileStorage;
import ru.shift.userimporter.core.util.HashCalculator;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UploadedFileServiceTest {

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @InjectMocks
    private UploadedFileService uploadedFileService;

    @Mock
    private FileStorage fileStorage;


    @Test
    void getOrThrow_shouldReturnFile_ifExists() {
        UploadedFile expectedFile = UploadedFile.builder()
                .id(1L)
                .originalFileName("test.csv")
                .storagePath("/uploads/test.csv")
                .status(FileStatus.NEW)
                .hash("qwert12345")
                .build();

        when(uploadedFileRepository.findById(1L)).thenReturn(Optional.of(expectedFile));
        UploadedFile result = uploadedFileService.getOrThrow(1L);

        assertEquals(expectedFile, result);

    }

    @Test
    void getOrThrow_shouldThrowAppException_ifNonExistent() {
        when(uploadedFileRepository.findById(1L)).thenReturn(Optional.empty());
        AppException exception = assertThrows(AppException.class, () ->
                uploadedFileService.getOrThrow(1L));

        assertEquals(AppErrorCode.RESOURCE_NOT_FOUND, exception.getAppErrorCode());
    }

    @Test
    void uploadFile_shouldThrowAppException_ifAlreadyExists() {
        MockMultipartFile file = new MockMultipartFile(
                "test.csv",
                "test1_csv",
                "text/csv",
                "content A".getBytes()
        );

        String hash = HashCalculator.computeHash(file);
        when(uploadedFileRepository.existsByHash(hash)).thenReturn(true);

        assertThrows(AppException.class, () -> uploadedFileService.uploadFile(file));
    }

    @Test
    void uploadFile_shouldReturnFileIdResponse_ifNewFile() {
        MockMultipartFile file = new MockMultipartFile(
                "test.csv", "test1_csv", "text/csv", "content A".getBytes());

        String hash = HashCalculator.computeHash(file);
        when(uploadedFileRepository.existsByHash(hash)).thenReturn(false);

        Path fakePath = Path.of("/uploads/test.scv");
        when(fileStorage.storeFile(file)).thenReturn(fakePath);

        UploadedFile savedFile = UploadedFile.builder()
                .id(1L)
                .originalFileName("test.scv")
                .storagePath(fakePath.toString())
                .status(FileStatus.NEW)
                .hash(hash)
                .build();
        when(uploadedFileRepository.save(any(UploadedFile.class))).thenReturn(savedFile);

        FileIdResponse response = uploadedFileService.uploadFile(file);

        assertEquals("1", response.fileId());
    }

    @Test
    void uploadFile_shouldThrowIllegalArgumentException_ifEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "test.csv", "test1_csv", "text/csv", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> uploadedFileService.uploadFile(file));

    }
}

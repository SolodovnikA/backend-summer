package ru.shift.userimporter.core.service;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.shift.userimporter.core.model.*;
import ru.shift.userimporter.core.repository.UploadedFileRepository;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileProcessingRunnerTest {

    @Mock
    private FileProcessingErrorService fileProcessingErrorService;

    @Mock
    private UserService userService;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @InjectMocks
    private FileProcessingRunner fileProcessingRunner;

    @TempDir
    Path tempDir;

    @Test
    void runAsync_shouldCompleteProcessing_ifRowIsValid() throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, "Иван,Иванов,Иванович,ivan@shift.ru,79995551122,1995-03-14\n");

        UploadedFile uploadedFile = UploadedFile.builder()
                .id(1L)
                .storagePath(csvFile.toString())
                .build();

        when(userService.findByPhone("79995551122")).thenReturn(Optional.empty());

        fileProcessingRunner.runAsync(uploadedFile);

        verify(uploadedFileRepository).save(any(UploadedFile.class));
        verify(userService).saveUsers(any());
    }

    @Test
    void runAsync_shouldCountInvalidRows_ifPresent() throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, "иван,Иванов,Иванович,ivan@shift.ru,79995551122,1995-03-14\n");

        UploadedFile uploadedFile = UploadedFile.builder()
                .id(1L)
                .storagePath(csvFile.toString())
                .build();

        FileProcessingError testError = FileProcessingError.builder()
                .uploadedFile(uploadedFile)
                .rowNumber(1)
                .errorMessage("Неверный формат имени!")
                .errorCode(ErrorCode.INVALID_NAME)
                .build();

        when(fileProcessingErrorService.buildError(any(), anyInt(), anyString(), eq(ErrorCode.INVALID_NAME)))
                .thenReturn(testError);

        fileProcessingRunner.runAsync(uploadedFile);

        verify(fileProcessingErrorService).buildError(any(), anyInt(), anyString(), eq(ErrorCode.INVALID_NAME));
        verify(uploadedFileRepository).save(any(UploadedFile.class));

    }

    @Test
    void runAsync_shouldUpdateExistingUser_ifPhoneMatches() throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, "Иван,Иванов,Иванович,ivan@shift.ru,79995551122,1995-03-14\n");

        UploadedFile uploadedFile = UploadedFile.builder()
                .id(1L)
                .storagePath(csvFile.toString())
                .build();

        User existingUser = User.builder()
                .phone("79995551122")
                .build();

        when(userService.findByPhone("79995551122")).thenReturn(Optional.of(existingUser));
        fileProcessingRunner.runAsync(uploadedFile);

        verify(userService).saveUsers(any());
        verify(uploadedFileRepository).save(any(UploadedFile.class));
    }

    @Test
    void runAsync_shouldMarkAsFailed_ifFileIsUnreadable() {
        UploadedFile uploadedFile = UploadedFile.builder()
                .id(1L)
                .storagePath("/nonexistent/path/to/file.csv")
                .build();

        fileProcessingRunner.runAsync(uploadedFile);

        ArgumentCaptor<UploadedFile> fileCaptor = ArgumentCaptor.forClass(UploadedFile.class);
        verify(uploadedFileRepository).save(fileCaptor.capture());

        assertEquals(FileStatus.FAILED, fileCaptor.getValue().getStatus());


    }
}

package ru.shift.userimporter.core.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class HashCalculatorTest {

    @Test
    void sameContent_shouldReturnSameHash() {
        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1_csv",
                "text/csv",
                "some.content".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "file2",
                "test2_csv",
                "text/csv",
                "some.content".getBytes()
        );

        String result1 = HashCalculator.computeHash(file1);
        String result2 = HashCalculator.computeHash(file2);

        assertEquals(result1, result2);
    }

    @Test
    void differentContent_shouldReturnUnequal() {
        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1_csv",
                "text/csv",
                "content A".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "file2",
                "test2_csv",
                "text/csv",
                "content B".getBytes()
        );

        String result1 = HashCalculator.computeHash(file1);
        String result2 = HashCalculator.computeHash(file2);

        assertNotEquals(result1, result2);
    }

    @Test
    void ioException_shouldThrowRunTimeException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("Не удалось прочитать файл"));

        assertThrows(RuntimeException.class, () -> HashCalculator.computeHash(file));
    }
}

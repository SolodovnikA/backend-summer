package ru.shift.userimporter.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.shift.userimporter.api.dto.DetailedFileStatistic;
import ru.shift.userimporter.api.dto.FileIdResponse;
import ru.shift.userimporter.api.dto.FileResponse;
import ru.shift.userimporter.api.dto.FileStatistic;
import ru.shift.userimporter.core.model.FileStatus;
import ru.shift.userimporter.core.service.UploadedFileService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(FileController.class)
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadedFileService uploadedFileService;

    @Test
    void processFile_shouldReturn204() throws Exception {
        mockMvc.perform(post("/files/1/processing")).andExpect(status().isNoContent());
    }

    @Test
    void sendFile_shouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "content A".getBytes()
        );

        when(uploadedFileService.uploadFile(any())).thenReturn(new FileIdResponse("1"));

        mockMvc.perform(multipart("/files").file(file)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").value("1"));
    }

    @Test
    void getDetailedStatistic_shouldReturn200() throws Exception {
        DetailedFileStatistic testStatistic = DetailedFileStatistic.builder()
                .insertedLinesCount(5)
                .updatedLinesCount(3)
                .errors(List.of())
                .build();

        when(uploadedFileService.getDetailedStatistic(1L)).thenReturn(testStatistic);

        mockMvc.perform(get("/files/1/statistics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.insertedLinesCount").value(5));
    }

    @Test
    void getFiles_shouldReturn200() throws Exception {
        FileStatistic testStatistic = FileStatistic.builder()
                .insertedLinesCount(5)
                .updatedLinesCount(3)
                .errorProcessedLinesCount(2)
                .build();

        FileResponse testResponse = FileResponse.builder()
                .fileId("1")
                .status("NEW")
                .statistic(testStatistic)
                .build();

        when(uploadedFileService.getFiles(null)).thenReturn(List.of(testResponse));

        mockMvc.perform(get("/files/statistics")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"));

    }
}

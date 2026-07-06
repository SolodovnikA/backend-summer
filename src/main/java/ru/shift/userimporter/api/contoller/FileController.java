package ru.shift.userimporter.api.contoller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.shift.userimporter.api.dto.FileIdResponse;
import ru.shift.userimporter.api.dto.FileStatistic;
import ru.shift.userimporter.core.service.UploadedFileService;


import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;


@RequiredArgsConstructor
@RequestMapping("/files")
@RestController
public class FileController {
    private final UploadedFileService uploadedFileService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FileIdResponse sendFile(@RequestParam("file")MultipartFile file) {
        return uploadedFileService.uploadFile(file);
    }

    @PostMapping("/{fileId}/processing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processFile(@PathVariable Long fileId) {
        uploadedFileService.processFile(fileId);
    }

    @GetMapping("/statistics")
    public FileStatistic getStatistics() {
        return uploadedFileService.getStatistics();
    }


}

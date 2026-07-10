package ru.shift.userimporter.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.shift.userimporter.api.dto.DetailedFileStatistic;
import ru.shift.userimporter.api.dto.FileIdResponse;
import ru.shift.userimporter.api.dto.FileResponse;
import ru.shift.userimporter.core.model.FileStatus;
import ru.shift.userimporter.core.service.UploadedFileService;


import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.util.List;


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

    @GetMapping("/{fileId}/statistics")
    public DetailedFileStatistic getDetailedStatistic(@PathVariable Long fileId) {
        return uploadedFileService.getDetailedStatistic(fileId);
    }

    @GetMapping("/statistics")
    public List<FileResponse> getFiles(@RequestParam(required = false) FileStatus status){
        return uploadedFileService.getFiles(status);
    }


}

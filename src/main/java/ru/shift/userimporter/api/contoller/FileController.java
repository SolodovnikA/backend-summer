package ru.shift.userimporter.api.contoller;

import ru.shift.userimporter.api.dto.FileIdResponse;
import ru.shift.userimporter.core.service.UploadedFileService;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@RestController
public class FileController {
    private final UploadedFileService uploadedFileService;

    public FileController(UploadedFileService uploadedFileService) {
        this.uploadedFileService = uploadedFileService;
    }

    @PostMapping("/files")
    public ResponseEntity<FileIdResponse> sendFile(@RequestParam("file")MultipartFile file) throws IOException {
        Long fileId = uploadedFileService.uploadFile(file);
        FileIdResponse fileIdResponse = new FileIdResponse(String.valueOf(fileId));
        return ResponseEntity.status(HttpStatus.CREATED).body(fileIdResponse);
    }

}

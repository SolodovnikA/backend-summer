package ru.shift.userimporter.api.contoller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.shift.userimporter.api.dto.FileIdResponse;
import ru.shift.userimporter.core.service.UploadedFileService;


import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
@RequestMapping("/files")
@RestController
public class FileController {
    private final UploadedFileService uploadedFileService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FileIdResponse sendFile(@RequestParam("file")MultipartFile file) throws IOException,
            NoSuchAlgorithmException, FileAlreadyExistsException {
        Long fileId = uploadedFileService.uploadFile(file);
        return new FileIdResponse(String.valueOf(fileId));
    }

}

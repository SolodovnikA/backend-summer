package ru.shift.userimporter.core.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.shift.userimporter.core.model.ErrorCode;
import ru.shift.userimporter.core.model.FileProcessingError;
import ru.shift.userimporter.core.model.UploadedFile;
import ru.shift.userimporter.core.repository.FileProcessingErrorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileProcessingErrorService {
    private final FileProcessingErrorRepository fileProcessingErrorRepository;

    public FileProcessingError buildError(UploadedFile uploadedFile, Integer rowNumber,
                                           String errorMessage, ErrorCode errorCode) {
        return FileProcessingError.builder()
                .uploadedFile(uploadedFile)
                .rowNumber(rowNumber)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    @Transactional
    public void saveErrors(List<FileProcessingError> errors) {
        fileProcessingErrorRepository.saveAll(errors);
    }

}

package ru.shift.userimporter.api.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import ru.shift.userimporter.api.dto.FileResponse;
import ru.shift.userimporter.api.dto.FileStatistic;
import ru.shift.userimporter.api.dto.ProcessingError;
import ru.shift.userimporter.core.model.FileProcessingError;
import ru.shift.userimporter.core.model.UploadedFile;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileMapper {

    public static FileResponse toFileResponse(UploadedFile file) {
        FileStatistic statistic = FileStatistic.builder()
                .insertedLinesCount(file.getInsertedRows()== null ? 0 : file.getInsertedRows())
                .updatedLinesCount(file.getUpdatedRows() == null ? 0 : file.getUpdatedRows())
                .errorProcessedLinesCount(file.getInvalidRows() == null ? 0 : file.getInvalidRows())
                .build();

        return new FileResponse(String.valueOf(file.getId()), file.getStatus().name(), statistic);
    }

    public static ProcessingError toProcessingError(FileProcessingError error) {
        return new ProcessingError(error.getRowNumber(), error.getErrorCode().name(), error.getErrorMessage());
    }
}

package ru.shift.userimporter.api.dto;

import java.util.List;

public record DetailedFileStatistic(int insertedLinesCount, int updatedLinesCount, List<ProcessingError> errors) {
}

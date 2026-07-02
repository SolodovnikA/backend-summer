package ru.shift.userimporter.api.dto;

import java.util.List;

public record DetailedFileStatistic(Integer insertedLinesCount, Integer updatedLinesCount, List<ProcessingError> errors) {
}

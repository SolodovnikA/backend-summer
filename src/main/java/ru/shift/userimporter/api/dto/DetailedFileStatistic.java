package ru.shift.userimporter.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record DetailedFileStatistic(int insertedLinesCount, int updatedLinesCount, List<ProcessingError> errors) {
}

package ru.shift.userimporter.api.dto;

import lombok.Builder;

@Builder
public record FileStatistic(int insertedLinesCount, int updatedLinesCount, int errorProcessedLinesCount) {
}
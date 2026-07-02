package ru.shift.userimporter.api.dto;

public record FileStatistic(Integer insertedLinesCount, Integer updatedLinesCount, Integer errorProcessedLinesCount) {
}
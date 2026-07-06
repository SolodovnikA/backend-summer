package ru.shift.userimporter.api.dto;

public record FileStatistic(int insertedLinesCount, int updatedLinesCount, int errorProcessedLinesCount) {
}
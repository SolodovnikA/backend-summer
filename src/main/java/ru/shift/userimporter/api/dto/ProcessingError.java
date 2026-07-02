package ru.shift.userimporter.api.dto;

public record ProcessingError(Integer lineNumber, String errorCode, String errorMessage) {
}

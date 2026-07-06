package ru.shift.userimporter.core.model;


import java.time.LocalDate;

public record RowValidationResult(boolean valid, ErrorCode errorCode, String errorMessage, LocalDate birthDate) {
    public static RowValidationResult valid(LocalDate birthDate) {
        return new RowValidationResult(true, null, null, birthDate);
    }

    public static RowValidationResult invalid(ErrorCode errorCode, String errorMessage) {
        return new RowValidationResult(false, errorCode, errorMessage, null);
    }
}


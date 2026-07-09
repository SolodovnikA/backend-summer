package ru.shift.userimporter.core.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final AppErrorCode appErrorCode;

    public AppException(AppErrorCode appErrorCode, String message) {
        super(message);
        this.appErrorCode = appErrorCode;
    }
}

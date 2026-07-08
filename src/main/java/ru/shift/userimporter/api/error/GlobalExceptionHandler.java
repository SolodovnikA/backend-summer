package ru.shift.userimporter.api.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.shift.userimporter.api.dto.ErrorResponse;
import ru.shift.userimporter.core.exception.AppException;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Некорректный запрос: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex) {
        log.error("Непредвиденная ошибка", ex);
        return new ErrorResponse("Внутренняя ошибка сервера");
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        HttpStatus status = switch(ex.getErrorCode()) {
            case FILE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        log.warn("{}: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getMessage()));
    }

}

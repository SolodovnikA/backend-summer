package ru.shift.userimporter.api.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.shift.userimporter.api.dto.ErrorResponse;
import ru.shift.userimporter.core.exception.ResourceNotFoundException;

import java.nio.file.FileAlreadyExistsException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Некорректный запрос: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFileAlreadyExists(FileAlreadyExistsException ex) {
        log.warn("Попытка загрузить дублирующийся файл:  {}", ex.getMessage());
        return new ErrorResponse("Файл с таким содержимым уже существует");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Файл не найден: {}", ex.getMessage());
        return new ErrorResponse("Не удалось найти нужный файл");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex) {
        log.error("Непредвиденная ошибка", ex);
        return new ErrorResponse("Внутренняя ошибка сервера");
    }

}

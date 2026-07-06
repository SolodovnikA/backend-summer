package ru.shift.userimporter.core.exception;

public class FileAlreadyExistsException extends RuntimeException{
    public FileAlreadyExistsException(String message) {
        super(message);
    }
}

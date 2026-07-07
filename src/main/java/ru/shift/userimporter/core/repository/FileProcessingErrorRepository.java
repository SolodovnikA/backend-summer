package ru.shift.userimporter.core.repository;

import org.springframework.transaction.annotation.Transactional;;
import ru.shift.userimporter.core.model.FileProcessingError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileProcessingErrorRepository extends JpaRepository<FileProcessingError, Long> {
    List<FileProcessingError> findByUploadedFileId(Long fileId);


    @Transactional
    default void saveErrors(List<FileProcessingError> errors) {
        saveAll(errors);
    }
}

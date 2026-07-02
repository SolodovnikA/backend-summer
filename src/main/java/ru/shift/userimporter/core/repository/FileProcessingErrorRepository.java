package ru.shift.userimporter.core.repository;

import ru.shift.userimporter.core.model.FileProcessingError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileProcessingErrorRepository extends JpaRepository<FileProcessingError, Long> {
}

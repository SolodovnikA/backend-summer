package ru.shift.userimporter.core.repository;

import ru.shift.userimporter.core.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
}

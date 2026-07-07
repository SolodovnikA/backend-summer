package ru.shift.userimporter.core.repository;

import ru.shift.userimporter.core.model.FileStatus;
import ru.shift.userimporter.core.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    boolean existsByHash(String hash);

    List<UploadedFile> findByStatus(FileStatus status);

}

package ru.shift.userimporter.core.repository;

import org.springframework.data.jpa.repository.Query;
import ru.shift.userimporter.core.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    boolean existsByHash(String hash);

    @Query("SELECT COALESCE(SUM(u.insertedRows), 0) FROM UploadedFile u")
    Long sumInsertedRows();

    @Query("SELECT COALESCE(SUM(u.updatedRows), 0) FROM UploadedFile u")
    Long sumUpdatedRows();

    @Query("SELECT COALESCE(SUM(u.invalidRows), 0) FROM UploadedFile u")
    Long sumInvalidRows();




}

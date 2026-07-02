package ru.shift.userimporter.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFileName;

    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "inserted_rows")
    private Integer insertedRows;

    @Column(name = "updated_rows")
    private Integer updatedRows;


    public UploadedFile() {

    }


    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }

    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoragePath() { return storagePath; }

    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Integer getInsertedRows() { return insertedRows; }

    public void setInsertedRows(Integer insertedRows) { this.insertedRows = insertedRows; }

    public Integer getUpdatedRows() { return updatedRows; }

    public void setUpdatedRows(Integer updatedRows) { this.updatedRows = updatedRows; }
}

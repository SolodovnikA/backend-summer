package ru.shift.userimporter.core.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileStatus status;

    @Column(name = "hash", nullable = false, unique = true)
    private String hash;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "processed_rows")
    private Integer processedRows;

    @Column(name = "valid_rows")
    private Integer validRows;

    @Column(name = "invalid_rows")
    private Integer invalidRows;

    @Column(name = "inserted_rows")
    private Integer insertedRows;

    @Column(name = "updated_rows")
    private Integer updatedRows;

}

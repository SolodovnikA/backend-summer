package ru.shift.userimporter.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "file_processing_errors")
public class FileProcessingError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private UploadedFile uploadedFile;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "error_code", nullable = false)
    private String errorCode;

    @Column(name = "raw_data")
    private String rawData;


    public FileProcessingError() {

    }


    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public UploadedFile getUploadedFile() { return uploadedFile;}

    public void setUploadedFile(UploadedFile uploadedFile) { this.uploadedFile = uploadedFile; }

    public Integer getRowNumber() {return rowNumber; }

    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }

    public String getErrorMessage() { return errorMessage; }

    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorCode() { return errorCode; }

    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getRawData() { return rawData; }

    public void setRawData(String rawData) { this.rawData = rawData; }
}

package com.kosmoskan.storing.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "works")
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", nullable = false, length = 255)
    private String studentName;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_format", nullable = false, length = 10)
    private String fileFormat;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private OffsetDateTime uploadedAt;

    public Long getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public String getFilePath() { return filePath; }
    public Long getFileSize() { return fileSize; }
    public String getFileFormat() { return fileFormat; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }

    public void setId(Long id) { this.id = id; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
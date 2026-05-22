package com.kosmoskan.analysis.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_id", nullable = false, unique = true)
    private Long workId;

    @Column(name = "student_name", nullable = false, length = 255)
    private String studentName;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "file_format", nullable = false, length = 10)
    private String fileFormat;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "format_valid", nullable = false, length = 10)
    private String formatValid;

    @Column(name = "size_valid", nullable = false, length = 10)
    private String sizeValid;

    @Convert(converter = StringListConverter.class)
    @Column(name = "issues", columnDefinition = "text")
    private List<String> issues;

    @Column(name = "wordcloud_path", length = 500)
    private String wordcloudPath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public Long getWorkId() { return workId; }
    public String getStudentName() { return studentName; }
    public String getFilename() { return filename; }
    public String getStatus() { return status; }
    public String getFileFormat() { return fileFormat; }
    public Long getFileSize() { return fileSize; }
    public String getFormatValid() { return formatValid; }
    public String getSizeValid() { return sizeValid; }
    public List<String> getIssues() { return issues; }
    public String getWordcloudPath() { return wordcloudPath; }
    public OffsetDateTime getCreatedAt() { return createdAt; }


    public void setId(Long id) { this.id = id; }
    public void setWorkId(Long workId) { this.workId = workId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setStatus(String status) { this.status = status; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setFormatValid(String formatValid) { this.formatValid = formatValid; }
    public void setSizeValid(String sizeValid) { this.sizeValid = sizeValid; }
    public void setIssues(List<String> issues) { this.issues = issues; }
    public void setWordcloudPath(String wordcloudPath) { this.wordcloudPath = wordcloudPath; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
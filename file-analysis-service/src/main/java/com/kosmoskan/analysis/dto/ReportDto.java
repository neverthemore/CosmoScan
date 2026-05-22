package com.kosmoskan.analysis.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;


@Data
public class ReportDto {
    private Long id;
    private Long workId;
    private String studentName;
    private String filename;
    private String status;
    private String fileFormat;
    private Long fileSize;
    private String formatValid;
    private String sizeValid;
    private List<String> issues;
    private boolean hasWordcloud;
    private OffsetDateTime createdAt;
}

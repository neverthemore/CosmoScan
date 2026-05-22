package com.kosmoskan.storing.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class WorkDto {
    private Long id;
    private String studentName;
    private String originalFilename;
    private Long fileSize;
    private String fileFormat;
    private OffsetDateTime uploadedAt;
}

package com.kosmoskan.analysis.controller;

import com.kosmoskan.analysis.dto.ReportDto;
import com.kosmoskan.analysis.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "File Analysis Service", description = "Анализ работ и отчёты")
public class ReportController {

    private final AnalysisService service;

    public ReportController(AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/health")
    @Operation(summary = "Проверка работоспособности")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "file-analysis-service");
    }

    @PostMapping("/analyze/{workId}")
    @Operation(summary = "Запустить технический анализ работы по ID")
    public ReportDto analyze(@PathVariable Long workId) {
        return service.analyze(workId);
    }

    @GetMapping("/reports")
    @Operation(summary = "Получить все отчёты")
    public List<ReportDto> getAllReports() {
        return service.findAll();
    }

    @GetMapping("/reports/{workId}")
    @Operation(summary = "Получить отчёт по ID работы")
    public ReportDto getReport(@PathVariable Long workId) {
        return service.findByWorkId(workId);
    }

    @GetMapping("/reports/{workId}/wordcloud")
    @Operation(summary = "Получить облако слов (PNG изображение)")
    public ResponseEntity<Resource> getWordcloud(@PathVariable Long workId) {
        String path = service.getWordcloudPath(workId);
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"wordcloud_" + workId + ".png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}

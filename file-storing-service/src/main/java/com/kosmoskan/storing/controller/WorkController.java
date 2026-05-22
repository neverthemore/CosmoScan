package com.kosmoskan.storing.controller;

import com.kosmoskan.storing.dto.WorkDto;
import com.kosmoskan.storing.service.WorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "File Storing Service", description = "Хранение файлов студенческих работ")
public class WorkController {

    private final WorkService service;

        public WorkController(WorkService service) {
        this.service = service;
    }

    @GetMapping("/health")
    @Operation(summary = "Проверка работоспособности сервиса")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "service", "file-storing-service"
        );
    }

       @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить работу студента")
    public ResponseEntity<WorkDto> uploadFile(
            @Parameter(description = "ФИО студента")
            @RequestParam("student_name") String studentName,
            @Parameter(description = "Файл работы (pdf, docx или txt, не более 1 МБ)")
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.save(studentName, file));
    }


    @GetMapping("/files")
    @Operation(summary = "Список всех сданных работ")
    public List<WorkDto> listFiles() {
        return service.findAll();
    }


    @GetMapping("/files/{id}")
    @Operation(summary = "Метаданные работы по ID")
    public WorkDto getFileMeta(@PathVariable Long id) {
        return service.findById(id);
    }


    @GetMapping("/files/{id}/content")
    @Operation(summary = "Скачать файл работы")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        WorkDto meta = service.findById(id);
        Resource resource = service.getFileContent(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getOriginalFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}

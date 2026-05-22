package com.kosmoskan.apigateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@RestController
@Tag(name = "КосмоСкан API Gateway", description = "Единая точка входа в систему")
public class GatewayController {

    private final RestClient storingClient;
    private final RestClient analysisClient;

    public GatewayController(
            @Value("${app.file-storing-url}") String storingUrl,
            @Value("${app.file-analysis-url}") String analysisUrl) {
        this.storingClient  = RestClient.builder().baseUrl(storingUrl).build();
        this.analysisClient = RestClient.builder().baseUrl(analysisUrl).build();
    }

    @GetMapping("/health")
    @Operation(summary = "Состояние всех сервисов системы")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gateway", "ok");
        response.put("services", Map.of(
            "file_storing",  pingService(storingClient),
            "file_analysis", pingService(analysisClient)
        ));
        return response;
    }


    @PostMapping(value = "/works", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Принять работу студента (сохранить + проанализировать)")
    public ResponseEntity<Map<String, Object>> submitWork(
            @RequestParam("student_name") String studentName,
            @RequestParam("file") MultipartFile file) throws IOException {

        Map<?, ?> workData = saveFile(studentName, file);
        Long workId = ((Number) workData.get("id")).longValue();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("work", workData);

        try {
            Map<?, ?> reportData = analysisClient.post()
                    .uri("/analyze/{id}", workId)
                    .retrieve()
                    .body(Map.class);
            response.put("report", reportData);
            response.put("message",
                "Работа успешно принята. Статус проверки: " + reportData.get("status"));
        } catch (Exception e) {

            response.put("report", null);
            response.put("warning",
                "Файл сохранён, но сервис анализа временно недоступен. " +
                "Отчёт можно получить позже: GET /works/" + workId + "/reports");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/works")
    @Operation(summary = "Список всех сданных работ")
    public List<?> listWorks() {
        try {
            return storingClient.get().uri("/files").retrieve().body(List.class);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "File Storing Service недоступен. Попробуйте позже."
            );
        }
    }


    @GetMapping("/works/{id}")
    @Operation(summary = "Информация о работе по ID")
    public Map<?, ?> getWork(@PathVariable Long id) {
        try {
            return storingClient.get().uri("/files/{id}", id).retrieve().body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Работа не найдена");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "File Storing Service недоступен"
            );
        }
    }


    @GetMapping("/works/{id}/reports")
    @Operation(summary = "Отчёт о технической проверке работы (для преподавателя)")
    public Map<?, ?> getWorkReport(@PathVariable Long id) {
        try {
            return analysisClient.get().uri("/reports/{id}", id).retrieve().body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Отчёт не найден. Возможно анализ ещё не выполнен."
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "File Analysis Service недоступен"
            );
        }
    }


    @GetMapping("/works/{id}/wordcloud")
    @Operation(summary = "Облако слов для работы (PNG изображение)")
    public ResponseEntity<byte[]> getWordcloud(@PathVariable Long id) {
        try {
            byte[] image = analysisClient.get()
                    .uri("/reports/{id}/wordcloud", id)
                    .retrieve()
                    .body(byte[].class);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"wordcloud_" + id + ".png\"")
                    .body(image);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Облако слов не найдено"
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "File Analysis Service недоступен"
            );
        }
    }


    @GetMapping("/reports")
    @Operation(summary = "Все отчёты о проверке работ")
    public List<?> getAllReports() {
        try {
            return analysisClient.get().uri("/reports").retrieve().body(List.class);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "File Analysis Service недоступен"
            );
        }
    }

    private Map<?, ?> saveFile(String studentName, MultipartFile file) throws IOException {
        try {
            byte[] bytes = file.getBytes();
            ByteArrayResource fileResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() { return file.getOriginalFilename(); }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("student_name", studentName);
            body.add("file", fileResource);

            return storingClient.post()
                    .uri("/files")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                e.getStatusCode(),
                e.getResponseBodyAsString()
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "File Storing Service недоступен. Попробуйте позже."
            );
        }
    }

    private String pingService(RestClient client) {
        try {
            client.get().uri("/health").retrieve().body(Map.class);
            return "ok";
        } catch (Exception e) {
            return "unavailable";
        }
    }
}

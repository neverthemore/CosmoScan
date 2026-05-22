package com.kosmoskan.analysis.service;

import com.kosmoskan.analysis.dto.ReportDto;
import com.kosmoskan.analysis.model.Report;
import com.kosmoskan.analysis.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalysisService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("pdf", "docx", "txt");
    private static final long MAX_FILE_SIZE = 1024 * 1024L;

    private final ReportRepository repository;
    private final TextExtractorService extractor;
    private final WordCloudService wordCloudService;
    private final RestClient restClient;
    private final String reportsDir;

    public AnalysisService(
            ReportRepository repository,
            TextExtractorService extractor,
            WordCloudService wordCloudService,
            @Value("${app.file-storing-url}") String storingUrl,
            @Value("${app.reports-dir}") String reportsDir) throws IOException {
        this.repository = repository;
        this.extractor = extractor;
        this.wordCloudService = wordCloudService;
        this.restClient = RestClient.builder().baseUrl(storingUrl).build();
        this.reportsDir = reportsDir;
        Files.createDirectories(Paths.get(reportsDir));
    }

    public ReportDto analyze(Long workId) {
        if (repository.existsByWorkId(workId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Отчёт для работы ID=" + workId + " уже существует. " +
                "Для просмотра используйте GET /reports/" + workId
            );
        }

        Map<?, ?> meta = fetchMetadata(workId);
        byte[] fileContent = fetchContent(workId);
        String format = (String) meta.get("fileFormat");
        long size = ((Number) meta.get("fileSize")).longValue();
        List<String> issues = new ArrayList<>();
        boolean formatOk = ALLOWED_FORMATS.contains(format);
        boolean sizeOk = size <= MAX_FILE_SIZE;

        if (!formatOk) {
            issues.add("Недопустимый формат: " + format + ". Разрешены: " + ALLOWED_FORMATS);
        }
        if (!sizeOk) {
            issues.add("Размер " + size + " байт превышает допустимый (" + MAX_FILE_SIZE + " байт)");
        }

        String wordcloudPath = null;
        if (formatOk && fileContent != null) {
            String text = extractor.extractText(fileContent, format);
            String wcPath = reportsDir + "/wordcloud_" + workId + ".png";
            if (wordCloudService.generate(text, wcPath)) {
                wordcloudPath = wcPath;
            }
        }

        Report report = new Report();
        report.setWorkId(workId);
        report.setStudentName((String) meta.get("studentName"));
        report.setFilename((String) meta.get("originalFilename"));
        report.setStatus(formatOk && sizeOk ? "accepted" : "needs_revision");
        report.setFileFormat(format);
        report.setFileSize(size);
        report.setFormatValid(formatOk ? "ok" : "error");
        report.setSizeValid(sizeOk ? "ok" : "error");
        report.setIssues(issues);
        report.setWordcloudPath(wordcloudPath);

        return toDto(repository.save(report));
    }

    public List<ReportDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public ReportDto findByWorkId(Long workId) {
        return repository.findByWorkId(workId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Отчёт для работы ID=" + workId + " не найден. " +
                    "Сначала запустите анализ: POST /analyze/" + workId
                ));
    }

    public String getWordcloudPath(Long workId) {
        Report report = repository.findByWorkId(workId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Отчёт не найден"
                ));
        if (report.getWordcloudPath() == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Облако слов недоступно для данной работы"
            );
        }
        return report.getWordcloudPath();
    }

    private Map<?, ?> fetchMetadata(Long workId) {
        try {
            return restClient.get()
                    .uri("/files/{id}", workId)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Работа ID=" + workId + " не найдена"
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "File Storing Service недоступен: " + e.getMessage()
            );
        }
    }

    private byte[] fetchContent(Long workId) {
        try {
            return restClient.get()
                    .uri("/files/{id}/content", workId)
                    .retrieve()
                    .body(byte[].class);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Не удалось получить файл: " + e.getMessage()
            );
        }
    }

    private ReportDto toDto(Report r) {
        ReportDto dto = new ReportDto();
        dto.setId(r.getId());
        dto.setWorkId(r.getWorkId());
        dto.setStudentName(r.getStudentName());
        dto.setFilename(r.getFilename());
        dto.setStatus(r.getStatus());
        dto.setFileFormat(r.getFileFormat());
        dto.setFileSize(r.getFileSize());
        dto.setFormatValid(r.getFormatValid());
        dto.setSizeValid(r.getSizeValid());
        dto.setIssues(r.getIssues() != null ? r.getIssues() : List.of());
        dto.setHasWordcloud(r.getWordcloudPath() != null);
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}

package com.kosmoskan.storing.service;

import com.kosmoskan.storing.dto.WorkDto;
import com.kosmoskan.storing.model.Work;
import com.kosmoskan.storing.repository.WorkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;


@Service
public class WorkService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("pdf", "docx", "txt");

    private static final long MAX_FILE_SIZE = 1024 * 1024L;

    private final WorkRepository repository;
    private final Path uploadDir;


    public WorkService(WorkRepository repository,
                       @Value("${app.upload-dir:/app/uploads}") String uploadDirStr)
            throws IOException {
        this.repository = repository;
        this.uploadDir = Paths.get(uploadDirStr);
        // Создаём папку если её нет (и все промежуточные папки тоже)
        Files.createDirectories(this.uploadDir);
    }


    public WorkDto save(String studentName, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);

        if (!ALLOWED_FORMATS.contains(extension)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Недопустимый формат '" + extension + "'. Разрешены: " + ALLOWED_FORMATS
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Файл слишком большой: " + file.getSize() + " байт. " +
                "Максимальный размер: " + MAX_FILE_SIZE + " байт (1 МБ)"
            );
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String storedName = timestamp + "_" + originalName;
        Path filePath = uploadDir.resolve(storedName);

          Files.write(filePath, file.getBytes());

        Work work = new Work();
        work.setStudentName(studentName);
        work.setOriginalFilename(originalName);
        work.setStoredFilename(storedName);
        work.setFilePath(filePath.toString());
        work.setFileSize(file.getSize());
        work.setFileFormat(extension);

        return toDto(repository.save(work));
    }

    public List<WorkDto> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public WorkDto findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Работа с ID=" + id + " не найдена"
                ));
    }

    public Resource getFileContent(Long id) {
        Work work = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Работа не найдена"
                ));
        try {
            Resource resource = new UrlResource(Paths.get(work.getFilePath()).toUri());
            if (!resource.exists()) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Файл не найден на сервере"
                );
            }
            return resource;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка чтения файла: " + e.getMessage()
            );
        }
    }

    private WorkDto toDto(Work work) {
        WorkDto dto = new WorkDto();
        dto.setId(work.getId());
        dto.setStudentName(work.getStudentName());
        dto.setOriginalFilename(work.getOriginalFilename());
        dto.setFileSize(work.getFileSize());
        dto.setFileFormat(work.getFileFormat());
        dto.setUploadedAt(work.getUploadedAt());
        return dto;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}

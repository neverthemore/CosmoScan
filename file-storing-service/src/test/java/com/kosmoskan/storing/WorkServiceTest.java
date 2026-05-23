package com.kosmoskan.storing;

import com.kosmoskan.storing.model.Work;
import com.kosmoskan.storing.repository.WorkRepository;
import com.kosmoskan.storing.service.WorkService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkServiceTest {

    @Mock
    private WorkRepository repository;

    private WorkService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        service = new WorkService(repository, tempDir.toString());
    }

    @Test
    void save_ValidTxt_CallsRepositorySave() throws IOException {
        when(repository.save(any(Work.class))).thenAnswer(inv -> {
            Work w = inv.getArgument(0);
            w.setId(1L);
            w.setUploadedAt(OffsetDateTime.now());
            return w;
        });
        var file = new MockMultipartFile(
                "file", "hw.txt", "text/plain", "hello".getBytes()
        );
        var result = service.save("Иванов", file);
        assertNotNull(result);
        assertEquals("txt", result.getFileFormat());
        verify(repository, times(1)).save(any(Work.class));
    }

    @Test
    void save_Pdf_SavesCorrectFormat() throws IOException {
        when(repository.save(any())).thenAnswer(inv -> {
            Work w = inv.getArgument(0);
            w.setId(2L);
            w.setUploadedAt(OffsetDateTime.now());
            return w;
        });
        var file = new MockMultipartFile(
                "file", "r.pdf", "application/pdf", "pdf".getBytes()
        );
        assertEquals("pdf", service.save("Петров", file).getFileFormat());
    }

    @Test
    void save_Docx_SavesCorrectFormat() throws IOException {
        when(repository.save(any())).thenAnswer(inv -> {
            Work w = inv.getArgument(0);
            w.setId(3L);
            w.setUploadedAt(OffsetDateTime.now());
            return w;
        });
        var file = new MockMultipartFile(
                "file", "e.docx", "application/vnd", "docx".getBytes()
        );
        assertEquals("docx", service.save("Сидоров", file).getFileFormat());
    }

    @Test
    void save_ZipFile_ThrowsBadRequest_RepositoryNotCalled() {
        var file = new MockMultipartFile(
                "file", "a.zip", "application/zip", "PK".getBytes()
        );
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.save("Т", file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void save_ExeFile_ThrowsBadRequest() {
        var file = new MockMultipartFile(
                "file", "v.exe", "application/exe", "MZ".getBytes()
        );
        assertThrows(ResponseStatusException.class, () -> service.save("Т", file));
        verify(repository, never()).save(any());
    }

    @Test
    void save_FileOver1MB_ThrowsBadRequest_RepositoryNotCalled() {
        var file = new MockMultipartFile(
                "file", "b.txt", "text/plain", new byte[1024 * 1024 + 1]
        );
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.save("Т", file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void save_FileExactly1MB_Passes() throws IOException {
        when(repository.save(any())).thenAnswer(inv -> {
            Work w = inv.getArgument(0);
            w.setId(4L);
            w.setUploadedAt(OffsetDateTime.now());
            return w;
        });
        var file = new MockMultipartFile(
                "file", "ok.txt", "text/plain", new byte[1024 * 1024]
        );
        assertDoesNotThrow(() -> service.save("Т", file));
    }

    @Test
    void save_NoExtension_ThrowsBadRequest() {
        var file = new MockMultipartFile(
                "file", "noext", "text/plain", "d".getBytes()
        );
        assertThrows(ResponseStatusException.class, () -> service.save("Т", file));
    }

    @Test
    void findAll_ReturnsMappedDtos() {
        when(repository.findAll()).thenReturn(List.of(
                makeWork(1L, "Иванов"),
                makeWork(2L, "Петров")
        ));
        var list = service.findAll();
        assertEquals(2, list.size());
        assertEquals("Иванов", list.get(0).getStudentName());
        assertEquals("Петров", list.get(1).getStudentName());
    }

    @Test
    void findAll_Empty_ReturnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void findById_Exists_ReturnsDto() {
        when(repository.findById(1L)).thenReturn(Optional.of(makeWork(1L, "Козлов")));
        var dto = service.findById(1L);
        assertEquals(1L, dto.getId());
        assertEquals("Козлов", dto.getStudentName());
    }

    @Test
    void findById_NotFound_ThrowsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.findById(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getFileContent_NotFound_ThrowsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.getFileContent(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private Work makeWork(Long id, String name) {
        Work w = new Work();
        w.setId(id);
        w.setStudentName(name);
        w.setOriginalFilename("work.txt");
        w.setStoredFilename("ts_work.txt");
        w.setFilePath(tempDir.resolve("work.txt").toString());
        w.setFileSize(512L);
        w.setFileFormat("txt");
        w.setUploadedAt(OffsetDateTime.now());
        return w;
    }
}
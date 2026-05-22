package com.kosmoskan.storing;

import com.kosmoskan.storing.controller.WorkController;
import com.kosmoskan.storing.dto.WorkDto;
import com.kosmoskan.storing.service.WorkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkController.class)
class WorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkService service;

    private WorkDto makeDto() {
        WorkDto dto = new WorkDto();
        dto.setId(1L);
        dto.setStudentName("Иванов Иван");
        dto.setOriginalFilename("work.txt");
        dto.setFileSize(100L);
        dto.setFileFormat("txt");
        dto.setUploadedAt(OffsetDateTime.now());
        return dto;
    }

    @Test
    void health_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("ok"))
               .andExpect(jsonPath("$.service").value("file-storing-service"));
    }

    @Test
    void upload_ValidTxtFile_ShouldReturn200() throws Exception {
        when(service.save(eq("Иванов Иван"), any())).thenReturn(makeDto());

        MockMultipartFile file = new MockMultipartFile(
            "file", "work.txt", "text/plain", "Hello World".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Иванов Иван"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.fileFormat").value("txt"));
    }

    @Test
    void upload_InvalidFormat_ShouldReturn400() throws Exception {
        when(service.save(any(), any()))
            .thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Недопустимый формат"
            ));

        MockMultipartFile file = new MockMultipartFile(
            "file", "work.zip", "application/zip", "data".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Петров"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void upload_FileTooLarge_ShouldReturn400() throws Exception {
        when(service.save(any(), any()))
            .thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Файл слишком большой"
            ));

        MockMultipartFile file = new MockMultipartFile(
            "file", "big.txt", "text/plain", new byte[2 * 1024 * 1024]
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Тестов"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void listFiles_ShouldReturnArray() throws Exception {
        when(service.findAll()).thenReturn(List.of(makeDto()));

        mockMvc.perform(get("/files"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value(1))
               .andExpect(jsonPath("$[0].studentName").value("Иванов Иван"));
    }

    @Test
    void listFiles_EmptyList_ShouldReturnEmptyArray() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/files"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getFileMeta_Existing_ShouldReturn200() throws Exception {
        when(service.findById(1L)).thenReturn(makeDto());

        mockMvc.perform(get("/files/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getFileMeta_NotFound_ShouldReturn404() throws Exception {
        when(service.findById(999L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдена"));

        mockMvc.perform(get("/files/999"))
               .andExpect(status().isNotFound());
    }
}

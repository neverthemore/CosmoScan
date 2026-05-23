package com.kosmoskan.storing;

import com.kosmoskan.storing.repository.WorkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WorkRepository workRepository;

    @Test
    void health_AlwaysReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void upload_ValidTxt_ResponseHasNoFilePathField() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "check.txt", "text/plain", "test content".getBytes()
        );
        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Безопасность Тест"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filePath").doesNotExist())
                .andExpect(jsonPath("$.storedFilename").doesNotExist());
    }

    @Test
    void upload_JsFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.js", "application/javascript", "alert(1)".getBytes()
        );
        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Ошибка Тест"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listFiles_ReturnsJsonArray() throws Exception {
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getFile_VeryLargeId_Returns404() throws Exception {
        mockMvc.perform(get("/files/999999999"))
                .andExpect(status().isNotFound());
    }
}
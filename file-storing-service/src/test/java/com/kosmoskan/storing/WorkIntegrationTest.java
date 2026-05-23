package com.kosmoskan.storing;

import com.jayway.jsonpath.JsonPath;
import com.kosmoskan.storing.model.Work;
import com.kosmoskan.storing.repository.WorkRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkRepository workRepository;

    private long extractId(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }


    @Test
    @Order(1)
    void health_ReturnsOkWithServiceName() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("file-storing-service"));
    }


    @Test
    @Order(2)
    void upload_ValidTxtFile_Returns200AndSavesToDb() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "homework.txt", "text/plain",
                "Это текст контрольной работы по информатике.".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Иванов Иван Иванович"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.studentName").value("Иванов Иван Иванович"))
                .andExpect(jsonPath("$.fileFormat").value("txt"))
                .andExpect(jsonPath("$.originalFilename").value("homework.txt"))
                .andExpect(jsonPath("$.fileSize").isNumber())
                .andExpect(jsonPath("$.uploadedAt").exists());

        assertTrue(workRepository.count() >= 1);
    }

    @Test
    @Order(3)
    void upload_ValidPdfFile_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf",
                "%PDF-1.4 fake pdf content for test".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Петров Пётр Петрович"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileFormat").value("pdf"))
                .andExpect(jsonPath("$.studentName").value("Петров Пётр Петрович"));
    }

    @Test
    @Order(4)
    void upload_ValidDocxFile_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "essay.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "PK fake docx content".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Сидорова Мария"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileFormat").value("docx"));
    }

    @Test
    @Order(5)
    void upload_FileExactly1MB_Returns200() throws Exception {
        byte[] exactly1mb = new byte[1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "exact1mb.txt", "text/plain", exactly1mb
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Граничный Тест"))
                .andExpect(status().isOk());
    }


    @Test
    @Order(6)
    void upload_ZipFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "archive.zip", "application/zip",
                "PK fake zip content".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Архивов"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    void upload_ExeFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "program.exe", "application/exe",
                "MZ content".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Вирусов"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    void upload_FileOver1MB_Returns400() throws Exception {
        byte[] tooBig = new byte[1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "toobig.txt", "text/plain", tooBig
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Большов"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(9)
    void upload_NoExtension_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "filenoext", "text/plain",
                "data".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Безымянов"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    void upload_RarFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "work.rar", "application/rar",
                "Rar!".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Тестов"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    void listFiles_AfterUploads_ReturnsNonEmptyArray() throws Exception {
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)
                ));
    }

    @Test
    @Order(12)
    void listFiles_ReturnsJsonArray() throws Exception {
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }


    @Test
    @Order(13)
    void getFileMeta_ExistingId_ReturnsCorrectData() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "meta_test.txt", "text/plain",
                "content".getBytes()
        );
        MvcResult result = mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Метаданных Тест"))
                .andReturn();

        long id = extractId(result.getResponse().getContentAsString());

        mockMvc.perform(get("/files/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.studentName").value("Метаданных Тест"))
                .andExpect(jsonPath("$.originalFilename").value("meta_test.txt"))
                .andExpect(jsonPath("$.fileFormat").value("txt"));
    }

    @Test
    @Order(14)
    void getFileMeta_NonExistingId_Returns404() throws Exception {
        mockMvc.perform(get("/files/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(15)
    void getFileMeta_IdZero_Returns404() throws Exception {
        mockMvc.perform(get("/files/0"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(16)
    void getFileMeta_VeryLargeId_Returns404() throws Exception {
        mockMvc.perform(get("/files/999999999"))
                .andExpect(status().isNotFound());
    }


    @Test
    @Order(17)
    void downloadFile_ExistingFile_ReturnsContent() throws Exception {
        byte[] originalContent =
                "Содержимое тестового файла для скачивания.".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "download_test.txt", "text/plain", originalContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Скачивателев"))
                .andReturn();

        long id = extractId(uploadResult.getResponse().getContentAsString());

        MvcResult downloadResult = mockMvc.perform(get("/files/{id}/content", id))
                .andExpect(status().isOk())
                .andReturn();

        assertArrayEquals(originalContent,
                downloadResult.getResponse().getContentAsByteArray());
    }

    @Test
    @Order(18)
    void downloadFile_NonExistingId_Returns404() throws Exception {
        mockMvc.perform(get("/files/99998/content"))
                .andExpect(status().isNotFound());
    }


    @Test
    @Order(19)
    void workModel_GettersAndSetters_WorkCorrectly() {
        Work work = new Work();
        work.setId(42L);
        work.setStudentName("Тестов Тест");
        work.setOriginalFilename("test.pdf");
        work.setStoredFilename("20250101_test.pdf");
        work.setFilePath("/uploads/test.pdf");
        work.setFileSize(1024L);
        work.setFileFormat("pdf");

        assertEquals(42L, work.getId());
        assertEquals("Тестов Тест", work.getStudentName());
        assertEquals("test.pdf", work.getOriginalFilename());
        assertEquals("20250101_test.pdf", work.getStoredFilename());
        assertEquals("/uploads/test.pdf", work.getFilePath());
        assertEquals(1024L, work.getFileSize());
        assertEquals("pdf", work.getFileFormat());
        assertNull(work.getUploadedAt());
    }

    @Test
    @Order(20)
    void workModel_SavedToDb_HasUploadedAt() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "model_test.txt", "text/plain",
                "test".getBytes()
        );
        MvcResult result = mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Модель Тест"))
                .andReturn();

        long id = extractId(result.getResponse().getContentAsString());

        Work saved = workRepository.findById(id).orElseThrow();
        assertNotNull(saved.getUploadedAt());
        assertEquals("Модель Тест", saved.getStudentName());
        assertEquals("txt", saved.getFileFormat());
    }

    @Test
    @Order(21)
    void workDto_ResponseHasAllFieldsExceptFilePath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "dto_check.txt", "text/plain",
                "dto test content".getBytes()
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "ДТО Тестов"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.studentName").exists())
                .andExpect(jsonPath("$.originalFilename").exists())
                .andExpect(jsonPath("$.fileSize").exists())
                .andExpect(jsonPath("$.fileFormat").exists())
                .andExpect(jsonPath("$.uploadedAt").exists())
                .andExpect(jsonPath("$.filePath").doesNotExist());
    }

    @Test
    @Order(22)
    void upload_AllThreeFormats_AllSavedToDb() throws Exception {
        long countBefore = workRepository.count();

        for (String[] info : new String[][]{
                {"w1.txt",  "text/plain",      "txt"},
                {"w2.pdf",  "application/pdf", "pdf"},
                {"w3.docx", "application/vnd", "docx"}
        }) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", info[0], info[1], "content".getBytes()
            );
            mockMvc.perform(multipart("/files")
                            .file(file)
                            .param("student_name", "Студент " + info[2]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileFormat").value(info[2]));
        }

        assertEquals(countBefore + 3, workRepository.count());
    }

    @Test
    @Order(23)
    void upload_SmallFile_SavesCorrectFileSize() throws Exception {
        byte[] content = "tiny".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "tiny.txt", "text/plain", content
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", "Размеров"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileSize").value(content.length));
    }

    @Test
    @Order(24)
    void upload_TwoFilesWithSameName_BothSavedOk() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "same.txt", "text/plain",
                    ("Content version " + i).getBytes()
            );
            mockMvc.perform(multipart("/files")
                            .file(file)
                            .param("student_name", "Дубликатов " + i))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.originalFilename").value("same.txt"));
        }
    }

    @Test
    @Order(25)
    void upload_StudentNameWithSpaces_SavedCorrectly() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "work.txt", "text/plain", "content".getBytes()
        );
        String name = "Иванов-Петров Александр Сергеевич";

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("student_name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value(name));
    }

    @Test
    @Order(26)
    void getFileMeta_AfterMultipleUploads_EachIdUnique() throws Exception {
        long[] ids = new long[3];
        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "unique_" + i + ".txt", "text/plain",
                    ("content " + i).getBytes()
            );
            MvcResult result = mockMvc.perform(multipart("/files")
                            .file(file)
                            .param("student_name", "Уникальный " + i))
                    .andReturn();
            ids[i] = extractId(result.getResponse().getContentAsString());
        }

        assertNotEquals(ids[0], ids[1]);
        assertNotEquals(ids[1], ids[2]);
        assertNotEquals(ids[0], ids[2]);

        for (long id : ids) {
            mockMvc.perform(get("/files/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
        }
    }
}
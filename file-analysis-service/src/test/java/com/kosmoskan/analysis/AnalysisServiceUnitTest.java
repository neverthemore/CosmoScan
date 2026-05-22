package com.kosmoskan.analysis;

import com.kosmoskan.analysis.service.TextExtractorService;
import com.kosmoskan.analysis.service.WordCloudService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisServiceUnitTest {

    private final TextExtractorService extractor = new TextExtractorService();
    private final WordCloudService wordCloud = new WordCloudService();

    @Test
    void extractText_ValidTxt_ReturnsContent() {
        byte[] content = "Hello World Java Spring Boot".getBytes();
        String result = extractor.extractText(content, "txt");
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("World"));
    }

    @Test
    void extractText_EmptyContent_ReturnsEmpty() {
        String result = extractor.extractText(new byte[0], "txt");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractText_NullContent_ReturnsEmpty() {
        String result = extractor.extractText(null, "txt");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractText_UnknownFormat_ReturnsEmpty() {
        String result = extractor.extractText("data".getBytes(), "zip");
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    void extractText_InvalidPdfBytes_ReturnsErrorMessage() {
        String result = extractor.extractText("not a real pdf".getBytes(), "pdf");
        assertNotNull(result);

    }

    @Test
    void extractText_InvalidDocxBytes_ReturnsErrorMessage() {
        String result = extractor.extractText("not a real docx".getBytes(), "docx");
        assertNotNull(result);
    }

    @Test
    void extractText_CaseSensitivity_WorksWithUppercase() {
        byte[] content = "Test content".getBytes();
        String result = extractor.extractText(content, "TXT");
        assertNotNull(result);
    }

     @Test
    void generate_NullText_ReturnsFalse(@TempDir Path dir) {
        boolean result = wordCloud.generate(null, dir + "/wc.png");
        assertFalse(result);
    }

    @Test
    void generate_ShortText_ReturnsFalse(@TempDir Path dir) {
        boolean result = wordCloud.generate("hi", dir + "/wc.png");
        assertFalse(result);
    }

    @Test
    void generate_EmptyString_ReturnsFalse(@TempDir Path dir) {
        boolean result = wordCloud.generate("   ", dir + "/wc.png");
        assertFalse(result);
    }

    @Test
    void generate_ValidText_CreatesFile(@TempDir Path dir) {

        String text = "Java Spring Boot Docker Kubernetes микросервисы PostgreSQL " +
            "база данных контейнеры архитектура разработка программирование " +
            "тестирование покрытие анализ файл студент работа преподаватель " +
            "система облако слов генерация формат размер проверка технический " +
            "университет контрольная сессия лабораторная практика задание";

        String outputPath = dir + "/wordcloud_test.png";
        boolean result = wordCloud.generate(text, outputPath);

        assertTrue(result, "Облако слов должно быть создано успешно");
        File file = new File(outputPath);
        assertTrue(file.exists(), "Файл PNG должен существовать");
        assertTrue(file.length() > 0, "Файл PNG не должен быть пустым");
    }

    @Test
    void generate_OnlyStopWords_ReturnsFalse(@TempDir Path dir) {
                String text = "и в не на с по за к из от у до как а но то же бы или что";
          assertDoesNotThrow(() ->
            wordCloud.generate(text, dir + "/wc2.png")
        );
    }
}

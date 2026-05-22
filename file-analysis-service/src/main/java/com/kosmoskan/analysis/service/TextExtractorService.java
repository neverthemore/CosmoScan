package com.kosmoskan.analysis.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class TextExtractorService {

    public String extractText(byte[] content, String format) {
        if (content == null || content.length == 0) return "";
        try {
            return switch (format.toLowerCase()) {
                case "txt"  -> extractFromTxt(content);
                case "pdf"  -> extractFromPdf(content);
                case "docx" -> extractFromDocx(content);
                default     -> "";
            };
        } catch (Exception e) {
            return "Не удалось извлечь текст: " + e.getMessage();
        }
    }

    private String extractFromTxt(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    private String extractFromPdf(byte[] content) throws Exception {
        try (PDDocument doc = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractFromDocx(byte[] content) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }
}
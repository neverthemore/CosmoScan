package com.kosmoskan.analysis.service;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.palette.ColorPalette;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class WordCloudService {

      private static final int MIN_TEXT_LENGTH = 20;

    private static final Set<String> STOP_WORDS = Set.of(
        "и", "в", "не", "на", "с", "по", "за", "к", "из", "от", "у", "до",
        "как", "а", "но", "то", "же", "бы", "или", "что", "это", "для",
        "при", "без", "под", "над", "через", "между", "об", "о",
        "the", "a", "an", "is", "in", "of", "to", "and", "for", "on",
        "at", "by", "with", "was", "are", "has", "had", "be", "it", "as"
    );


    public boolean generate(String text, String outputPath) {
        if (text == null || text.trim().length() < MIN_TEXT_LENGTH) {
            return false;
        }
        try {
            List<WordFrequency> frequencies = buildWordFrequencies(text);
            if (frequencies.isEmpty()) return false;

            Dimension dimension = new Dimension(800, 400);
            WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);

            wordCloud.setPadding(2);
            wordCloud.setBackground(new RectangleBackground(dimension));
            wordCloud.setColorPalette(new ColorPalette(
                new Color(0x0D47A1),
                new Color(0x1565C0),
                new Color(0x1976D2),
                new Color(0x42A5F5),
                new Color(0x64B5F6)
            ));
            wordCloud.setFontScalar(new SqrtFontScalar(12, 45));

            wordCloud.build(frequencies);
            wordCloud.writeToFile(outputPath);
            return true;

        } catch (Exception e) {
            return false;
        }
    }


    private List<WordFrequency> buildWordFrequencies(String text) {
        Map<String, Integer> freqMap = new HashMap<>();

               String[] words = text.toLowerCase()
                .replaceAll("[^а-яёa-z0-9\\s]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                freqMap.merge(word, 1, Integer::sum);
            }
        }


        return freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(100)
                .map(e -> new WordFrequency(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}

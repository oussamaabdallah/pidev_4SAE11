package org.example.offer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de traduction gratuit : MyMemory (par défaut, sans clé) ou LibreTranslate.
 * Langues supportées : FR, EN, AR.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private static final String MYMEMORY_URL = "https://api.mymemory.translated.net/get";
    private static final String DEFAULT_LIBRETRANSLATE_URL = "https://libretranslate.de/translate";
    /** MyMemory limite à 500 bytes par requête ; on découpe à 400 caractères pour rester sûr. */
    private static final int MYMEMORY_MAX_CHARS = 400;

    private final RestTemplate restTemplate;

    @Value("${app.translation.provider:mymemory}")
    private String provider;

    @Value("${app.translation.libretranslate-url:" + DEFAULT_LIBRETRANSLATE_URL + "}")
    private String libretranslateUrl;

    @Value("${app.translation.libretranslate-api-key:}")
    private String apiKey;

    /** Langue source supposée pour les offres (souvent en français). */
    @Value("${app.translation.source-lang:fr}")
    private String defaultSourceLang;

    private static final Set<String> ALLOWED_LANGUAGES = Set.of("fr", "en", "ar");

    /**
     * Traduit une liste de textes vers la langue cible.
     * Utilise MyMemory (gratuit, sans clé) par défaut, ou LibreTranslate si configuré.
     */
    public List<String> translate(List<String> texts, String targetLanguage) {
        String target = normalizeTargetLanguage(targetLanguage);
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<String> toTranslate = texts.stream()
                .map(t -> t == null || t.isBlank() ? " " : t)
                .limit(10)
                .collect(Collectors.toList());

        if ("mymemory".equalsIgnoreCase(provider)) {
            return translateWithMyMemory(toTranslate, target);
        }
        return translateWithLibreTranslate(toTranslate, target);
    }

    /**
     * MyMemory : gratuit, sans clé API. Limite ~500 bytes par requête donc découpage pour les longs textes.
     */
    private List<String> translateWithMyMemory(List<String> texts, String target) {
        String source = "fr".equals(target) ? "en" : defaultSourceLang;
        String langpair = source + "|" + target;
        List<String> result = new ArrayList<>();
        for (String text : texts) {
            try {
                result.add(translateOneTextMyMemory(text, langpair));
            } catch (Exception e) {
                log.warn("MyMemory failed for text length {}, trying LibreTranslate: {}", text.length(), e.getMessage());
                return translateWithLibreTranslate(texts, target);
            }
        }
        return result;
    }

    private String translateOneTextMyMemory(String text, String langpair) {
        if (text.length() <= MYMEMORY_MAX_CHARS) {
            return callMyMemory(text, langpair);
        }
        List<String> chunks = chunkText(text, MYMEMORY_MAX_CHARS);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) out.append(" ");
            out.append(callMyMemory(chunks.get(i), langpair));
        }
        return out.toString();
    }

    private List<String> chunkText(String text, int maxLen) {
        List<String> list = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLen, text.length());
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) end = lastSpace + 1;
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start) end = lastNewline + 1;
            }
            list.add(text.substring(start, end).trim());
            start = end;
        }
        return list;
    }

    private String callMyMemory(String q, String langpair) {
        URI uri = UriComponentsBuilder.fromHttpUrl(MYMEMORY_URL)
                .queryParam("q", q)
                .queryParam("langpair", langpair)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        ResponseEntity<MyMemoryResponse> response = restTemplate.getForEntity(uri, MyMemoryResponse.class);
        if (response.getBody() == null || response.getBody().getResponseData() == null) {
            throw new RuntimeException("MyMemory returned empty response");
        }
        String translated = response.getBody().getResponseData().getTranslatedText();
        if (translated == null || translated.isBlank()) {
            throw new RuntimeException("MyMemory returned no translation");
        }
        return translated;
    }

    /**
     * LibreTranslate (optionnel, peut nécessiter une clé selon l'instance).
     */
    private List<String> translateWithLibreTranslate(List<String> toTranslate, String target) {
        Map<String, Object> body = new HashMap<>();
        body.put("q", toTranslate.size() == 1 ? toTranslate.get(0) : toTranslate);
        body.put("source", "auto");
        body.put("target", target);
        body.put("format", "text");
        if (apiKey != null && !apiKey.isBlank()) {
            body.put("api_key", apiKey);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<LibreTranslateResponse> response = restTemplate.exchange(
                    libretranslateUrl, HttpMethod.POST, entity, LibreTranslateResponse.class);
            if (response.getBody() == null || response.getBody().getTranslatedText() == null) {
                return translateOneByOneLibre(toTranslate, target);
            }
            Object translated = response.getBody().getTranslatedText();
            List<String> result;
            if (translated instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) translated;
                result = list;
            } else {
                result = List.of(translated.toString());
            }
            if (result.size() != toTranslate.size()) {
                return translateOneByOneLibre(toTranslate, target);
            }
            return result;
        } catch (Exception e) {
            log.error("LibreTranslate API error: {}", e.getMessage());
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }

    private List<String> translateOneByOneLibre(List<String> texts, String target) {
        List<String> out = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        for (String text : texts) {
            Map<String, Object> body = new HashMap<>();
            body.put("q", text);
            body.put("source", "auto");
            body.put("target", target);
            body.put("format", "text");
            if (apiKey != null && !apiKey.isBlank()) {
                body.put("api_key", apiKey);
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<LibreTranslateResponse> response = restTemplate.exchange(
                    libretranslateUrl, HttpMethod.POST, entity, LibreTranslateResponse.class);
            if (response.getBody() != null && response.getBody().getTranslatedText() != null) {
                Object t = response.getBody().getTranslatedText();
                out.add(t instanceof List ? ((List<?>) t).get(0).toString() : t.toString());
            } else {
                throw new RuntimeException("No translatedText in response");
            }
        }
        return out;
    }

    private String normalizeTargetLanguage(String target) {
        if (target == null || target.isBlank()) return "en";
        String lower = target.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_LANGUAGES.contains(lower)) return lower;
        if ("french".equals(lower) || "français".equals(lower)) return "fr";
        if ("english".equals(lower) || "anglais".equals(lower)) return "en";
        if ("arabic".equals(lower) || "arabe".equals(lower)) return "ar";
        return "en";
    }

    @Data
    private static class MyMemoryResponse {
        @JsonProperty("responseData")
        private MyMemoryResponseData responseData;
    }

    @Data
    private static class MyMemoryResponseData {
        @JsonProperty("translatedText")
        private String translatedText;
    }

    @Data
    private static class LibreTranslateResponse {
        @JsonProperty("translatedText")
        private Object translatedText;
    }
}

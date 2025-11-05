package com.example.schedulewatcher.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class RuzClient {
    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${ruz.base-url:https://ruz.spbstu.ru/api/v1/ruz}")
    private String baseUrl;

    public String resolveGroupIdByCode(String code) {
        String q = url(code);
        String[] urls = new String[] {
                baseUrl + "/search/groups?term=" + q,
                baseUrl + "/search/groups?q=" + q,
                baseUrl + "/search/groups?name=" + q
        };
        for (String u : urls) {
            String id = pickFirstId(u);
            if (id != null) return id;
        }
        return null;
    }

    public String resolveTeacherIdByTCode(String tcode) {
        if (tcode != null && tcode.startsWith("T-")) {
            String digits = tcode.substring(2).replaceAll("\\D+", "");
            if (!digits.isEmpty()) return digits;
        }
        String q = url(tcode);
        String[] urls = new String[] {
                baseUrl + "/search/teachers?term=" + q,
                baseUrl + "/search/teachers?q=" + q,
                baseUrl + "/search/teachers?name=" + q
        };
        for (String u : urls) {
            String id = pickFirstId(u);
            if (id != null) return id;
        }
        return tcode;
    }

    public String fetchScheduleJson(String subjectType, String ruzId) {
        try {
            String url = "teacher".equals(subjectType)
                    ? baseUrl + "/teachers/" + ruzId + "/scheduler"
                    : baseUrl + "/scheduler/" + ruzId;
            ResponseEntity<String> res = http.getForEntity(url, String.class);
            return res.getBody();
        } catch (Exception e) {
            return "{\"error\":\"fetch_failed\"}";
        }
    }

    // ---- helpers ----
    private String pickFirstId(String url) {
        try {
            String body = http.getForObject(url, String.class);
            JsonNode node = om.readTree(body);
            if (node.isArray() && node.size() > 0) {
                JsonNode first = node.get(0);
                if (first.has("id")) return first.get("id").asText();
                if (first.has("Id")) return first.get("Id").asText();
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String url(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}

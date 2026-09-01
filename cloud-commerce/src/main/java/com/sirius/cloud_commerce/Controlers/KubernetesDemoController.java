package com.sirius.cloud_commerce.Controlers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KubernetesDemoController {

    private final String ambiente;
    private final String apiKey;

    public KubernetesDemoController(
            @Value("${AMBIENTE:local}") String ambiente,
            @Value("${API_KEY:}") String apiKey) {
        this.ambiente = ambiente;
        this.apiKey = apiKey;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "application", "cloud-commerce",
                "version", "v1");
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "ambiente", ambiente,
                "apiKeyConfigured", apiKey != null && !apiKey.isBlank());
    }
}

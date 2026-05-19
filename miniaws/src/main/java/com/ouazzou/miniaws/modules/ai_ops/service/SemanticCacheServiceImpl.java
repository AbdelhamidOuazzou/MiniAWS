package com.ouazzou.miniaws.modules.ai_ops.service;

import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SemanticCacheServiceImpl implements ISemanticCacheService {
    //l'outil classique de Spring pour faire des requêtes HTTP
    private final RestTemplate restTemplate;
    private final String PYTHON_SERVICE_URL = "http://localhost:8001/api/cache";

    public SemanticCacheServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Optional<VmDeploymentRequest> findSimilarRequest(String prompt) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("prompt", prompt);

            ResponseEntity<VmDeploymentRequest> response = restTemplate.postForEntity(
                    PYTHON_SERVICE_URL + "/search", request, VmDeploymentRequest.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la recherche dans le cache Python : " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void saveToCache(String prompt, VmDeploymentRequest result) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("prompt", prompt);
            request.put("result", result);

            restTemplate.postForEntity(PYTHON_SERVICE_URL + "/save", request, Void.class);
            System.out.println("✅ Sauvegardé dans le cache sémantique Python.");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la sauvegarde dans le cache Python : " + e.getMessage());
        }
    }
}
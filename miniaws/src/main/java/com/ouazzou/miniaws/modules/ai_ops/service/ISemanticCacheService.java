package com.ouazzou.miniaws.modules.ai_ops.service;

import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;

import java.util.Optional;

public interface ISemanticCacheService {
    Optional<VmDeploymentRequest> findSimilarRequest(String prompt);
    void saveToCache(String prompt, VmDeploymentRequest result);
}

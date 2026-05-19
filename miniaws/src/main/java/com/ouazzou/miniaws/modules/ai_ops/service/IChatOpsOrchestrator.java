package com.ouazzou.miniaws.modules.ai_ops.service;

import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;
import com.ouazzou.miniaws.modules.compute.domain.ServerInstance;

public interface IChatOpsOrchestrator {
    VmDeploymentRequest analyzeDeploymentRequest(String userMessage);
    ServerInstance executeMagicDeployment(String userMessage);
}
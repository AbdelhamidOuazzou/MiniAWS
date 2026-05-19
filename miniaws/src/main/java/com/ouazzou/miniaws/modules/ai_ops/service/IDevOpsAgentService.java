package com.ouazzou.miniaws.modules.ai_ops.service;

public interface IDevOpsAgentService {
    // On définit juste CE QUE le service sait faire (pas COMMENT il le fait)
    String askAgent(String userMessage);
}
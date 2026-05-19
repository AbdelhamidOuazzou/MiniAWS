package com.ouazzou.miniaws.modules.ai_ops.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class DevOpsAgentServiceImpl implements IDevOpsAgentService {

    private final ChatClient chatClient;

    public DevOpsAgentServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String askAgent(String userMessage) {
        return chatClient.prompt()
                .system("Tu es un agent DevOps pour le projet MiniAWS. Réponds de façon concise et technique.")
                .user(userMessage)
                .call()
                .content();
    }
}
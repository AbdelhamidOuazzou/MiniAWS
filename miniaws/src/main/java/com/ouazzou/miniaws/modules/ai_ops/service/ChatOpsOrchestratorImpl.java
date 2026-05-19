package com.ouazzou.miniaws.modules.ai_ops.service;

import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;
import com.ouazzou.miniaws.modules.compute.domain.ServerInstance;
import com.ouazzou.miniaws.modules.compute.service.IComputeService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
public class ChatOpsOrchestratorImpl implements IChatOpsOrchestrator {

    private final ChatClient chatClient;
    private final IComputeService computeService;
    private final ISemanticCacheService semanticCacheService;

    public ChatOpsOrchestratorImpl(ChatClient.Builder chatClientBuilder, IComputeService computeService, ISemanticCacheService semanticCacheService) {
        this.chatClient = chatClientBuilder.build();
        this.computeService = computeService;
        this.semanticCacheService = semanticCacheService;
    }


    @Override
    public VmDeploymentRequest analyzeDeploymentRequest(String userMessage) {
        // 1. Check Semantic Cache
        var cachedRequest = semanticCacheService.findSimilarRequest(userMessage);
        if (cachedRequest.isPresent()) {
            System.out.println("🚀 [RAG] Requête similaire trouvée dans le cache sémantique !");
            return cachedRequest.get();
        }

        System.out.println("🤖 [RAG] Aucune requête similaire, appel à Ollama...");
        
        BeanOutputConverter<VmDeploymentRequest> converter = new BeanOutputConverter<>(VmDeploymentRequest.class);
        String formatInstructions = converter.getFormat();

        String response = chatClient.prompt()
                .system(sys -> sys.text(
                                "Tu es l'architecte Cloud de MiniAWS. L'utilisateur va te décrire son besoin. " +
                                        "Tu dois déduire la RAM (en Mo), les vCPUs nécessaires, et le système d'exploitation (os). " +
                                        "Règles strictes : RAM max 4096 Mo, CPU max 2. " +
                                        "Si l'OS n'est pas précisé dans la phrase, mets 'ubuntu'. " +
                                        "Formats d'OS acceptés : 'ubuntu', 'debian'. " +
                                        "IMPORTANT: Tu DOIS répondre UNIQUEMENT avec un JSON valide et bien formaté. N'ajoute AUCUN texte avant ou après le JSON. N'oublie pas l'accolade fermante '}' à la fin." +
                                        "{format_instructions}")
                        .param("format_instructions", formatInstructions))
                .user(userMessage)
                .call()
                .content();

        // Correction "sale" mais efficace si Ollama oublie l'accolade de fin à cause de la RAM/Modèle lent
        if (response != null && !response.trim().endsWith("}")) {
             response = response + "\n}";
        }

        VmDeploymentRequest result = converter.convert(response);
        
        // Save to Cache
        if (result != null) {
            semanticCacheService.saveToCache(userMessage, result);
        }
        
        return result;
    }

    @Override
    public ServerInstance executeMagicDeployment(String userMessage) {
        System.out.println("🧠 1. L'IA analyse la demande : " + userMessage);
        VmDeploymentRequest request = analyzeDeploymentRequest(userMessage);

        System.out.println("🤖 2. L'IA a décidé : " + request.nomServeur() + " (" + request.ramRecommandee() + "Mo RAM) - OS: " + request.os());

        System.out.println("⚙️ 3. Ordre de création envoyé au ComputeService...");
        ServerInstance server = computeService.createServer(request);

        System.out.println("✅ 4. Réponse immédiate envoyée à l'utilisateur.");
        return server;
    }
}
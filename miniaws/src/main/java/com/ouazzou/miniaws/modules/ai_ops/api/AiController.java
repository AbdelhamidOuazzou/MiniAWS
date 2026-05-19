package com.ouazzou.miniaws.modules.ai_ops.api;

import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;
import com.ouazzou.miniaws.modules.ai_ops.service.IChatOpsOrchestrator;
import com.ouazzou.miniaws.modules.ai_ops.service.IDevOpsAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final IDevOpsAgentService devOpsAgentService;
    private final IChatOpsOrchestrator chatOpsOrchestrator;

    // Pour discuter avec l'IA en texte libre
    @GetMapping("/chat")
    public ResponseEntity<String> chatWithDevOps(@RequestParam String message) {
        try {
            return ResponseEntity.ok(devOpsAgentService.askAgent(message));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur de l'IA : " + e.getMessage());
        }
    }

    // Pour que l'IA génère le JSON de configuration de la VM pour lafficehr aux users pour le confirmer
    @GetMapping("/deploy-intent")
    public ResponseEntity<VmDeploymentRequest> analyzeIntent(@RequestParam String message) {
        return ResponseEntity.ok(chatOpsOrchestrator.analyzeDeploymentRequest(message));
    }


    //fait tout
    @PostMapping("/magic-deploy")
    public ResponseEntity<?> magicDeploy(@RequestParam String message) {
        try {
            // Un seul appel, et tout le cloud s'active !
            var server = chatOpsOrchestrator.executeMagicDeployment(message);
            return ResponseEntity.ok(server);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Le déploiement a échoué : " + e.getMessage());
        }
    }
}
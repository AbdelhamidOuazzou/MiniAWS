package com.ouazzou.miniaws.modules.compute.api;

import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;
import com.ouazzou.miniaws.modules.compute.domain.ServerInstance;
import com.ouazzou.miniaws.modules.compute.domain.ServerStatus;
import com.ouazzou.miniaws.modules.compute.service.IComputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/compute/servers")
@RequiredArgsConstructor
public class ComputeController {

    private final IComputeService computeService;

    // ✅ POST /api/compute/servers/create
    // Ton téléphone envoie : { "name": "mon-serveur", "ram": 1024, "cpu": 1 }
    @PostMapping("/create")
    public ResponseEntity<?> createServer(@RequestBody CreateServerRequest request) {
        try {
            // 1. On "traduit" la requête de l'application mobile vers l'objet attendu par ton service
            VmDeploymentRequest deploymentRequest = new VmDeploymentRequest(
                    request.name(),
                    request.ram(),
                    request.cpu(),
                    "Déploiement manuel depuis l'interface classique", // Explication par défaut
                    request.os() // L'OS choisi par l'utilisateur (ou null)
            );

            // 2. On passe l'objet unique, comme on l'a fait pour l'IA !
            ServerInstance server = computeService.createServer(deploymentRequest);

            return ResponseEntity.ok(server);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }

    // ✅ POST /api/compute/servers/{vmId}/start
    @PostMapping("/{proxmoxVmId}/start")
    public ResponseEntity<String> startServer(@PathVariable Integer proxmoxVmId) {
        try {
            computeService.startServer(proxmoxVmId);
            return ResponseEntity.ok("Ordre de démarrage envoyé pour la VM " + proxmoxVmId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }



     // ✅ GET /api/compute/servers (FILTRÉ PAR USER)
    @GetMapping
    public ResponseEntity<List<ServerInstance>> getMyServers(Authentication authentication) {
        String uid = authentication.getName(); // On récupère l'UID du token Firebase
        return ResponseEntity.ok(computeService.getServersByOwner(uid));
    }

    // ✅ GET /api/compute/admin/all (ADMIN UNIQUEMENT)
    @GetMapping("/admin/all")
    public ResponseEntity<List<ServerInstance>> getAllServersForAdmin() {
        return ResponseEntity.ok(computeService.getAllServers());
    }

    // ✅ DELETE /api/compute/servers/{proxmoxVmId}
    @DeleteMapping("/{proxmoxVmId}")
    public ResponseEntity<String> deleteServer(@PathVariable Integer proxmoxVmId) {
        try {
            computeService.deleteServer(proxmoxVmId);
            return ResponseEntity.ok("Serveur supprimé");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ GET /api/compute/servers/status/{status}
    // Filtre par statut : RUNNING, STOPPED, PENDING, ERROR
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ServerInstance>> getServersByStatus(Authentication authentication,
                                                                     @PathVariable ServerStatus status) {
        String uid = authentication.getName();
        // On vérifie si l'utilisateur est ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            // L'admin voit TOUT le datacenter pour ce statut
            return ResponseEntity.ok(computeService.getServersByStatus(status));
        } else {
            // L'utilisateur normal ne voit que SES serveurs pour ce statut
            return ResponseEntity.ok(computeService.getServersByOwnerAndStatus(uid, status));
        }
    }

    // ✅ POST /api/compute/servers/{proxmoxVmId}/stop
    @PostMapping("/{proxmoxVmId}/stop")
    public ResponseEntity<String> stopServer(@PathVariable Integer proxmoxVmId) {
        try {
            computeService.stopServer(proxmoxVmId);
            return ResponseEntity.ok("Ordre d'arrêt (brutal) envoyé pour la VM " + proxmoxVmId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }

    // ✅ GET /api/compute/servers/{proxmoxVmId}/ip
    @GetMapping("/{proxmoxVmId}/ip")
    public ResponseEntity<String> getServerIp(@PathVariable Integer proxmoxVmId) {
        try {
            String ipInfo = computeService.getServerIp(proxmoxVmId);
            if (ipInfo != null && !ipInfo.isEmpty()) {
                return ResponseEntity.ok(ipInfo);
            } else {
                return ResponseEntity.status(404).body("IP introuvable. L'Agent QEMU est-il installé sur la VM ?");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }


   //pour lendpoint createServer
    record CreateServerRequest(String name, Integer ram, Integer cpu,String os) {}
}
package com.ouazzou.miniaws.modules.compute.service;

import com.ouazzou.miniaws.modules.compute.domain.ServerInstance;
import com.ouazzou.miniaws.modules.compute.domain.ServerStatus;
import com.ouazzou.miniaws.modules.compute.infrastructure.IProxmoxApiClient;
import com.ouazzou.miniaws.modules.compute.infrastructure.ProxmoxApiClient;
import com.ouazzou.miniaws.modules.compute.repository.ServerInstanceRepository;
import com.ouazzou.miniaws.modules.iam.domain.AppUser;
import com.ouazzou.miniaws.modules.iam.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComputeServiceImpl implements IComputeService {

    private final ServerInstanceRepository repository;
    private final IProxmoxApiClient proxmoxApiClient;
    private final AppUserRepository userRepository;

    @Value("${proxmox.quota.global.max-ram}")
    private Integer globalMaxRam;

    @Value("${proxmox.quota.global.max-cpu}")
    private Integer globalMaxCpu;

    // on injecte l'interface dans elle-même (Lazy pour éviter la boucle infinie)
    @Lazy
    @Autowired
    private IComputeService self;

    @Override
    public ServerInstance createServer(VmDeploymentRequest request) {
        int templateId = mapOsToTemplateId(request.os());
        log.info("🚀 Déploiement demandé pour l'OS : {} (Template ID : {})",
                request.os() != null ? request.os() : "Ubuntu", templateId);

        if (repository.existsByName(request.nomServeur())) {
            log.error("❌ Déploiement annulé : Le serveur '{}' existe déjà dans la base de données locale.", request.nomServeur());
            throw new RuntimeException("Un serveur nommé '" + request.nomServeur() + "' existe déjà");
        }

        validateQuotas(request.ramRecommandee(), request.cpuRecommande());

        Optional<Integer> vmIdOpt = proxmoxApiClient.cloneVm(request.nomServeur(), templateId);

        if (vmIdOpt.isEmpty()) {
            throw new RuntimeException("Échec du clonage de la VM '" + request.nomServeur() + "' sur Proxmox");
        }

        Integer vraiVmId = vmIdOpt.get();

        log.info("⌛ [Proxmox] VM#{} clonée. Attente courte de 3s pour le déverrouillage...", vraiVmId);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("⚙️ [Proxmox] Configuration RAM/CPU pour la VM#{}...", vraiVmId);
        boolean isConfigured = proxmoxApiClient.configureVm(vraiVmId, request.ramRecommandee(), request.cpuRecommande());
        if (!isConfigured) {
            log.error("Le clonage a réussi (VM#{}) mais l'allocation RAM/CPU a échoué", vraiVmId);
        }
        String currentUid = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        ServerInstance server = ServerInstance.builder()
                .name(request.nomServeur())
                .ramAllocated(request.ramRecommandee())
                .cpuAllocated(request.cpuRecommande())
                .status(ServerStatus.STOPPED)
                .proxmoxVmId(vraiVmId)
                .ownerId(currentUid)
                .build();

        repository.save(server);

        startServer(vraiVmId);
        self.waitForIpAndSaveAsync(vraiVmId);
        return server;
    }

    private int mapOsToTemplateId(String os) {
        if (os == null) return 9000;
        return switch (os.toLowerCase()) {
            case "debian" -> 9001;
            case "ubuntu" -> 9000;
            default -> 9000;
        };
    }

    @Override
    @Async
    public void waitForIpAndSaveAsync(Integer proxmoxVmId) {
        log.info("👻 [ASYNC] Démarrage de la surveillance IP pour la VM #{}", proxmoxVmId);
        for (int i = 0; i < 15; i++) {
            try {
                Thread.sleep(5000);
                String cleanIp = proxmoxApiClient.getVmIp(proxmoxVmId);
                if (cleanIp != null) {
                    repository.findByProxmoxVmId(proxmoxVmId).ifPresent(server -> {
                        server.setIpAddress(cleanIp);
                        server.setStatus(ServerStatus.RUNNING);
                        repository.save(server);
                    });
                    log.info("✅ [ASYNC] Succès : IP {} sauvegardée pour la VM #{}", cleanIp, proxmoxVmId);
                    return;
                }
                log.debug("... [ASYNC] VM #{} : En attente de l'Agent QEMU (Tentative {}/15)", proxmoxVmId, i + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[ASYNC] Surveillance interrompue pour VM #{}", proxmoxVmId);
                break;
            }
        }
        log.warn("❌ [ASYNC] Timeout : Impossible de lire l'IP de la VM #{} après 1m15", proxmoxVmId);
    }

    @Override
    public void startServer(Integer proxmoxVmId) {
        ServerInstance server = repository.findByProxmoxVmId(proxmoxVmId)
                .orElseThrow(() -> new RuntimeException("Serveur VM#" + proxmoxVmId + " introuvable"));

        if (server.getStatus() == ServerStatus.RUNNING) {
            log.warn("VM #{} déjà en cours d'exécution", proxmoxVmId);
            return;
        }

        boolean isStarted = proxmoxApiClient.startVm(proxmoxVmId);
        server.setStatus(isStarted ? ServerStatus.RUNNING : ServerStatus.ERROR);
        repository.save(server);

        if (isStarted) {
            log.info("✅ Serveur '{}' démarré avec succès", server.getName());
        } else {
            log.error("❌ Échec du démarrage du serveur '{}'", server.getName());
        }
    }

    @Override
    public void stopServer(Integer proxmoxVmId) {
        boolean success = proxmoxApiClient.stopVm(proxmoxVmId);
        if (success) {
            repository.findByProxmoxVmId(proxmoxVmId).ifPresent(server -> {
                server.setStatus(ServerStatus.STOPPED);
                repository.save(server);
                log.info("Serveur {} éteint avec succès et mis à jour en BDD.", proxmoxVmId);
            });
        } else {
            throw new RuntimeException("Proxmox a refusé d'arrêter la VM " + proxmoxVmId);
        }
    }

    @Override
    public List<ServerInstance> getServersByOwner(String ownerId) {
            return repository.findByOwnerId(ownerId);
    }

    @Override
     public void deleteServer(Integer proxmoxVmId) {
           ServerInstance server = repository.findByProxmoxVmId(proxmoxVmId)
                     .orElseThrow(() -> new RuntimeException("Serveur introuvable"));

             // 1. On l'arrête d'abord (Proxmox ne peut pas supprimer une VM allumée)
             proxmoxApiClient.stopVm(proxmoxVmId);

             // 2. On attend un peu que Proxmox enregistre l'arrêt
             try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

             // 3. On supprime sur Proxmox
             boolean deleted = proxmoxApiClient.deleteVm(proxmoxVmId);

             if (deleted) {
                     // 4. On supprime de notre base de données locale
                     repository.delete(server);
                     log.info("🗑️ Serveur {} (VM#{}) supprimé avec succès.", server.getName(), proxmoxVmId);
                 } else {
                     throw new RuntimeException("Échec de la suppression sur Proxmox.");
                 }
         }
    @Override
    public List<ServerInstance> getServersByOwnerAndStatus(String ownerId, ServerStatus status) {
            return repository.findByOwnerIdAndStatus(ownerId, status);
    }

    @Override
    public String getServerIp(Integer proxmoxVmId) {
        return proxmoxApiClient.getVmIp(proxmoxVmId);
    }

    @Override
    public List<ServerInstance> getAllServers() {
        return repository.findAll();
    }

    @Override
    public List<ServerInstance> getServersByStatus(ServerStatus status) {
        return repository.findByStatus(status);
    }

    private void validateQuotas(Integer ram, Integer cpu) {

        // =================================================================
        // 🌍 ÉTAPE 1 : VÉRIFICATION DE LA CAPACITÉ GLOBALE (Le Datacenter)
        // =================================================================
        Integer totalGlobalRam = repository.sumRamAllocatedGlobal();
        Integer totalGlobalCpu = repository.sumCpuAllocatedGlobal();

        int currentGlobalRam = totalGlobalRam != null ? totalGlobalRam : 0;
        int currentGlobalCpu = totalGlobalCpu != null ? totalGlobalCpu : 0;

        if (currentGlobalRam + ram > globalMaxRam) {
            // On ne dit pas combien il reste aux utilisateurs normaux pour des raisons de sécurité,
            // on dit juste que le cloud est plein ! (Comme AWS : "InsufficientCapacityException")
            throw new RuntimeException("Capacité MiniAWS épuisée : Impossible d'allouer la RAM demandée pour le moment. Réessayez plus tard.");
        }
        if (currentGlobalCpu + cpu > globalMaxCpu) {
            throw new RuntimeException("Capacité MiniAWS épuisée : Plus aucun vCPU disponible sur l'infrastructure physique.");
        }

        // =================================================================
        // 👤 ÉTAPE 2 : VÉRIFICATION DU QUOTA PERSONNEL (L'Utilisateur)
        // =================================================================
        String currentUid = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = userRepository.findById(currentUid)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans le système IAM"));

        Integer totalPersonalRam = repository.sumRamAllocatedByOwnerId(currentUid);
        Integer totalPersonalCpu = repository.sumCpuAllocatedByOwnerId(currentUid);

        int currentPersonalRam = totalPersonalRam != null ? totalPersonalRam : 0;
        int currentPersonalCpu = totalPersonalCpu != null ? totalPersonalCpu : 0;

        if (currentPersonalRam + ram > currentUser.getMaxRamAllowed()) {
            throw new RuntimeException("RAM insuffisante — " + (currentUser.getMaxRamAllowed() - currentPersonalRam) +
                    " Mo disponibles sur votre quota de " + currentUser.getMaxRamAllowed() + " Mo");
        }
        if (currentPersonalCpu + cpu > currentUser.getMaxCpuAllowed()) {
            throw new RuntimeException("CPU insuffisant — " + (currentUser.getMaxCpuAllowed() - currentPersonalCpu) +
                    " vCPUs disponibles sur votre quota de " + currentUser.getMaxCpuAllowed());
        }
    }
}
package com.ouazzou.miniaws.modules.monitoring.service;

import com.ouazzou.miniaws.modules.compute.domain.ServerInstance;
import com.ouazzou.miniaws.modules.compute.domain.ServerStatus;
import com.ouazzou.miniaws.modules.compute.service.IComputeService;
import com.ouazzou.miniaws.modules.compute.infrastructure.IProxmoxApiClient;
import com.ouazzou.miniaws.modules.monitoring.domain.VmMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    //Il permet d'envoyer des messages à tous les clients connectés sur un topic donné
    private final SimpMessagingTemplate messagingTemplate;

    // On importe les interfaces du module Compute
    private final IComputeService computeService;
    private final IProxmoxApiClient proxmoxApiClient;

    @Scheduled(fixedRate = 2000)
    public void broadcastRealMetrics() {
        // 1. On récupère TOUS les serveurs qui sont censés être allumés en base de données
        List<ServerInstance> runningServers = computeService.getServersByStatus(ServerStatus.RUNNING);

        if (runningServers.isEmpty()) {
            return; // Rien à faire si aucune VM ne tourne
        }

        // 2. Pour chaque serveur allumé, on demande les vraies stats à Proxmox
        for (ServerInstance server : runningServers) {
            try {
                VmMetrics realMetrics = proxmoxApiClient.getVmStats(server.getProxmoxVmId());
                // 1. On envoie au propriétaire uniquement
                messagingTemplate.convertAndSend("/topic/metrics/user/" + server.getOwnerId(), realMetrics);
                // 2. On envoie aussi au canal Admin (pour que l'admin voit TOUT)
                messagingTemplate.convertAndSend("/topic/metrics/admin", realMetrics);

            } catch (Exception e) {
                log.error("Erreur lors de la diffusion des métriques pour la VM {}", server.getProxmoxVmId());
            }
        }
    }
}
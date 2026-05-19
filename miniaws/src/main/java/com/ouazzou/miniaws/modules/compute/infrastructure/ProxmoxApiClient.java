package com.ouazzou.miniaws.modules.compute.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ouazzou.miniaws.modules.monitoring.domain.VmMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ProxmoxApiClient implements IProxmoxApiClient {

    @Value("${proxmox.api.url}")
    private String proxmoxUrl;

    @Value("${proxmox.api.token}")
    private String apiToken;

    @Value("${proxmox.node.name:pve}")
    private String nodeName;

    // L'ID de notre Template Golden

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public ProxmoxApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper(); // Instancié une seule fois
    }

    // 1. Cloner le template
    public Optional<Integer> cloneVm(String name, int templateId) {
        // Utilise le templateId dans l'URL au lieu d'une variable fixe
        String url = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + templateId + "/clone";
        int newVmId = generateVmId();
        String safeName = name.replaceAll("[^a-zA-Z0-9\\-]", "-").toLowerCase();

        Map<String, Object> body = new HashMap<>();
        body.put("newid", newVmId);
        body.put("name", safeName);
        body.put("full", 0);

        try {
            HttpEntity<String> request = buildRequest(mapper.writeValueAsString(body));
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("VM #{} clonée avec succès à partir du template {}", newVmId, templateId);
                return Optional.of(newVmId);
            }
        } catch (Exception e) {
            log.error("Erreur lors du clonage de la VM '{}' : {}", name, e.getMessage());
        }
        return Optional.empty();
    }

    // 2. Configurer la RAM et le CPU du clone
    public boolean configureVm(Integer vmId, Integer ram, Integer cpu) {
        String url = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + vmId + "/config";

        Map<String, Object> body = new HashMap<>();
        body.put("memory", ram);
        body.put("cores", cpu);

        try {
            HttpEntity<String> request = buildRequest(mapper.writeValueAsString(body));
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("VM #{} configurée avec {}MB de RAM et {} Cores", vmId, ram, cpu);
                return true;
            }
        } catch (Exception e) {
            log.error("Erreur de configuration pour la VM #{} : {}", vmId, e.getMessage());
        }
        return false;
    }

    // 3. Démarre la VM
    public boolean startVm(Integer vmId) {
        String url = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + vmId + "/status/start";
        try {
            HttpEntity<String> request = buildRequest("{}");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("VM #{} démarrée, statut : {}", vmId, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Erreur démarrage VM #{} : {}", vmId, e.getMessage());
            return false;
        }
    }

    // 4. Stoppe la VM
    public boolean stopVm(Integer vmId) {
        String url = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + vmId + "/status/stop";
        try {
            HttpEntity<String> request = buildRequest("{}");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("VM #{} : Signal d'arrêt envoyé.", vmId);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Erreur arrêt VM #{} : {}", vmId, e.getMessage());
            return false;
        }
    }
    @Override
    public boolean deleteVm(Integer vmId) {
             String url = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + vmId;
            try {
                     HttpEntity<String> request = buildRequest(null);
                     // On utilise exchange avec HttpMethod.DELETE
                     ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
                     log.info("VM #{} : Requête de suppression envoyée, statut : {}", vmId, response.getStatusCode());
                     return response.getStatusCode().is2xxSuccessful();
                 } catch (Exception e) {
                     log.error("Erreur lors de la suppression de la VM #{} : {}", vmId, e.getMessage());
                     return false;
                }
        }

    // 5. Récupère l'IP exacte de la VM en parsant le JSON
    public String getVmIp(Integer vmId) {
        String url = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + vmId + "/agent/network-get-interfaces";

        try {
            // Un GET avec des headers (Body à null)
            HttpEntity<String> request = buildRequest(null);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Lecture de l'arbre JSON
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode results = root.path("data").path("result"); // Proxmox met la réponse dans data -> result

                for (JsonNode interfaceNode : results) {
                    String ifaceName = interfaceNode.path("name").asText();

                    // On ignore l'interface locale (loopback)
                    if ("lo".equals(ifaceName)) continue;

                    JsonNode ips = interfaceNode.path("ip-addresses");
                    for (JsonNode ipNode : ips) {
                        // On cherche uniquement les adresses IPv4
                        if ("ipv4".equals(ipNode.path("ip-address-type").asText())) {
                            String ip = ipNode.path("ip-address").asText();
                            // Double vérification pour éviter le 127.0.0.1
                            if (!"127.0.0.1".equals(ip)) {
                                return ip; // On a trouvé notre IP !
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // L'erreur est normale si la VM vient juste de démarrer et que l'agent n'est pas encore prêt
            log.debug("Agent réseau non prêt pour VM #{} : {}", vmId, e.getMessage());
        }
        return null;
    }

    @Override
    public VmMetrics getVmStats(int vmId) {
        try {
            String url = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + vmId + "/status/current";

            HttpEntity<String> request = buildRequest(null);

            // On lance la requête avec le bon request (qui contient le Token)
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, JsonNode.class);

            JsonNode data = response.getBody().get("data");

            // 1. Le statut (running, stopped)
            String status = data.get("status").asText();

            // Si la machine est éteinte, CPU et RAM sont à 0
            if (!"running".equals(status)) {
                return new VmMetrics(vmId, 0.0, 0.0, status.toUpperCase());
            }

            // 2. Calcul du CPU (Proxmox renvoie un chiffre comme 0.05 pour 5%)
            double cpuUsage = data.get("cpu").asDouble() * 100;

            // 3. Calcul de la RAM (Proxmox renvoie les octets utilisés et le max)
            double memUsed = data.get("mem").asDouble();
            double memMax = data.get("maxmem").asDouble();
            double ramUsage = (memUsed / memMax) * 100;

            return new VmMetrics(vmId, cpuUsage, ramUsage, "RUNNING");

        } catch (Exception e) {
            log.error("Impossible de récupérer les stats de la VM {}", vmId, e);
            // En cas d'erreur réseau, on renvoie des zéros pour ne pas faire planter l'application
            return new VmMetrics(vmId, 0.0, 0.0, "ERROR");
        }
    }
    // Méthode utilitaire pour centraliser les Headers
    private <T> HttpEntity<T> buildRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "PVEAPIToken=" + apiToken);
        return new HttpEntity<>(body, headers);
    }

    // Génère un vmId unique entre 100 et 9999
    private Integer generateVmId() {
        return (int) (Math.random() * 9900) + 100;
    }
}
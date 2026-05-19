package com.ouazzou.miniaws.modules.iam.service;

import com.ouazzou.miniaws.modules.iam.domain.AppUser;
import com.ouazzou.miniaws.modules.iam.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IamServiceImpl implements IIamService {

    private final AppUserRepository userRepository;


    @Value("${miniaws.admin.email}")
    private String adminEmail;

    @Override
    public AppUser getOrCreateUser(String firebaseUid, String email) {
        Optional<AppUser> existingUser = userRepository.findById(firebaseUid);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        log.info("🌟 Nouvel utilisateur détecté : {}", email);

        String assignedRole = "ROLE_USER";
        int assignedMaxServers = 3;
        int assignedMaxCpu = 4;
        int assignedMaxRam = 4096;

        // Si l'email correspond à celui du patron...
        if (email != null && email.equalsIgnoreCase(adminEmail)) {
            assignedRole = "ROLE_ADMIN";
            assignedMaxServers = 1000;   // L'admin fait ce qu'il veut
            assignedMaxCpu = 100;        // Open bar sur les CPUs
            assignedMaxRam = 256000;     // Open bar sur la RAM
            log.warn("👑 ATTENTION : Création du compte Super Administrateur !");
        }

        AppUser newUser = AppUser.builder()
                .firebaseUid(firebaseUid)
                .email(email)
                .role(assignedRole)
                .maxServersAllowed(assignedMaxServers)
                .maxCpuAllowed(assignedMaxCpu)
                .maxRamAllowed(assignedMaxRam)
                .build();

        return userRepository.save(newUser);
    }

    @Override
    public AppUser getUserProfile(String firebaseUid) {
        return userRepository.findById(firebaseUid)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé en BDD"));
    }
}
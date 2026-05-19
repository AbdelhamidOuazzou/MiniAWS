package com.ouazzou.miniaws.modules.iam.api;

import com.ouazzou.miniaws.modules.iam.domain.AppUser;
import com.ouazzou.miniaws.modules.iam.repository.AppUserRepository;
import com.ouazzou.miniaws.modules.iam.service.IIamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/iam")
@RequiredArgsConstructor
public class IamController {

    private final IIamService iamService;

    /**
     * Point d'entrée pour récupérer son propre profil.
     * L'objet "Authentication" est injecté automatiquement par Spring Security
     * après avoir lu le Token Firebase !
     */
    @GetMapping("/me")
    //Spring voit le paramètre Authentication → va le chercher dans le SecurityContextHolder → l'injecte directement
    public ResponseEntity<AppUser> getMyProfile(Authentication authentication) {

        // 1. On récupère l'UID Firebase depuis le token sécurisé
        //getName() retourne le premier paramètre du UsernamePasswordAuthenticationToken créé dans le filtre
        String myUid = authentication.getName();

        // 2. On délègue au Service ! Fini le "userRepository.findById()" ici.
        AppUser myUser = iamService.getUserProfile(myUid);

        return ResponseEntity.ok(myUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AppUser> loginOrSyncUser(Authentication authentication, @RequestBody Map<String, String> requestData) {
        String myUid = authentication.getName();
        String email = requestData.get("email"); // L'Android enverra l'email

        AppUser user = iamService.getOrCreateUser(myUid, email);
        return ResponseEntity.ok(user);
    }
}
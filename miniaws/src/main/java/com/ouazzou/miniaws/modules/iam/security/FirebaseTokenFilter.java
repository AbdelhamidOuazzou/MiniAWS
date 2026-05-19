package com.ouazzou.miniaws.modules.iam.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.ouazzou.miniaws.modules.iam.domain.AppUser;
import com.ouazzou.miniaws.modules.iam.service.IIamService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final IIamService iamService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. On cherche le "badge" dans l'en-tête HTTP
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // On enlève le mot "Bearer "

            try {
                // 2. Le SDK Firebase vérifie mathématiquement que le token n'est pas falsifié
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                String uid = decodedToken.getUid();
                String email = decodedToken.getEmail();

                // 3. On cherche l'utilisateur dans la BDD ou on le crée
                AppUser user = iamService.getOrCreateUser(uid, email);

                // 4. On donne le feu vert à Spring Security
                String role = user.getRole();
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }
                //objet Spring Security qui représente "cet utilisateur est authentifié
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                 uid, null, Collections.singletonList(new SimpleGrantedAuthority(role))
                         );// L'utilisateur est connecté !
                //Il stocke l'authentification pour toute la durée de la requête
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // Le token est faux, expiré, ou bidouillé -> Porte fermée !
                System.err.println("❌ Accès refusé : Token invalide (" + e.getMessage() + ")");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // On passe la requête au suivant (vers ton Controller)
        filterChain.doFilter(request, response);
    }
}
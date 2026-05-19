package com.ouazzou.miniaws.modules.iam.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
//n'utilise pas la sécurité par défaut, utilise ma configuration à moi
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;

    @Bean
    //C'est LE bean central de Spring Security. Il définit toutes les règles de sécurité de l'application
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // On désactive CSRF car on utilise des Tokens (REST API) pas de cookies
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Pas de cookies de session
                .authorizeHttpRequests(auth -> auth
                             .requestMatchers("/api/ai/chat").permitAll()
                             .requestMatchers("/ws/monitoring").permitAll()                             // Route Admin
                             .requestMatchers("/api/compute/servers/admin/**").hasRole("ADMIN")
                             .anyRequest().authenticated())
                // On place notre Vigile Firebase JUSTE AVANT le filtre de base de Spring
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
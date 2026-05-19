package com.ouazzou.miniaws.modules.monitoring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // C'est l'URL à laquelle l'application Android va se connecter pour ouvrir le tunnel.
        // On autorise toutes les origines pour faciliter tes tests depuis le téléphone.
        registry.addEndpoint("/ws/monitoring")
                //accepte les connexions depuis n'importe quelle origine (Android, browser, etc.)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Le serveur va "diffuser" ses messages sur les adresses commençant par /topic
        registry.enableSimpleBroker("/topic");

        // Si l'Android veut envoyer un message au serveur via WebSocket, il utilisera le préfixe /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}
package com.ouazzou.miniaws.modules.iam.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct    //exécute cette méthode une seule fois, juste après que Spring ait créé ce bean
    public void initFirebase() {
        try {
            // On va chercher le fichier JSON
            InputStream serviceAccount = getClass().getClassLoader()
                    .getResourceAsStream("firebase-service-account.json");

            if (serviceAccount == null) {
                throw new RuntimeException("❌ Fichier firebase-service-account.json introuvable !");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // On connecte officiellement ton Spring Boot aux serveurs de Google
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("🔥 Firebase Admin SDK initialisé avec succès !");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'initialisation de Firebase :");
            e.printStackTrace();
        }
    }
}
package com.ouazzou.miniaws.ui.voice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.ServerInstance;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceCommandActivity extends AppCompatActivity {

    private TextView tvVoiceResult;
    private FloatingActionButton fabMicrophone;
    private MaterialButton btnSendToApi;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    // Coordonnées GPS reçues du Dashboard
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_command);

        // 1. Récupération du GPS
        latitude = getIntent().getDoubleExtra("LATITUDE", 0.0);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 0.0);

        // 2. Liaison XML
        tvVoiceResult = findViewById(R.id.tvVoiceResult);
        fabMicrophone = findViewById(R.id.fabMicrophone);
        btnSendToApi = findViewById(R.id.btnSendToApi);

        // 4. Configuration du moteur vocal Google
        setupSpeechRecognizer();

        // 4. Action : Bouton Micro
        fabMicrophone.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            } else {
                startListening();
            }
        });

        // 5. Action : Bouton Déployer (Envoi à Spring Boot)
        btnSendToApi.setOnClickListener(v -> sendCommandToSpringBoot());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ==========================================
    // 🧠 LOGIQUE RÉSEAU (API SPRING BOOT)
    // ==========================================
    private void sendCommandToSpringBoot() {
        String commandText = tvVoiceResult.getText().toString();

        // On bloque le bouton pendant le chargement
        btnSendToApi.setEnabled(false);
        btnSendToApi.setText("DÉPLOIEMENT EN COURS (OLLAMA)...");
        btnSendToApi.setBackgroundColor(Color.GRAY);

        // Appel de l'API via Retrofit (Nouvelle route magicDeploy)
        ApiClient.getApi().magicDeploy(commandText).enqueue(new Callback<ServerInstance>() {
            @Override
            public void onResponse(Call<ServerInstance> call, Response<ServerInstance> response) {
                btnSendToApi.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ServerInstance server = response.body();
                    Toast.makeText(VoiceCommandActivity.this, "✅ Succès ! Serveur " + server.getName() + " créé.", Toast.LENGTH_LONG).show();

                    // On ferme cet écran pour retourner au Dashboard
                    finish();
                } else {
                    btnSendToApi.setText("RÉESSAYER");
                    btnSendToApi.setBackgroundColor(Color.RED);
                    Toast.makeText(VoiceCommandActivity.this, "❌ Erreur API : " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ServerInstance> call, Throwable t) {
                btnSendToApi.setEnabled(true);
                btnSendToApi.setText("RÉESSAYER");
                btnSendToApi.setBackgroundColor(Color.RED);
                Toast.makeText(VoiceCommandActivity.this, "⚠️ Serveur injoignable : " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API_ERROR", "Erreur : ", t);
            }
        });
    }

    // ==========================================
    // 🎙️ LOGIQUE VOCALE (SPEECH TO TEXT)
    // ==========================================
    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvVoiceResult.setText("Écoute en cours...");
                tvVoiceResult.setTextColor(Color.parseColor("#03DAC5")); // Cyan
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    // On affiche le texte reconnu
                    tvVoiceResult.setText(matches.get(0));
                    tvVoiceResult.setTextColor(Color.WHITE);

                    // On fait apparaître le bouton d'envoi à l'API !
                    btnSendToApi.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(int error) {
                tvVoiceResult.setText("Erreur d'écoute. Rappuyez sur le micro.");
                tvVoiceResult.setTextColor(Color.RED);
            }

            // Méthodes inutilisées mais obligatoires
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        btnSendToApi.setVisibility(View.GONE);
        speechRecognizer.startListening(speechRecognizerIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            Toast.makeText(this, "La permission du micro est obligatoire.", Toast.LENGTH_SHORT).show();
        }
    }
}
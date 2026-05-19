package com.ouazzou.miniaws.ui.voice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnMic;
    private List<ChatMessage> messageList = new ArrayList<>();
    
    private WebView sceneView;
    private volatile boolean isAiTalking = false;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isListening = false;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerViewChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        sceneView = findViewById(R.id.sceneViewChat);

        setup3DAvatar();
        setupSpeechToText();
        setupTextToSpeech();

        adapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Scroll automatique quand le clavier apparaît
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && messageList.size() > 0) {
                recyclerView.postDelayed(() -> recyclerView.scrollToPosition(messageList.size() - 1), 100);
            }
        });

        btnSend.setOnClickListener(v -> sendMessage());
        btnMic.setOnClickListener(v -> toggleListening());

        addMessage("Bonjour ! Je suis l'Agent IA de MiniAWS. Comment puis-je vous aider ?", false);
    }

    private void setupSpeechToText() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                etMessage.setHint("Écoute en cours...");
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                etMessage.setHint("Traitement...");
            }
            @Override public void onError(int error) {
                isListening = false;
                btnMic.setColorFilter(ContextCompat.getColor(ChatActivity.this, R.color.neon_cyan));
                etMessage.setHint("Ask the Cloud Orchestrator...");
                Toast.makeText(ChatActivity.this, "Erreur de reconnaissance vocale", Toast.LENGTH_SHORT).show();
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                btnMic.setColorFilter(ContextCompat.getColor(ChatActivity.this, R.color.neon_cyan));
                etMessage.setHint("Ask the Cloud Orchestrator...");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    etMessage.setText(matches.get(0));
                    sendMessage();
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void toggleListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
            return;
        }

        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            btnMic.setColorFilter(ContextCompat.getColor(this, R.color.neon_cyan));
            etMessage.setHint("Ask the Cloud Orchestrator...");
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizer.startListening(intent);
            isListening = true;
            btnMic.setColorFilter(ContextCompat.getColor(this, R.color.neon_purple));
        }
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.FRENCH);
                
                textToSpeech.setPitch(0.7f);
                
                try {
                    for (android.speech.tts.Voice voice : textToSpeech.getVoices()) {
                        if (voice.getName().toLowerCase().contains("male") && voice.getLocale().getLanguage().startsWith("fr")) {
                            textToSpeech.setVoice(voice);
                            break;
                        }
                    }
                } catch (Exception e) {
                }

                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { 
                        isAiTalking = true;
                        runOnUiThread(() -> {
                            if(sceneView != null) {
                                sceneView.evaluateJavascript("setTalking(true);", null);
                            }
                        });
                    }
                    @Override public void onDone(String utteranceId) { 
                        isAiTalking = false;
                        runOnUiThread(() -> {
                            if(sceneView != null) {
                                sceneView.evaluateJavascript("setTalking(false);", null);
                            }
                        });
                    }
                    @Override public void onError(String utteranceId) { 
                        isAiTalking = false;
                        runOnUiThread(() -> {
                            if(sceneView != null) {
                                sceneView.evaluateJavascript("setTalking(false);", null);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void setup3DAvatar() {
        sceneView.setBackgroundColor(0); // Transparent
        
        android.webkit.WebSettings webSettings = sceneView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Requis pour les Web Components modernes
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        sceneView.setWebChromeClient(new android.webkit.WebChromeClient());
        sceneView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("models/avatar.glb")) {
                    try {
                        java.io.InputStream is = getAssets().open("models/avatar.glb");
                        return new android.webkit.WebResourceResponse("model/gltf-binary", "UTF-8", is);
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        
        try {
            java.io.InputStream is = getAssets().open("avatar.html");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String html = new String(buffer, "UTF-8");
            // Utiliser une fausse URL https:// pour contourner les blocages stricts de fetch() sur file://
            sceneView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessage(text, true);
        etMessage.setText("");

        ApiClient.getApi().chat(text).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String aiResponse = response.body().string();
                        addMessage(aiResponse, false);
                        
                        if (textToSpeech != null) {
                            textToSpeech.speak(aiResponse, TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE");
                        }
                    } catch (java.io.IOException e) {
                        addMessage("Erreur lors de la lecture de la réponse.", false);
                    }
                } else {
                    addMessage("Désolé, je rencontre une erreur serveur.", false);
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                addMessage("Erreur réseau : impossible de contacter l'IA.", false);
            }
        });
    }

    private void addMessage(String text, boolean isUser) {
        messageList.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accordée. Vous pouvez parler.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission micro refusée.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
package com.ouazzou.miniaws.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.location.GpsTracker;
import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.AppUser;
import com.ouazzou.miniaws.ui.manual.ManualDeployActivity;
import com.ouazzou.miniaws.ui.scanner.QrScannerActivity;
import com.ouazzou.miniaws.ui.voice.VoiceCommandActivity;
import com.ouazzou.miniaws.ui.voice.ChatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardVoiceCommand;
    private MaterialCardView cardQrScanner;
    private MaterialCardView cardTextManual;
    private MaterialCardView cardMonitoring;
    private MaterialCardView cardAiChat;
    private ImageButton btnLogout;
    private TextView tvTitle;
    private TextView tvGpsStatus;

    private GpsTracker gpsTracker;
    private AppUser currentUser;

    // On stocke les coordonnées pour les envoyer aux autres écrans
    private double currentLat = 0.0;
    private double currentLon = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Liaison avec le design XML
        tvTitle = findViewById(R.id.tvTitle);
        btnLogout = findViewById(R.id.btnLogout);
        
        // Clic sur le titre/avatar pour voir le profil
        tvTitle.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardVoiceCommand = findViewById(R.id.cardVoiceCommand);
        cardQrScanner = findViewById(R.id.cardQrScanner);
        cardTextManual = findViewById(R.id.cardTextManual);
        cardMonitoring = findViewById(R.id.cardMonitoring);
        cardAiChat = findViewById(R.id.cardAiChat);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);

        // 2. Lancement du GPS en arrière-plan
        gpsTracker = new GpsTracker(this);
        fetchLocation();

        // 3. Récupération du profil utilisateur
        fetchUserProfile();

        // 4. Configuration des boutons (Navigation)

        btnLogout.setOnClickListener(v -> handleLogout());

        cardVoiceCommand.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VoiceCommandActivity.class);
            intent.putExtra("LATITUDE", currentLat);
            intent.putExtra("LONGITUDE", currentLon);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardQrScanner.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QrScannerActivity.class);
            intent.putExtra("LATITUDE", currentLat);
            intent.putExtra("LONGITUDE", currentLon);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardTextManual.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ManualDeployActivity.class);
            intent.putExtra("LATITUDE", currentLat);
            intent.putExtra("LONGITUDE", currentLon);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardMonitoring.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.ouazzou.miniaws.ui.monitor.VmListActivity.class);
            if (currentUser != null) {
                intent.putExtra("USER_ROLE", currentUser.getRole());
                intent.putExtra("USER_UID", currentUser.getFirebaseUid());
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void handleLogout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        com.ouazzou.miniaws.utils.Constants.CURRENT_TOKEN = "";
        Intent intent = new Intent(MainActivity.this, com.ouazzou.miniaws.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private void fetchUserProfile() {
        ApiClient.getApi().getMyProfile().enqueue(new Callback<AppUser>() {
            @Override
            public void onResponse(Call<AppUser> call, Response<AppUser> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateUiForRole();
                }
            }

            @Override
            public void onFailure(Call<AppUser> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erreur profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUiForRole() {
        if (currentUser != null && currentUser.getRole() != null) {
            boolean isAdmin = currentUser.getRole().toUpperCase().contains("ADMIN");
            String roleText = isAdmin ? " (ADMIN)" : "";
            tvTitle.setText("MINI AWS" + roleText);

            if (isAdmin) {
                tvTitle.setTextColor(getResources().getColor(R.color.neon_cyan));
            }
        }
    }

    private void fetchLocation() {
        tvGpsStatus.setText("● SYSTEM BOOTING...");
        tvGpsStatus.setTextColor(getResources().getColor(R.color.neon_orange));

        gpsTracker.getCurrentLocation(this, new GpsTracker.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLat = latitude;
                currentLon = longitude;

                tvGpsStatus.setText(String.format("● SYSTEM ONLINE (%.2f, %.2f)", latitude, longitude));
                tvGpsStatus.setTextColor(getResources().getColor(R.color.neon_green));
            }

            @Override
            public void onError(String errorMessage) {
                tvGpsStatus.setText("● GPS OFFLINE");
                tvGpsStatus.setTextColor(getResources().getColor(R.color.neon_red));
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
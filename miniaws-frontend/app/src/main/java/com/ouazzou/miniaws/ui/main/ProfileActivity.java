package com.ouazzou.miniaws.ui.main;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.AppUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileEmail, tvProfileRole, tvProfileUid;
    private TextView tvQuotaServers, tvQuotaCpu, tvQuotaRam;
    private MaterialButton btnCloseProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvProfileUid = findViewById(R.id.tvProfileUid);
        tvQuotaServers = findViewById(R.id.tvQuotaServers);
        tvQuotaCpu = findViewById(R.id.tvQuotaCpu);
        tvQuotaRam = findViewById(R.id.tvQuotaRam);
        btnCloseProfile = findViewById(R.id.btnCloseProfile);

        btnCloseProfile.setOnClickListener(v -> finish());

        fetchProfile();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void fetchProfile() {
        ApiClient.getApi().getMyProfile().enqueue(new Callback<AppUser>() {
            @Override
            public void onResponse(Call<AppUser> call, Response<AppUser> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppUser user = response.body();
                    updateUi(user);
                } else {
                    Toast.makeText(ProfileActivity.this, "Erreur lors de la récupération du profil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AppUser> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUi(AppUser user) {
        tvProfileEmail.setText(user.getEmail());
        tvProfileRole.setText(user.getRole());
        tvProfileUid.setText("UID: " + user.getFirebaseUid());
        
        // On récupère les quotas réels depuis le backend
        tvQuotaServers.setText(String.valueOf(user.getMaxServersAllowed()));
        tvQuotaCpu.setText(user.getMaxCpuAllowed() + " vCPUs");
        tvQuotaRam.setText(user.getMaxRamAllowed() + " MB");
    }
}
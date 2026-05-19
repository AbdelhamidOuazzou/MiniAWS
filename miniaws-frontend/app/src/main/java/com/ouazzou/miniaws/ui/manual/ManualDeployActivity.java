package com.ouazzou.miniaws.ui.manual;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.ServerInstance;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManualDeployActivity extends AppCompatActivity {

    private TextInputEditText etCommand;
    private MaterialButton btnSendManual;

    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_deploy);

        latitude = getIntent().getDoubleExtra("LATITUDE", 0.0);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 0.0);

        etCommand = findViewById(R.id.etCommand);
        btnSendManual = findViewById(R.id.btnSendManual);

        btnSendManual.setOnClickListener(v -> sendCommand());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void sendCommand() {
        String commandText = etCommand.getText().toString().trim();
        if (commandText.isEmpty()) {
            Toast.makeText(this, "Veuillez écrire une commande", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendManual.setEnabled(false);
        btnSendManual.setText("DÉPLOIEMENT EN COURS...");
        btnSendManual.setBackgroundColor(Color.GRAY);

        ApiClient.getApi().magicDeploy(commandText).enqueue(new Callback<ServerInstance>() {
            @Override
            public void onResponse(Call<ServerInstance> call, Response<ServerInstance> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ManualDeployActivity.this, "✅ Serveur " + response.body().getName() + " déployé !", Toast.LENGTH_LONG).show();
                    finish(); // Retour au Dashboard
                } else {
                    btnSendManual.setEnabled(true);
                    btnSendManual.setText("RÉESSAYER");
                    btnSendManual.setBackgroundColor(Color.RED);
                    String errorMsg = "Erreur " + response.code();
                    if (response.code() == 401) errorMsg = "Non autorisé (Problème de Token)";
                    if (response.code() == 404) errorMsg = "Route non trouvée sur le serveur";
                    Toast.makeText(ManualDeployActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ServerInstance> call, Throwable t) {
                btnSendManual.setEnabled(true);
                btnSendManual.setText("RÉESSAYER");
                btnSendManual.setBackgroundColor(Color.RED);
                Toast.makeText(ManualDeployActivity.this, "Connexion impossible : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
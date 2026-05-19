package com.ouazzou.miniaws.ui.monitor;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.ouazzou.miniaws.R;

import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.ServerInstance;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VmDetailActivity extends AppCompatActivity {

    private TextView tvDetailName, tvDetailIp, tvDetailVmId, tvDetailRam, tvDetailCpu, tvDetailOs;
    private MaterialButton btnStopServer;
    private Integer vmId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vm_detail);

        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailIp = findViewById(R.id.tvDetailIp);
        tvDetailVmId = findViewById(R.id.tvDetailVmId);
        tvDetailRam = findViewById(R.id.tvDetailRam);
        tvDetailCpu = findViewById(R.id.tvDetailCpu);
        tvDetailOs = findViewById(R.id.tvDetailOs);
        btnStopServer = findViewById(R.id.btnStopServer);

        // Récupération des infos envoyées par la liste
        String serverName = getIntent().getStringExtra("SERVER_NAME");
        String serverIp = getIntent().getStringExtra("SERVER_IP");
        vmId = getIntent().getIntExtra("SERVER_VMID", -1);
        int ram = getIntent().getIntExtra("SERVER_RAM", 0);
        int cpu = getIntent().getIntExtra("SERVER_CPU", 0);
        String status = getIntent().getStringExtra("SERVER_STATUS");

        tvDetailName.setText(serverName != null ? serverName : "Serveur Inconnu");
        tvDetailIp.setText(serverIp != null ? serverIp : "IP en cours d'attribution...");
        tvDetailVmId.setText(String.valueOf(vmId));
        tvDetailRam.setText(ram + " MB");
        tvDetailCpu.setText(String.valueOf(cpu));
        tvDetailOs.setText("Linux (Ubuntu/Debian)");

        if ("RUNNING".equals(status)) {
            btnStopServer.setText("ARRÊTER L'INSTANCE");
            btnStopServer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF5252")));
        } else {
            btnStopServer.setText("DÉMARRER L'INSTANCE");
            btnStopServer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E676")));
        }

        btnStopServer.setOnClickListener(v -> toggleServerState(status));
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void toggleServerState(String currentStatus) {
        btnStopServer.setEnabled(false);
        btnStopServer.setText("CHARGEMENT...");

        Call<okhttp3.ResponseBody> call = "RUNNING".equals(currentStatus) 
            ? ApiClient.getApi().stopServer(vmId) 
            : ApiClient.getApi().startServer(vmId);

        call.enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VmDetailActivity.this, "Action réussie", Toast.LENGTH_SHORT).show();
                    finish(); // On revient à la liste pour voir le changement
                } else {
                    Toast.makeText(VmDetailActivity.this, "Erreur API", Toast.LENGTH_SHORT).show();
                    btnStopServer.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(VmDetailActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                btnStopServer.setEnabled(true);
            }
        });
    }
}
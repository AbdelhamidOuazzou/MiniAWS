package com.ouazzou.miniaws.ui.monitor;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.ServerInstance;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.gson.Gson;
import com.ouazzou.miniaws.models.VmMetrics;
import com.ouazzou.miniaws.utils.Constants;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class VmListActivity extends AppCompatActivity implements VmAdapter.OnVmActionListener {

    private RecyclerView recyclerView;
    private VmAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ChipGroup chipGroupFilters;
    private List<ServerInstance> serverList = new ArrayList<>();
    private String userRole = "USER";
    private String userUid = "";
    private String currentStatusFilter = "ALL";
    private WebSocket webSocket;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vm_list);

        userRole = getIntent().getStringExtra("USER_ROLE");
        userUid = getIntent().getStringExtra("USER_UID");
        
        if (userRole == null) userRole = "USER";
        if (userUid == null) userUid = "";

        recyclerView = findViewById(R.id.recyclerViewServers);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VmAdapter(serverList, this);
        recyclerView.setAdapter(adapter);

        if (recyclerView.getItemAnimator() != null) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        swipeRefreshLayout.setOnRefreshListener(this::fetchServers);

        setupFilters();
        fetchServers();
        startWebSocket();
    }

    private void setupFilters() {
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentStatusFilter = "ALL";
            } else if (checkedId == R.id.chipRunning) {
                currentStatusFilter = "RUNNING";
            } else if (checkedId == R.id.chipStopped) {
                currentStatusFilter = "STOPPED";
            }
            fetchServers();
        });
    }

    private void fetchServers() {
        swipeRefreshLayout.setRefreshing(true);
        Call<List<ServerInstance>> call;
        
        if (!"ALL".equals(currentStatusFilter)) {
            call = ApiClient.getApi().getServersByStatus(currentStatusFilter);
        } else {
            boolean isAdmin = userRole != null && userRole.toUpperCase().contains("ADMIN");
            if (isAdmin) {
                call = ApiClient.getApi().getAllServersAdmin();
            } else {
                call = ApiClient.getApi().getMyServers();
            }
        }

        call.enqueue(new Callback<List<ServerInstance>>() {
            @Override
            public void onResponse(Call<List<ServerInstance>> call, Response<List<ServerInstance>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    serverList = response.body();
                    adapter.updateList(serverList);
                } else {
                    Toast.makeText(VmListActivity.this, "Erreur serveur", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ServerInstance>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(VmListActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startWebSocket() {
        Request request = new Request.Builder().url(Constants.WS_URL).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                webSocket.send("CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\n\n\u0000");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.startsWith("CONNECTED")) {
                    boolean isAdmin = userRole != null && userRole.toUpperCase().contains("ADMIN");
                    String destination = isAdmin ? "/topic/metrics/admin" : "/topic/metrics/user/" + userUid;
                    webSocket.send("SUBSCRIBE\nid:sub-0\ndestination:" + destination + "\n\n\u0000");
                    return;
                }

                if (text.startsWith("MESSAGE")) {
                    try {
                        String jsonPart = text.substring(text.indexOf("\n\n") + 2, text.lastIndexOf("\u0000"));
                        runOnUiThread(() -> {
                            VmMetrics metrics = new Gson().fromJson(jsonPart, VmMetrics.class);
                            if (metrics != null) {
                                adapter.updateMetrics(metrics);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                recyclerView.postDelayed(() -> startWebSocket(), 5000);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
        }
    }

    @Override
    public void onStart(ServerInstance server) {
        ApiClient.getApi().startServer(server.getProxmoxVmId()).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) fetchServers();
            }
            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {}
        });
    }

    @Override
    public void onStop(ServerInstance server) {
        ApiClient.getApi().stopServer(server.getProxmoxVmId()).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) fetchServers();
            }
            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {}
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onDelete(ServerInstance server) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Voulez-vous supprimer " + server.getName() + " ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    ApiClient.getApi().deleteServer(server.getProxmoxVmId()).enqueue(new Callback<okhttp3.ResponseBody>() {
                        @Override
                        public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                            if (response.isSuccessful()) fetchServers();
                        }
                        @Override
                        public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
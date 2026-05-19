package com.ouazzou.miniaws.ui.monitor;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.models.ServerInstance;
import com.ouazzou.miniaws.models.VmMetrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VmAdapter extends RecyclerView.Adapter<VmAdapter.VmViewHolder> {

    private List<ServerInstance> serverList;
    private OnVmActionListener listener;
    private Map<Integer, VmMetrics> metricsMap = new HashMap<>();

    public interface OnVmActionListener {
        void onStart(ServerInstance server);
        void onStop(ServerInstance server);
        void onDelete(ServerInstance server);
    }

    public VmAdapter(List<ServerInstance> serverList, OnVmActionListener listener) {
        this.serverList = serverList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_server, parent, false);
        return new VmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VmViewHolder holder, int position) {
        onBindViewHolder(holder, position, java.util.Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull VmViewHolder holder, int position, @NonNull List<Object> payloads) {
        ServerInstance server = serverList.get(position);

        if (payloads.isEmpty()) {
            // Full update
            holder.tvServerName.setText(server.getName());
            holder.tvServerDetails.setText(String.format("VM ID: %d | RAM: %d MB | CPU: %d",
                    server.getProxmoxVmId(), server.getRamAllocated(), server.getCpuAllocated()));
            holder.tvServerIp.setText(server.getIpAddress() != null ? server.getIpAddress() : "IP en cours...");

            // Status coloring
            if ("RUNNING".equals(server.getStatus())) {
                holder.tvServerName.setTextColor(Color.parseColor("#00E676")); // Green
                holder.btnStart.setVisibility(View.GONE);
                holder.btnStop.setVisibility(View.VISIBLE);
            } else if ("ERROR".equals(server.getStatus())) {
                holder.tvServerName.setTextColor(Color.RED);
                holder.btnStart.setVisibility(View.VISIBLE);
                holder.btnStop.setVisibility(View.VISIBLE);
            } else {
                holder.tvServerName.setTextColor(Color.WHITE);
                holder.btnStart.setVisibility(View.VISIBLE);
                holder.btnStop.setVisibility(View.GONE);
            }

            // Click listeners (only on full bind)
            holder.btnStart.setOnClickListener(v -> listener.onStart(server));
            holder.btnStop.setOnClickListener(v -> listener.onStop(server));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(server));

            // Ouvrir les détails au clic sur la carte
            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(v.getContext(), VmDetailActivity.class);
                intent.putExtra("SERVER_NAME", server.getName());
                intent.putExtra("SERVER_IP", server.getIpAddress());
                intent.putExtra("SERVER_VMID", server.getProxmoxVmId());
                intent.putExtra("SERVER_RAM", server.getRamAllocated());
                intent.putExtra("SERVER_CPU", server.getCpuAllocated());
                intent.putExtra("SERVER_STATUS", server.getStatus());
                v.getContext().startActivity(intent);
            });

            // Metrics display
            updateMetricsView(holder, metricsMap.get(server.getProxmoxVmId()), server.getStatus(), false);
        } else {
            // ⚡ Partial update (Fluid)
            VmMetrics metrics = (VmMetrics) payloads.get(0);
            updateMetricsView(holder, metrics, server.getStatus(), true);
        }
    }

    private void updateMetricsView(VmViewHolder holder, VmMetrics metrics, String status, boolean animate) {
        if (metrics != null && "RUNNING".equals(status)) {
            holder.metricsLayout.setVisibility(View.VISIBLE);
            int cpu = (int) metrics.getCpuUsagePercentage();
            int ram = (int) metrics.getRamUsagePercentage();
            
            if (animate) {
                animateProgress(holder.pbCpu, cpu);
                animateProgress(holder.pbRam, ram);
            } else {
                holder.pbCpu.setProgress(cpu);
                holder.pbRam.setProgress(ram);
            }
            
            holder.tvCpu.setText(String.format("%.1f%%", metrics.getCpuUsagePercentage()));
            holder.tvRam.setText(String.format("%.1f%%", metrics.getRamUsagePercentage()));
        } else {
            holder.metricsLayout.setVisibility(View.GONE);
            holder.pbCpu.setProgress(0);
            holder.pbRam.setProgress(0);
        }
    }

    private void animateProgress(ProgressBar pb, int target) {
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), target);
        animation.setDuration(800); // 800ms pour une transition très douce
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    @Override
    public int getItemCount() {
        return serverList.size();
    }

    public void updateList(List<ServerInstance> newList) {
        this.serverList = newList;
        // On nettoie la map des metrics pour les serveurs qui ne sont plus RUNNING
        for (ServerInstance s : newList) {
            if (!"RUNNING".equals(s.getStatus())) {
                metricsMap.remove(s.getProxmoxVmId());
            }
        }
        notifyDataSetChanged();
    }

    public void updateMetrics(VmMetrics metrics) {
        if ("RUNNING".equals(metrics.getStatus())) {
            metricsMap.put(metrics.getVmId(), metrics);
        } else {
            metricsMap.remove(metrics.getVmId());
        }

        for (int i = 0; i < serverList.size(); i++) {
            if (serverList.get(i).getProxmoxVmId().equals(metrics.getVmId())) {
                // Use payload to avoid flicker
                notifyItemChanged(i, metrics);
                break;
            }
        }
    }

    static class VmViewHolder extends RecyclerView.ViewHolder {
        TextView tvServerName, tvServerDetails, tvServerIp, tvCpu, tvRam;
        ProgressBar pbCpu, pbRam;
        ImageButton btnStart, btnStop, btnDelete;
        View metricsLayout;

        public VmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServerName = itemView.findViewById(R.id.tvServerName);
            tvServerDetails = itemView.findViewById(R.id.tvServerDetails);
            tvServerIp = itemView.findViewById(R.id.tvServerIp);
            tvCpu = itemView.findViewById(R.id.tvCpu);
            tvRam = itemView.findViewById(R.id.tvRam);
            pbCpu = itemView.findViewById(R.id.pbCpu);
            pbRam = itemView.findViewById(R.id.pbRam);
            btnStart = itemView.findViewById(R.id.btnStart);
            btnStop = itemView.findViewById(R.id.btnStop);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            metricsLayout = itemView.findViewById(R.id.metricsLayout);
        }
    }
}
package com.ouazzou.miniaws.api;

import com.ouazzou.miniaws.models.AppUser;
import com.ouazzou.miniaws.models.ServerInstance;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MiniAwsApi {

    // 1. IAM & Profil
    @GET("/api/iam/me")
    Call<AppUser> getMyProfile();

    // 2. Compute - Liste & Admin
    @GET("/api/compute/servers")
    Call<List<ServerInstance>> getMyServers();

    @GET("/api/compute/servers/admin/all")
    Call<List<ServerInstance>> getAllServersAdmin();

    @GET("/api/compute/servers/status/{status}")
    Call<List<ServerInstance>> getServersByStatus(@Path("status") String status);

    // 3. Compute - Contrôles
    @POST("/api/compute/servers/{proxmoxVmId}/start")
    Call<okhttp3.ResponseBody> startServer(@Path("proxmoxVmId") Integer proxmoxVmId);

    @POST("/api/compute/servers/{proxmoxVmId}/stop")
    Call<okhttp3.ResponseBody> stopServer(@Path("proxmoxVmId") Integer proxmoxVmId);

    @DELETE("/api/compute/servers/{proxmoxVmId}")
    Call<okhttp3.ResponseBody> deleteServer(@Path("proxmoxVmId") Integer proxmoxVmId);

    // 4. AI Ops
    @POST("/api/ai/magic-deploy")
    Call<ServerInstance> magicDeploy(@Query("message") String message);

    @GET("/api/ai/chat")
    Call<okhttp3.ResponseBody> chat(@Query("message") String message);
}
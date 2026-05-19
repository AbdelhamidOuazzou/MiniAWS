package com.ouazzou.miniaws.modules.compute.service;

import com.ouazzou.miniaws.modules.compute.domain.ServerInstance;
import com.ouazzou.miniaws.modules.compute.domain.ServerStatus;
import com.ouazzou.miniaws.modules.ai_ops.domain.VmDeploymentRequest;
import java.util.List;

public interface IComputeService {

    ServerInstance createServer(VmDeploymentRequest request);

    void waitForIpAndSaveAsync(Integer proxmoxVmId);

    void startServer(Integer proxmoxVmId);

    void stopServer(Integer proxmoxVmId);

    String getServerIp(Integer proxmoxVmId);

    List<ServerInstance> getAllServers();

    List<ServerInstance> getServersByStatus(ServerStatus status);

    List<ServerInstance> getServersByOwner(String ownerId);

    void deleteServer(Integer proxmoxVmId);

    List<ServerInstance> getServersByOwnerAndStatus(String ownerId, ServerStatus status);
}
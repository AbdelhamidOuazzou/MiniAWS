package com.ouazzou.miniaws.modules.compute.infrastructure;

import com.ouazzou.miniaws.modules.monitoring.domain.VmMetrics;

import java.util.Optional;

public interface IProxmoxApiClient {
    Optional<Integer> cloneVm(String newVmName, int templateId);
    boolean configureVm(Integer vmId, Integer ram, Integer cpu);
    boolean startVm(Integer vmId);
    boolean stopVm(Integer vmId);
    String getVmIp(Integer vmId);
    VmMetrics getVmStats(int vmId);
    boolean deleteVm(Integer vmId);
}
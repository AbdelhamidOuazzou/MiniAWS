package com.ouazzou.miniaws.models;

public class VmMetrics {
    private Integer vmId;
    private double cpuUsagePercentage;
    private double ramUsagePercentage;
    private String status;

    public Integer getVmId() { return vmId; }
    public double getCpuUsagePercentage() { return cpuUsagePercentage; }
    public double getRamUsagePercentage() { return ramUsagePercentage; }
    public String getStatus() { return status; }
}
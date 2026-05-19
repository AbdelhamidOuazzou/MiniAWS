package com.ouazzou.miniaws.models;

public class ServerInstance {
    private Long id;
    private String name;
    private Integer proxmoxVmId;
    private Integer ramAllocated;
    private Integer cpuAllocated;
    private String status; // RUNNING, STOPPED, ERROR
    private String ownerId;
    private String ipAddress;
    private String createdAt;
    private String updatedAt;

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getProxmoxVmId() { return proxmoxVmId; }
    public Integer getRamAllocated() { return ramAllocated; }
    public Integer getCpuAllocated() { return cpuAllocated; }
    public String getStatus() { return status; }
    public String getOwnerId() { return ownerId; }
    public String getIpAddress() { return ipAddress; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
package com.ouazzou.miniaws.models;

public class VmDeploymentRequest {
    private String command;
    private double latitude;
    private double longitude;

    public VmDeploymentRequest(String command, double latitude, double longitude) {
        this.command = command;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getCommand() { return command; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
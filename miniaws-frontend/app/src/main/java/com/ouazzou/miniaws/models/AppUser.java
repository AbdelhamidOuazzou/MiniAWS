package com.ouazzou.miniaws.models;

public class AppUser {
    private String firebaseUid;
    private String email;
    private String role;
    private int maxServersAllowed;
    private int maxCpuAllowed;
    private int maxRamAllowed;

    public String getFirebaseUid() { return firebaseUid; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public int getMaxServersAllowed() { return maxServersAllowed; }
    public int getMaxCpuAllowed() { return maxCpuAllowed; }
    public int getMaxRamAllowed() { return maxRamAllowed; }
}
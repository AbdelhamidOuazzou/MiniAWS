package com.ouazzou.miniaws.modules.iam.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    private String firebaseUid;

    private String email;

    private String role;

    private int maxServersAllowed;

    @Builder.Default
    private int maxCpuAllowed = 4;

    @Builder.Default
    private int maxRamAllowed = 4096;

   // private String avatarUrl;
}
package com.ouazzou.miniaws.modules.compute.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "server_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ServerInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private Integer proxmoxVmId;

    @Column(nullable = false)
    private Integer ramAllocated;

    @Column(nullable = false)
    private Integer cpuAllocated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerStatus status;

    @Column(nullable = false)
    private String ownerId;

    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
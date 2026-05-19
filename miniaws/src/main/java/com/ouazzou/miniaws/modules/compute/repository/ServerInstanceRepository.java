package com.ouazzou.miniaws.modules.compute.repository;

import com.ouazzou.miniaws.modules.compute.domain.ServerInstance;
import com.ouazzou.miniaws.modules.compute.domain.ServerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerInstanceRepository extends JpaRepository<ServerInstance, Long> {

    Optional<ServerInstance> findByProxmoxVmId(Integer proxmoxVmId);
    List<ServerInstance> findByStatus(ServerStatus status);
    boolean existsByName(String name);

    // 👤 1. Les sommes PERSONNELLES (pour l'utilisateur)
    @Query("SELECT SUM(s.cpuAllocated) FROM ServerInstance s WHERE s.ownerId = :ownerId")
    Integer sumCpuAllocatedByOwnerId(@Param("ownerId") String ownerId);

    @Query("SELECT SUM(s.ramAllocated) FROM ServerInstance s WHERE s.ownerId = :ownerId")
    Integer sumRamAllocatedByOwnerId(@Param("ownerId") String ownerId);

    // 🌍 2. Les sommes GLOBALES (pour tout le serveur MiniAWS)
    @Query("SELECT SUM(s.cpuAllocated) FROM ServerInstance s")
    Integer sumCpuAllocatedGlobal();

    @Query("SELECT SUM(s.ramAllocated) FROM ServerInstance s")
    Integer sumRamAllocatedGlobal();

    List<ServerInstance> findByOwnerId(String ownerId);

    List<ServerInstance> findByOwnerIdAndStatus(String ownerId, ServerStatus status);
}
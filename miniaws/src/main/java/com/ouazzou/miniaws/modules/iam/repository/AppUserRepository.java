package com.ouazzou.miniaws.modules.iam.repository;

import com.ouazzou.miniaws.modules.iam.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {
}
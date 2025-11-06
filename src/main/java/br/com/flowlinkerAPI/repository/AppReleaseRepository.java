package br.com.flowlinkerAPI.repository;

import br.com.flowlinkerAPI.model.AppRelease;
import br.com.flowlinkerAPI.model.AppRelease.Arch;
import br.com.flowlinkerAPI.model.AppRelease.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppReleaseRepository extends JpaRepository<AppRelease, Long> {
    Optional<AppRelease> findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(Platform platform, Arch arch, Boolean isActive);
}



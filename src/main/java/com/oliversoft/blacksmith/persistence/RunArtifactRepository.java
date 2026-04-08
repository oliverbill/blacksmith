package com.oliversoft.blacksmith.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;

public interface RunArtifactRepository extends JpaRepository<RunArtifact, Long> {

    Optional<RunArtifact> findByRunAndArtifactType(TenantRun run, ArtifactType type);
    Optional<RunArtifact> findTopByRunTenantIdAndAgentNameOrderByCreatedAtDesc(Long tenantId,AgentName agentName);
}

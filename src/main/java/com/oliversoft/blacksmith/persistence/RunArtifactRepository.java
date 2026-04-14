package com.oliversoft.blacksmith.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;

public interface RunArtifactRepository extends JpaRepository<RunArtifact, Long> {

    List<RunArtifact> findByRun(TenantRun run);

    Optional<RunArtifact> findTopByRunAndArtifactTypeOrderByCreatedAtDesc(TenantRun run, ArtifactType artifactType);

    Optional<RunArtifact> findTopByRunTenantIdAndAgentNameOrderByCreatedAtDesc(Long tenantId, AgentName agentName);
    Optional<RunArtifact> findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(Long tenantId, ArtifactType artifactType);
}

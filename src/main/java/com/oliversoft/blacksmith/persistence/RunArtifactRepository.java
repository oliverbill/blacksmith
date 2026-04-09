package com.oliversoft.blacksmith.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;

public interface RunArtifactRepository extends JpaRepository<RunArtifact, Long> {

    Optional<RunArtifact> findTopByRunTenantIdAndAgentNameOrderByCreatedAtDesc(Long tenantId,AgentName agentName);
    Optional<RunArtifact> findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(Long tenantId,ArtifactType artifactType);
}

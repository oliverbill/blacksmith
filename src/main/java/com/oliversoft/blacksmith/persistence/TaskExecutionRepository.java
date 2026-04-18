package com.oliversoft.blacksmith.persistence;

import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {

    Optional<TaskExecution> findFirstByArtifactAndStatus(RunArtifact artifact, TaskStatus status);

    List<TaskExecution> findByArtifactIn(Collection<RunArtifact> artifacts);

    List<TaskExecution> findByArtifactRun(TenantRun run);
    
    List<TaskExecution> findByStatus(TaskStatus status);

    long countByArtifactAndStatus(RunArtifact artifact, TaskStatus status);
}

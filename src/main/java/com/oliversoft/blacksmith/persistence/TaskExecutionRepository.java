package com.oliversoft.blacksmith.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.TaskStatus;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {

    Optional<TaskExecution> findFirstByArtifactAndStatus(RunArtifact artifact, TaskStatus status);

    List<TaskExecution> findByArtifactRun(TenantRun run);
    
    List<TaskExecution> findByStatus(TaskStatus status);
    
    long countByStatus(TaskStatus status);
    
    List<TaskExecution> findByArtifactAndStatus(RunArtifact artifact, TaskStatus status);
    
    long countByArtifactAndStatus(RunArtifact artifact, TaskStatus status);
}

package com.oliversoft.blacksmith.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.enumeration.TaskStatus;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long>{
    
    Optional<TaskExecution> findFirstByArtifactAndStatus(RunArtifact artifact, TaskStatus status);
}

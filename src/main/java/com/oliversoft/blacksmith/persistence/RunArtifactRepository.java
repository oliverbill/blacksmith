package com.oliversoft.blacksmith.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.oliversoft.blacksmith.model.entity.RunArtifact;

public interface RunArtifactRepository extends JpaRepository<RunArtifact, Long> {

}

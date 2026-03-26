package com.oliversoft.blacksmith.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.RunTask;

public interface RunTaskRepository extends JpaRepository<RunTask, Long>{
    
}

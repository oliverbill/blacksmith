package com.oliversoft.blacksmith.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.TenantRun;

public interface TenantRunRepository extends JpaRepository<TenantRun, Long> {

}

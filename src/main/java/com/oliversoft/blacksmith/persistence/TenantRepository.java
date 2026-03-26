package com.oliversoft.blacksmith.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

}
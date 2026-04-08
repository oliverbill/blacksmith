package com.oliversoft.blacksmith.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    public List<Tenant> findByNameContainingIgnoreCase(String name);
}
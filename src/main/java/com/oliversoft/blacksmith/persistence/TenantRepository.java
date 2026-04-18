package com.oliversoft.blacksmith.persistence;

import com.oliversoft.blacksmith.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    public List<Tenant> findByNameContainingIgnoreCase(String name);
}
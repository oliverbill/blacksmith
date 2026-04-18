package com.oliversoft.blacksmith.persistence;

import com.oliversoft.blacksmith.model.entity.TenantRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRunRepository extends JpaRepository<TenantRun, Long> {

}

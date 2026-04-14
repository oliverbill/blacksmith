package com.oliversoft.blacksmith.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oliversoft.blacksmith.model.entity.RefinementRequest;

public interface RefinementRequestRepository extends JpaRepository<RefinementRequest,Long>{

}

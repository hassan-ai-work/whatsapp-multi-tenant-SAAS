package com.levosoft.microservice.brain.business.repository;

import com.levosoft.microservice.brain.business.model.TenantBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantBusinessRepository extends JpaRepository<TenantBusiness, Long> {

    boolean existsByRegisteredNumber(String registeredNumber);

    Optional<TenantBusiness> findByIdAndTenantId(Long id, Long tenantId);

    List<TenantBusiness> findAllByTenantId(Long tenantId);
}

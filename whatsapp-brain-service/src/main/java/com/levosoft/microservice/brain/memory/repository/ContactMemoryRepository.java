package com.levosoft.microservice.brain.memory.repository;

import com.levosoft.microservice.brain.memory.model.ContactMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactMemoryRepository extends JpaRepository<ContactMemory, Long> {

    Optional<ContactMemory> findByIdAndTenantIdAndBusinessId(Long id, Long tenantId, Long businessId);

    Optional<ContactMemory> findByTenantIdAndBusinessIdAndIdempotencyHash(Long tenantId, Long businessId, String idempotencyHash);

    List<ContactMemory> findAllByTenantIdAndBusinessIdOrderByCreatedAtDesc(Long tenantId, Long businessId);
}

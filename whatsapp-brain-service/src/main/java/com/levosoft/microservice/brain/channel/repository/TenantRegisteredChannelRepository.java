package com.levosoft.microservice.brain.channel.repository;

import com.levosoft.microservice.brain.channel.model.TenantRegisteredChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRegisteredChannelRepository extends JpaRepository<TenantRegisteredChannel, Long> {

    List<TenantRegisteredChannel> findAllByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<TenantRegisteredChannel> findFirstByBusinessIdAndChannelCodeAndLinkedStatusOrderByCreatedAtDesc(
            Long businessId,
            String channelCode,
            Boolean linkedStatus
    );

    Optional<TenantRegisteredChannel> findByIdAndBusinessId(Long id, Long businessId);
}

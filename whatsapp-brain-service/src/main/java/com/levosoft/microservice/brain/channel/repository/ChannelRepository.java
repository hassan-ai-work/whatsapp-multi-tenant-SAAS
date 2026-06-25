package com.levosoft.microservice.brain.channel.repository;

import com.levosoft.microservice.brain.channel.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    Optional<Channel> findByCode(String code);
}

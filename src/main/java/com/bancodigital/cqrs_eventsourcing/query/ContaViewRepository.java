package com.bancodigital.cqrs_eventsourcing.query;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ContaViewRepository extends JpaRepository<ContaView, UUID> {
}
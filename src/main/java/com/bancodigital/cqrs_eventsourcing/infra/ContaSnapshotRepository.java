package com.bancodigital.cqrs_eventsourcing.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ContaSnapshotRepository extends JpaRepository<ContaSnapshotEntity, UUID> {
}
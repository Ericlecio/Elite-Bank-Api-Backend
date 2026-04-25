package com.bancodigital.cqrs_eventsourcing.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventStoreRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByAggregateId(UUID aggregateId);
    List<EventEntity> findByAggregateIdOrderByIdAsc(UUID aggregateId);
}
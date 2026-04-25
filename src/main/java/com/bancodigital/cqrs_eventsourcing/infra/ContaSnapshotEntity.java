package com.bancodigital.cqrs_eventsourcing.infra;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conta_snapshots")
public class ContaSnapshotEntity {

    @Id
    private UUID aggregateId;
    private String payload; // O JSON da classe Conta
    private Long versao;
    private LocalDateTime createdAt;

    public ContaSnapshotEntity() {}

    public ContaSnapshotEntity(UUID aggregateId, String payload, Long versao) {
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.versao = versao;
        this.createdAt = LocalDateTime.now();
    }

    // Getters e Setters
    public UUID getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Long getVersao() { return versao; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
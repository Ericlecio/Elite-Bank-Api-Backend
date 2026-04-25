package com.bancodigital.cqrs_eventsourcing.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "conta_view")
public class ContaView {

    @Id
    private UUID id;
    private String titular;
    private BigDecimal saldo;

    private Long versao = 0L;

    public ContaView() {}

    public ContaView(UUID id, String titular, BigDecimal saldo) {
        this.id = id;
        this.titular = titular;
        this.saldo = saldo;
    }

    public UUID getId() { return id; }
    public String getTitular() { return titular; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }
}
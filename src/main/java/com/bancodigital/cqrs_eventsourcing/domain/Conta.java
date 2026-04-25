package com.bancodigital.cqrs_eventsourcing.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Conta {
    private UUID id;
    private BigDecimal saldo;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    private Long versao = 0L;

    public Conta() {}

    public Conta(UUID id, String titular) {
        applyChange(new ContaCriadaEvent(id, titular));
    }

    public static Conta reconstruir(List<DomainEvent> historico) {
        Conta conta = new Conta();
        for (DomainEvent evento : historico) {
            conta.apply(evento);
        }
        return conta;
    }

    public void depositar(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do depósito deve ser maior que zero.");
        }
        applyChange(new DinheiroDepositadoEvent(this.id, valor));
    }

    public void sacar(BigDecimal valor) {
        if (this.saldo.compareTo(valor) < 0) {
            throw new RuntimeException("Saldo insuficiente! Saldo disponível: R$ " + this.saldo);
        }
        applyChange(new DinheiroSacadoEvent(this.id, valor));
    }

    private void applyChange(DomainEvent event) {
        apply(event);
        uncommittedEvents.add(event);
    }

    public void apply(DomainEvent event) {
        if (event instanceof ContaCriadaEvent e) {
            this.id = e.contaId();
            this.saldo = BigDecimal.ZERO;
        } else if (event instanceof DinheiroDepositadoEvent e) {
            this.saldo = this.saldo.add(e.valor());
        } else if (event instanceof DinheiroSacadoEvent e) {
            this.saldo = this.saldo.subtract(e.valor());
        }
        this.versao++;
    }

    public UUID getId() { return id; }
    public BigDecimal getSaldo() { return saldo; }
    public List<DomainEvent> getUncommittedEvents() { return uncommittedEvents; }
    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
}
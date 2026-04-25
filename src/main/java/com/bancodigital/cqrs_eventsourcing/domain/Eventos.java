package com.bancodigital.cqrs_eventsourcing.domain;

import java.math.BigDecimal;
import java.util.UUID;

// Remova o "public" de todos eles
record ContaCriadaEvent(UUID contaId, String titular) implements DomainEvent {}
record DinheiroDepositadoEvent(UUID contaId, BigDecimal valor) implements DomainEvent {}
record SaqueRealizadoEvent(UUID contaId, BigDecimal valor) implements DomainEvent {}
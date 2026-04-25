package com.bancodigital.cqrs_eventsourcing.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record DinheiroSacadoEvent(UUID contaId, BigDecimal valor) implements DomainEvent {}

package com.bancodigital.cqrs_eventsourcing.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaDTO(UUID origemId, UUID destinoId, BigDecimal valor) {}
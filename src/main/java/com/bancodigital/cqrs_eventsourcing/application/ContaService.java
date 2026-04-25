package com.bancodigital.cqrs_eventsourcing.application;

import com.bancodigital.cqrs_eventsourcing.domain.Conta;
import com.bancodigital.cqrs_eventsourcing.domain.DomainEvent;
import com.bancodigital.cqrs_eventsourcing.infra.ContaSnapshotEntity;
import com.bancodigital.cqrs_eventsourcing.infra.ContaSnapshotRepository;
import com.bancodigital.cqrs_eventsourcing.infra.EventEntity;
import com.bancodigital.cqrs_eventsourcing.infra.EventStoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
public class ContaService {
    private final EventStoreRepository repository;
    private final ContaSnapshotRepository snapshotRepository;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ContaService(EventStoreRepository repository,
                        ContaSnapshotRepository snapshotRepository,
                        ObjectMapper mapper,
                        KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.snapshotRepository = snapshotRepository;
        this.mapper = mapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public UUID criarConta(String titular) {
        Conta conta = new Conta(UUID.randomUUID(), titular);
        salvarEventos(conta);
        return conta.getId();
    }

    @Transactional
    public void depositar(UUID id, BigDecimal valor) {
        Conta conta = reconstruirConta(id);
        conta.depositar(valor);
        salvarEventos(conta);
    }

    @Transactional
    public void sacar(UUID id, BigDecimal valor) {
        Conta conta = reconstruirConta(id);
        conta.sacar(valor);
        salvarEventos(conta);
    }

    @Transactional
    public void transferir(TransferenciaDTO dto) {
        sacar(dto.origemId(), dto.valor());
        depositar(dto.destinoId(), dto.valor());
    }

    private Conta reconstruirConta(UUID id) {
        Optional<ContaSnapshotEntity> snapshotOpt = snapshotRepository.findById(id);
        Conta conta;
        long ultimaVersao = -1L;

        if (snapshotOpt.isPresent()) {
            try {
                conta = mapper.readValue(snapshotOpt.get().getPayload(), Conta.class);
                ultimaVersao = snapshotOpt.get().getVersao();
            } catch (Exception e) {
                throw new RuntimeException("Erro ao carregar snapshot", e);
            }
        } else {
            conta = new Conta();
        }

        final long versaoFiltro = ultimaVersao;

        // 2. Busca na Event Store APENAS os eventos que aconteceram DEPOIS do snapshot
        List<EventEntity> entities = repository.findByAggregateIdOrderByIdAsc(id).stream()
                .filter(e -> e.getId() > versaoFiltro)
                .toList();

        if (entities.isEmpty() && snapshotOpt.isEmpty()) {
            throw new RuntimeException("Conta não encontrada!");
        }

        List<DomainEvent> eventosRestantes = entities.stream().map(entity -> {
            try {
                String pacote = "com.bancodigital.cqrs_eventsourcing.domain.";
                Class<?> classeDoEvento = Class.forName(pacote + entity.getEventType());
                return (DomainEvent) mapper.readValue(entity.getPayload(), classeDoEvento);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao desserializar evento", e);
            }
        }).toList();

        for (DomainEvent ev : eventosRestantes) {
            conta.apply(ev);
        }

        return conta;
    }

    private void salvarEventos(Conta conta) {
        conta.getUncommittedEvents().forEach(event -> {
            saveAndSend(conta.getId(), event.getClass().getSimpleName(), event);
        });
        conta.getUncommittedEvents().clear();

        if (conta.getVersao() > 0 && conta.getVersao() % 10 == 0) {
            try {
                String stateJson = mapper.writeValueAsString(conta);
                snapshotRepository.save(new ContaSnapshotEntity(conta.getId(), stateJson, conta.getVersao()));
                System.out.println("📸 SNAPSHOT: Foto da conta " + conta.getId() + " guardada na versão " + conta.getVersao());
            } catch (Exception e) {
                System.err.println("Erro ao salvar snapshot: " + e.getMessage());
            }
        }
    }

    private void saveAndSend(UUID aggregateId, String eventType, Object event) {
        try {
            EventEntity entity = new EventEntity();
            entity.setAggregateId(aggregateId);
            entity.setEventType(eventType);
            entity.setPayload(mapper.writeValueAsString(event));
            entity.setCreatedAt(LocalDateTime.now());

            EventEntity salva = repository.save(entity);

            Map<String, Object> envelope = new HashMap<>();
            envelope.put("tipo", eventType);
            envelope.put("idEvento", salva.getId());
            envelope.put("dados", event);

            String payloadCompleto = mapper.writeValueAsString(envelope);
            kafkaTemplate.send("conta-eventos", aggregateId.toString(), payloadCompleto);

            System.out.println("SISTEMA: Evento [" + eventType + "] processado (ID: " + salva.getId() + ")");

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao processar JSON", e);
        }
    }
}
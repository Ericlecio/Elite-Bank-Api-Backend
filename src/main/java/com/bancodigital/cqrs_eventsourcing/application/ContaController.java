package com.bancodigital.cqrs_eventsourcing.application;

import com.bancodigital.cqrs_eventsourcing.infra.EventStoreRepository;
import com.bancodigital.cqrs_eventsourcing.query.ContaView;
import com.bancodigital.cqrs_eventsourcing.query.ContaViewRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/contas")
@Tag(name = "Contas Bancárias", description = "Operações de movimentação, saldo e extrato histórico")
public class ContaController {

    private final ContaService service;
    private final ContaViewRepository queryRepository;
    private final EventStoreRepository eventRepository;

    public ContaController(ContaService service,
                           ContaViewRepository queryRepository,
                           EventStoreRepository eventRepository) {
        this.service = service;
        this.queryRepository = queryRepository;
        this.eventRepository = eventRepository;
    }

    @PostMapping
    @Operation(summary = "Cria uma nova conta", description = "Gera um UUID e dispara o evento de criação no sistema")
    public UUID criarConta(@RequestBody String titular) {
        return service.criarConta(titular);
    }

    @PostMapping("/{id}/depositar")
    @Operation(summary = "Realiza um depósito", description = "Adiciona valores à conta e sincroniza com o banco de leitura via Kafka")
    public String depositar(@PathVariable UUID id, @RequestBody BigDecimal valor) {
        service.depositar(id, valor);
        return "Depósito de R$ " + valor + " processado e enviado ao Kafka!";
    }

    @GetMapping("/{id}/saldo")
    @Operation(summary = "Consulta o saldo atual", description = "Busca o saldo consolidado diretamente na tabela de consulta (Read Model)")
    public ContaView consultarSaldo(@PathVariable UUID id) {
        return queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta ainda não sincronizada ou inexistente"));
    }

    @GetMapping("/{id}/extrato")
    @Operation(summary = "Ver extrato completo", description = "Retorna a linha do tempo imutável de todos os eventos da conta (Event Store)")
    public List<Map<String, Object>> verExtratoCompleto(@PathVariable UUID id) {
        return eventRepository.findByAggregateIdOrderByIdAsc(id).stream().map(evento -> {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("data_hora", evento.getCreatedAt());
            linha.put("evento", evento.getEventType());
            linha.put("detalhes", evento.getPayload());
            return linha;
        }).toList();
    }

    @PostMapping("/{id}/sacar")
    @Operation(summary = "Realiza um saque", description = "Valida o saldo através do histórico de eventos antes de autorizar o débito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saque realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Saldo insuficiente para a operação")
    })
    public String sacar(@PathVariable UUID id, @RequestBody BigDecimal valor) {
        service.sacar(id, valor);
        return "Saque de R$ " + valor + " realizado com sucesso!";
    }

    @PostMapping("/transferir")
    @Operation(summary = "Transferência entre contas", description = "Executa um saque na origem e um depósito no destino de forma atômica")
    public String transferir(@RequestBody TransferenciaDTO dto) {
        service.transferir(dto);
        return "Transferência de R$ " + dto.valor() + " realizada com sucesso!";
    }
}
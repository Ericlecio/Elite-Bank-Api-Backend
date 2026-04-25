package com.bancodigital.cqrs_eventsourcing.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ContaEventListener {

    private final ContaViewRepository repository;
    private final ObjectMapper mapper;

    public ContaEventListener(ContaViewRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    @KafkaListener(topics = "conta-eventos", groupId = "banco-digital-group")
    public void consumir(String message) {
        try {
            JsonNode envelope = mapper.readTree(message);
            String tipo = envelope.get("tipo").asText();
            JsonNode dados = envelope.get("dados");

            long idEvento = envelope.has("idEvento") ? envelope.get("idEvento").asLong() : 0L;

            UUID aggregateId = UUID.fromString(dados.get("contaId").asText());

            if (tipo.equals("ContaCriadaEvent")) {
                if (repository.existsById(aggregateId)) return;

                String titular = dados.get("titular").asText();

                ContaView novaConta = new ContaView(aggregateId, titular, BigDecimal.ZERO);
                novaConta.setVersao(idEvento);
                repository.save(novaConta);

                System.out.println(" Idempotência: Conta registrada com sucesso.");
            }
            else if (dados.has("valor")) {
                BigDecimal valor = new BigDecimal(dados.get("valor").asText());
                ContaView conta = repository.findById(aggregateId)
                        .orElseThrow(() -> new RuntimeException("Conta não encontrada!"));

                if (idEvento > 0 && idEvento <= conta.getVersao()) {
                    System.out.println(" Evento " + idEvento + " ignorado (já processado anteriormente).");
                    return;
                }

                if (tipo.equals("DinheiroSacadoEvent")) {
                    conta.setSaldo(conta.getSaldo().subtract(valor));
                    System.out.println(" SAQUE: - R$ " + valor);
                } else {
                    conta.setSaldo(conta.getSaldo().add(valor));
                    System.out.println(" DEPÓSITO: + R$ " + valor);
                }

                conta.setVersao(idEvento);
                repository.save(conta);
                System.out.println(" Saldo consolidado (v" + idEvento + "): R$ " + conta.getSaldo());
            }
        } catch (Exception e) {
            System.err.println(" Erro ao processar mensagem: " + e.getMessage());
        }
    }
}
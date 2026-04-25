package com.bancodigital.cqrs_eventsourcing.application;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TesteRapido implements CommandLineRunner {

    private final ContaService service;

    public TesteRapido(ContaService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(" INICIANDO TESTE AUTOMÁTICO DE FLUXO COMPLETO...");

        UUID idOrigem = service.criarConta("Ericlecio (Conta Corrente)");
        UUID idDestino = service.criarConta("Conta Investimento (Reserva)");

        System.out.println(" Conta Origem: " + idOrigem);
        System.out.println(" Conta Destino: " + idDestino);

        service.depositar(idOrigem, new BigDecimal("5000.00"));
        System.out.println(" Depósito de R$ 5000 processado na Origem.");

        service.sacar(idOrigem, new BigDecimal("1000.00"));
        System.out.println(" Saque de R$ 1000 processado na Origem.");

        TransferenciaDTO dto = new TransferenciaDTO(idOrigem, idDestino, new BigDecimal("1500.00"));
        service.transferir(dto);
        System.out.println(" Transferência de R$ 1500 processada da Origem para o Destino.");

        System.out.println(" Iniciando teste de stress para forçar o Snapshot...");
        for (int i = 1; i <= 7; i++) {
            service.depositar(idOrigem, new BigDecimal("10.00"));
        }

        Thread.sleep(2000);

        System.out.println("=====================================================");
        System.out.println(" TESTE CONCLUÍDO COM SUCESSO!");
        System.out.println("Role o console para cima e procure a mensagem de SNAPSHOT!");
        System.out.println("=====================================================");
    }
}
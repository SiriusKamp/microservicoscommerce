package com.cloudcommerce.pedido.service;

import com.cloudcommerce.pedido.messaging.PedidoAuditoriaMessage;
import com.cloudcommerce.pedido.messaging.PedidoAuditoriaProducer;
import com.cloudcommerce.pedido.model.Pedido;
import com.cloudcommerce.pedido.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PedidoSemRespostaAuditoriaService {

    private static final String STATUS_PROCESSANDO = "PROCESSANDO";

    private final PedidoRepository pedidoRepository;
    private final PedidoAuditoriaProducer pedidoAuditoriaProducer;
    private final Set<Long> pedidosAuditados = ConcurrentHashMap.newKeySet();

    @Value("${commerce.auditoria.pedidos-sem-resposta.limite-segundos}")
    private long limiteSegundos;

    public PedidoSemRespostaAuditoriaService(
            PedidoRepository pedidoRepository,
            PedidoAuditoriaProducer pedidoAuditoriaProducer
    ) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoAuditoriaProducer = pedidoAuditoriaProducer;
    }

    @Scheduled(
            fixedDelayString = "${commerce.auditoria.pedidos-sem-resposta.fixed-delay-ms}",
            initialDelayString = "${commerce.auditoria.pedidos-sem-resposta.initial-delay-ms}"
    )
    public void auditarPedidosSemRespostaDoEstoque() {
        LocalDateTime limite = LocalDateTime
                .now()
                .minusSeconds(limiteSegundos);

        pedidoRepository
                .findByStatusAndCriadoEmBefore(STATUS_PROCESSANDO, limite)
                .stream()
                .filter(pedido -> pedidosAuditados.add(pedido.getId())) // Filtra usando true or false retornado pelo SET caso o pedido já esteja no SET.
                .map(this::criarMensagem)
                .forEach(pedidoAuditoriaProducer::enviar);
    }

    private PedidoAuditoriaMessage criarMensagem(Pedido pedido) {
        return new PedidoAuditoriaMessage(
                pedido.getId(),
                pedido.getStatus(),
                "Pedido criado, mas ainda sem resposta do estoque.",
                pedido.getCriadoEm(),
                LocalDateTime.now()
        );
    }
}

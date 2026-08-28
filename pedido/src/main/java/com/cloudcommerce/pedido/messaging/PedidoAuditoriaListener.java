package com.cloudcommerce.pedido.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoAuditoriaListener {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoAuditoriaListener.class);

    @RabbitListener(queues = "${commerce.rabbitmq.queue.auditoria-pedido}")
    public void receberAuditoria(PedidoAuditoriaMessage mensagem) {
        log.warn(
                "AUDITORIA: pedido {} continua com status {}. Motivo: {}",
                mensagem.pedidoId(),
                mensagem.statusAtual(),
                mensagem.motivo()
        );
    }
}

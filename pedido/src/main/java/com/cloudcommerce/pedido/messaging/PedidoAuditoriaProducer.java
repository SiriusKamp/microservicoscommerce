package com.cloudcommerce.pedido.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PedidoAuditoriaProducer {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoAuditoriaProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public PedidoAuditoriaProducer(
            RabbitTemplate rabbitTemplate,
            @Value("${commerce.rabbitmq.topic-exchange.auditoria}")
            String exchange,
            @Value("${commerce.rabbitmq.routing-key.pedido-sem-resposta-estoque}")
            String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void enviar(PedidoAuditoriaMessage mensagem) {
        log.warn(
                "Publicando auditoria do pedido {} sem resposta do estoque.",
                mensagem.pedidoId()
        );

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                mensagem
        );
    }
}

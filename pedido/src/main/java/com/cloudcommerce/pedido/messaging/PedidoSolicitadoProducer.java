package com.cloudcommerce.pedido.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PedidoSolicitadoProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public PedidoSolicitadoProducer(
            RabbitTemplate rabbitTemplate,
            @Value("${commerce.rabbitmq.exchange}")
            String exchange,
            @Value("${commerce.rabbitmq.routing-key.pedido-solicitado}")
            String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    // Publica o pedido solicitado na exchange. A routing key define em qual fila ele caira.
    public void enviar(PedidoSolicitadoMessage mensagem) {
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                mensagem
        );
    }
}

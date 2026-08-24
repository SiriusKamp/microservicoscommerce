package com.cloudcommerce.estoque.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EstoqueRespostaProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public EstoqueRespostaProducer(
            RabbitTemplate rabbitTemplate,
            @Value("${commerce.rabbitmq.exchange}")
            String exchange,
            @Value("${commerce.rabbitmq.routing-key.estoque-resposta}")
            String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    // Publica a resposta na exchange. A routing key define em qual fila ela cairá.
    public void enviarResposta(EstoqueRespostaMessage mensagem) {
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                mensagem
        );
    }
}

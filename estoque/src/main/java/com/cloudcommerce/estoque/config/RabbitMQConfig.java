package com.cloudcommerce.estoque.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


// RabbitMQConfig é uma classe de configuração do Spring.
// Ela lê valores do application.properties usando @Value, como nomes da exchange, filas e routing keys.
// Os métodos marcados com @Bean criam objetos Java gerenciados pelo Spring, como DirectExchange, Queue, Binding e MessageConverter.
// Esses objetos ficam no container de beans do Spring.
// Como o projeto tem a dependência spring-boot-starter-amqp, o Spring AMQP reconhece esses beans de RabbitMQ e declara essa topologia no broker RabbitMQ real: cria exchange, cria filas e cria bindings.
// A DirectExchange é o ponto central para onde as mensagens são publicadas.
// A Queue é onde as mensagens ficam armazenadas até algum serviço consumir.
// O Binding liga uma Queue a uma Exchange usando uma routing key. Ele responde: "mensagens que chegarem nesta exchange com esta routing key devem ir para esta fila".

@Configuration
public class RabbitMQConfig {

    // Lê do application.properties o nome da exchange central do fluxo de pedidos.
    @Value("${commerce.rabbitmq.exchange}")
    private String pedidosExchange;

    // Fila que receberá mensagens quando um pedido for solicitado.
    @Value("${commerce.rabbitmq.queue.pedido-solicitado}")
    private String pedidoSolicitadoQueue;

    // Fila que receberá a resposta do estoque sobre o pedido.
    @Value("${commerce.rabbitmq.queue.estoque-resposta}")
    private String estoqueRespostaQueue;

    // Chave usada para rotear mensagens do tipo pedido solicitado.
    @Value("${commerce.rabbitmq.routing-key.pedido-solicitado}")
    private String pedidoSolicitadoRoutingKey;

    // Chave usada para rotear mensagens de resposta do estoque.
    @Value("${commerce.rabbitmq.routing-key.estoque-resposta}")
    private String estoqueRespostaRoutingKey;

    // Cria uma exchange direta: a mensagem vai para a fila cuja routing key bater.
    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(
                pedidosExchange,
                true,  // durable: a exchange continua existindo após reiniciar o RabbitMQ.
                false  // autoDelete: false evita apagar a exchange quando ninguém estiver usando.
        );
    }

    // Cria a fila durável que o serviço estoque usará para consumir pedidos.
    @Bean
    public Queue pedidoSolicitadoQueue() {
        return QueueBuilder
                .durable(pedidoSolicitadoQueue)
                .build();
    }

    // Cria a fila durável que o serviço pedido usará para consumir respostas do estoque.
    @Bean
    public Queue estoqueRespostaQueue() {
        return QueueBuilder
                .durable(estoqueRespostaQueue)
                .build();
    }

    // Liga a fila de pedido solicitado na exchange usando a routing key pedido.solicitado.
    @Bean
    public Binding pedidoSolicitadoBinding(
            @Qualifier("pedidoSolicitadoQueue")
            Queue pedidoSolicitadoQueue,
            DirectExchange pedidosExchange
    ) {
        return BindingBuilder
                .bind(pedidoSolicitadoQueue)
                .to(pedidosExchange)
                .with(pedidoSolicitadoRoutingKey);
    }

    // Liga a fila de resposta do estoque na exchange usando a routing key estoque.resposta.
    @Bean
    public Binding estoqueRespostaBinding(
            @Qualifier("estoqueRespostaQueue")
            Queue estoqueRespostaQueue,
            DirectExchange pedidosExchange
    ) {
        return BindingBuilder
                .bind(estoqueRespostaQueue)
                .to(pedidosExchange)
                .with(estoqueRespostaRoutingKey);
    }

    // Converte objetos Java para JSON antes de publicar e JSON para Java ao consumir.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

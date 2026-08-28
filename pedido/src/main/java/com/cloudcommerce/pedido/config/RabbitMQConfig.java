package com.cloudcommerce.pedido.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O producer Declara para uma exchange quero publicar uma mensagem usando a ROUTING KEY X então é verificado se a key esta parametrizada na exchange olhando nos seus BINDINGS
// Estando ele busca a fila Retornada no "join" da exchange com a routing key e publica a mensagem na fila correta.

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


    
    // Topic exchange usada para auditoria, demonstrando roteamento por padrões.
    @Value("${commerce.rabbitmq.topic-exchange.auditoria}")
    private String auditoriaTopicExchange;

    // Fila que recebe eventos de auditoria dos pedidos.
    @Value("${commerce.rabbitmq.queue.auditoria-pedido}")
    private String auditoriaPedidoQueue;

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

    // Cria uma TopicExchange para eventos de auditoria.
    // O tipo topic permite bindings com padrões, por exemplo pedido.#.
    @Bean
    public TopicExchange auditoriaTopicExchange() {
        return new TopicExchange(
                auditoriaTopicExchange,
                true,
                false
        );
    }

    // Cria a fila onde chegam eventos de auditoria relacionados a pedidos.
    @Bean
    public Queue auditoriaPedidoQueue() {
        return QueueBuilder
                .durable(auditoriaPedidoQueue)
                .build();
    }

    // Liga a fila de auditoria na TopicExchange.
    // O padrão pedido.# recebe qualquer evento de auditoria iniciado por pedido.
    @Bean
    public Binding auditoriaPedidoBinding(
            @Qualifier("auditoriaPedidoQueue")
            Queue auditoriaPedidoQueue,
            TopicExchange auditoriaTopicExchange
    ) {
        return BindingBuilder
                .bind(auditoriaPedidoQueue)
                .to(auditoriaTopicExchange)
                .with("pedido.#");
    }

    // Converte objetos Java para JSON antes de publicar e JSON para Java ao consumir.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

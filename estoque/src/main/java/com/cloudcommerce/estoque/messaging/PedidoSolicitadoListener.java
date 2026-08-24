package com.cloudcommerce.estoque.messaging;

import com.cloudcommerce.estoque.service.EstoquePedidoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PedidoSolicitadoListener {

    private final EstoquePedidoService estoquePedidoService;
    private final EstoqueRespostaProducer estoqueRespostaProducer;

    public PedidoSolicitadoListener(
            EstoquePedidoService estoquePedidoService,
            EstoqueRespostaProducer estoqueRespostaProducer
    ) {
        this.estoquePedidoService = estoquePedidoService;
        this.estoqueRespostaProducer = estoqueRespostaProducer;
    }

    // Conecta este método à fila onde chegam pedidos solicitados.
    // A transação evita confirmar baixa de estoque se a publicação da resposta falhar.
    @Transactional
    @RabbitListener(queues = "${commerce.rabbitmq.queue.pedido-solicitado}")
    public void receberPedidoSolicitado(PedidoSolicitadoMessage mensagem) {
        EstoqueRespostaMessage resposta =
                estoquePedidoService.processarPedidoSolicitado(mensagem);

        estoqueRespostaProducer.enviarResposta(resposta);
    }
}

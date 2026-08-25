package com.cloudcommerce.pedido.messaging;

import com.cloudcommerce.pedido.service.ListnerService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EstoqueRespostaListener {

    private final ListnerService pedidoService;

    public EstoqueRespostaListener(ListnerService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @RabbitListener(queues = "${commerce.rabbitmq.queue.estoque-resposta}")
    public void receberRespostaEstoque(EstoqueRespostaMessage mensagem) {
        pedidoService.atualizarStatusComRespostaEstoque(mensagem);
    }
}
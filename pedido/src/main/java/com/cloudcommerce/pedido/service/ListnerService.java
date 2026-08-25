package com.cloudcommerce.pedido.service;

import com.cloudcommerce.pedido.messaging.EstoqueRespostaMessage;
import com.cloudcommerce.pedido.model.Pedido;
import com.cloudcommerce.pedido.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class ListnerService {

    private final PedidoRepository pedidoRepository;

    public ListnerService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public void atualizarStatusComRespostaEstoque(
            EstoqueRespostaMessage mensagem
    ) {
        validarRespostaEstoque(mensagem);

        Pedido pedido = pedidoRepository.findById(mensagem.pedidoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pedido " + mensagem.pedidoId() + " não encontrado."
                ));

        pedido.setStatus(mensagem.status());

        pedidoRepository.save(pedido);
    }

    private void validarRespostaEstoque(EstoqueRespostaMessage mensagem) {
        if (mensagem == null) {
            throw new IllegalArgumentException("Resposta do estoque não informada.");
        }

        if (mensagem.pedidoId() == null) {
            throw new IllegalArgumentException("Resposta do estoque sem pedidoId.");
        }

        if (mensagem.status() == null || mensagem.status().isBlank()) {
            throw new IllegalArgumentException("Resposta do estoque sem status.");
        }
    }
}

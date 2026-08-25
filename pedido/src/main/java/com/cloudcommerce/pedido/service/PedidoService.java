package com.cloudcommerce.pedido.service;

import com.cloudcommerce.pedido.dto.CriarPedidoRequest;
import com.cloudcommerce.pedido.messaging.PedidoSolicitadoMessage;
import com.cloudcommerce.pedido.messaging.PedidoSolicitadoProducer;
import com.cloudcommerce.pedido.model.Pedido;
import com.cloudcommerce.pedido.model.PedidoItem;
import com.cloudcommerce.pedido.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private static final String STATUS_PROCESSANDO = "PROCESSANDO";

    private final PedidoRepository pedidoRepository;
    private final PedidoSolicitadoProducer pedidoSolicitadoProducer;

    public PedidoService(
            PedidoRepository pedidoRepository,
            PedidoSolicitadoProducer pedidoSolicitadoProducer
    ) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoSolicitadoProducer = pedidoSolicitadoProducer;
    }

    public Pedido criar(CriarPedidoRequest request) {
        validarRequest(request);

        Pedido pedido = new Pedido();
        pedido.setStatus(STATUS_PROCESSANDO);

        List<PedidoItem> itens = new ArrayList<>();

        for (CriarPedidoRequest.Item itemRequest : request.itens()) {
            itens.add(criarItem(pedido, itemRequest));
        }

        pedido.setItens(itens);
        pedido.setValorTotal(calcularValorTotal(itens));

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // Publica depois do save para o estoque responder a um pedido ja gravado.
        pedidoSolicitadoProducer.enviar(criarMensagem(pedidoSalvo));

        return pedidoSalvo;
    }

    private PedidoItem criarItem(
            Pedido pedido,
            CriarPedidoRequest.Item itemRequest
    ) {
        PedidoItem item = new PedidoItem();

        item.setPedido(pedido);
        item.setProdutoId(itemRequest.produtoId());
        item.setQuantidade(itemRequest.quantidade());
        item.setPrecoUnitario(itemRequest.precoUnitario());

        return item;
    }

    private BigDecimal calcularValorTotal(List<PedidoItem> itens) {
        return itens.stream()
                .map(item -> item.getPrecoUnitario()
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PedidoSolicitadoMessage criarMensagem(Pedido pedido) {
        List<PedidoSolicitadoMessage.Item> itensMensagem = pedido.getItens()
                .stream()
                .map(item -> new PedidoSolicitadoMessage.Item(
                        item.getProdutoId(),
                        item.getQuantidade()
                ))
                .toList();

        return new PedidoSolicitadoMessage(
                pedido.getId(),
                pedido.getValorTotal(),
                pedido.getCriadoEm(),
                itensMensagem
        );
    }

    private void validarRequest(CriarPedidoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("O pedido precisa ser informado.");
        }

        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter pelo menos um item.");
        }

        for (CriarPedidoRequest.Item item : request.itens()) {
            validarItem(item);
        }
    }

    private void validarItem(CriarPedidoRequest.Item item) {
        if (item == null) {
            throw new IllegalArgumentException("O item do pedido precisa ser informado.");
        }

        if (item.produtoId() == null) {
            throw new IllegalArgumentException("O produto do item precisa ser informado.");
        }

        if (item.quantidade() == null || item.quantidade() <= 0) {
            throw new IllegalArgumentException("A quantidade do item precisa ser maior que zero.");
        }

        if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preco unitario do item precisa ser maior que zero.");
        }
    }
}

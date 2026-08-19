package com.cloudcommerce.pedido.dto;

import com.cloudcommerce.pedido.model.PedidoItem;

import java.math.BigDecimal;

public record PedidoItemResponse(
        Long id,
        Long produtoId,
        Integer quantidade,
        BigDecimal precoUnitario
) {

    public static PedidoItemResponse from(PedidoItem item) {
        return new PedidoItemResponse(
                item.getId(),
                item.getProdutoId(),
                item.getQuantidade(),
                item.getPrecoUnitario()
        );
    }
}

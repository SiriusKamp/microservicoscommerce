package com.cloudcommerce.pedido.dto;

import java.math.BigDecimal;
import java.util.List;

public record CriarPedidoRequest(
        List<Item> itens
) {
    public record Item(
            Long produtoId,
            Integer quantidade,
            BigDecimal precoUnitario
    ) {
    }
}
package com.cloudcommerce.pedido.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoSolicitadoMessage(
        Long pedidoId,
        BigDecimal valorTotal,
        LocalDateTime solicitadoEm,
        List<Item> itens
) {

    public record Item(
            Long produtoId,
            Integer quantidade
    ) {
    }
}

package com.cloudcommerce.pedido.messaging;

import java.time.LocalDateTime;

public record EstoqueRespostaMessage(
        Long pedidoId,
        Boolean estoqueDisponivel,
        String status,
        String motivo,
        LocalDateTime respondidoEm
) {
}

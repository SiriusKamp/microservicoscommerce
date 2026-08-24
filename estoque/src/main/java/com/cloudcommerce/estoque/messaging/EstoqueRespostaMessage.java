package com.cloudcommerce.estoque.messaging;

import java.time.LocalDateTime;

public record EstoqueRespostaMessage(
        Long pedidoId,
        Boolean estoqueDisponivel,
        String status,
        String motivo,
        LocalDateTime respondidoEm
) {
}

package com.cloudcommerce.pedido.messaging;

import java.time.LocalDateTime;

public record PedidoAuditoriaMessage(
        Long pedidoId,
        String statusAtual,
        String motivo,
        LocalDateTime criadoEm,
        LocalDateTime auditadoEm
) {
}

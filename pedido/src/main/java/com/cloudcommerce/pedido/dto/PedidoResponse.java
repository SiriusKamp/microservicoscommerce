package com.cloudcommerce.pedido.dto;

import com.cloudcommerce.pedido.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String status,
        BigDecimal valorTotal,
        LocalDateTime criadoEm,
        List<PedidoItemResponse> itens
) {

    public static PedidoResponse from(Pedido pedido) {

        List<PedidoItemResponse> itens =
                pedido.getItens() == null
                        ? List.of()
                        : pedido.getItens()
                                .stream()
                                .map(PedidoItemResponse::from)
                                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                pedido.getCriadoEm(),
                itens
        );
    }
}

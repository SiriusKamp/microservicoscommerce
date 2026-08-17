package com.cloudcommerce.pedido.controller;


import com.cloudcommerce.pedido.model.PedidoItem;
import com.cloudcommerce.pedido.repository.PedidoItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedido-itens")
@CrossOrigin(origins = "http://localhost:8081")
public class PedidoItemController {

    private final PedidoItemRepository pedidoItemRepository;

    public PedidoItemController(PedidoItemRepository pedidoItemRepository) {
        this.pedidoItemRepository = pedidoItemRepository;
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<PedidoItem> listarPorPedido(
            @PathVariable Long pedidoId
    ) {
        return pedidoItemRepository.findByPedidoId(pedidoId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoItem> buscar(
            @PathVariable Long id
    ) {
        return pedidoItemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
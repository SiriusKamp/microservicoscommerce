package com.cloudcommerce.pedido.controller;

import com.cloudcommerce.pedido.dto.PedidoResponse;
import com.cloudcommerce.pedido.dto.CriarPedidoRequest;
import com.cloudcommerce.pedido.repository.PedidoRepository;
import com.cloudcommerce.pedido.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@CrossOrigin(origins = "http://localhost:8081")
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;

    public PedidoController(
            PedidoRepository pedidoRepository,
            PedidoService pedidoService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable Long id) {

        return pedidoRepository.findById(id)
                .map(PedidoResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(
            @RequestBody CriarPedidoRequest request
    ) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(PedidoResponse.from(pedidoService.criar(request)));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }
}

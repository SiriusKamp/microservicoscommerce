package com.cloudcommerce.estoque.controller;

import com.cloudcommerce.estoque.model.Estoque;
import com.cloudcommerce.estoque.repository.EstoqueRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueRepository estoqueRepository;

    public EstoqueController(EstoqueRepository estoqueRepository) {
        this.estoqueRepository = estoqueRepository;
    }

    @GetMapping
    public List<Estoque> listar() {
        return estoqueRepository.findAll();
    }

    @GetMapping("/{idProduto}")
    public Estoque buscarPorProduto(@PathVariable Long idProduto) {
        return estoqueRepository
                .findByProdutoId(idProduto)
                .orElseThrow();
    }
}
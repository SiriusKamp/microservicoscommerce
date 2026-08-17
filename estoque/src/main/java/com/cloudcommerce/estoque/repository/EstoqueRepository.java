package com.cloudcommerce.estoque.repository;

import com.cloudcommerce.estoque.model.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByProdutoId(Long produtoId);
}
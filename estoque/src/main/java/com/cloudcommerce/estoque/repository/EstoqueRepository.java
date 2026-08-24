package com.cloudcommerce.estoque.repository;

import com.cloudcommerce.estoque.model.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByProdutoId(Long produtoId);

    // Bloqueia a linha do produto enquanto o estoque está sendo baixado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select estoque from Estoque estoque where estoque.produtoId = :produtoId")
    Optional<Estoque> findByProdutoIdParaAtualizar(
            @Param("produtoId") Long produtoId
    );
}

package com.cloudcommerce.pedido.repository;

import com.cloudcommerce.pedido.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Override
    @EntityGraph(attributePaths = "itens")
    List<Pedido> findAll();

    @Override
    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findById(Long id);

    List<Pedido> findByStatusAndCriadoEmBefore(
            String status,
            LocalDateTime criadoEm
    );
}

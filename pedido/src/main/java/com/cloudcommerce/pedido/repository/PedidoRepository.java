package com.cloudcommerce.pedido.repository;

import com.cloudcommerce.pedido.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
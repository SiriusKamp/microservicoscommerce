package com.cloudcommerce.pedido.repository;

import com.cloudcommerce.pedido.model.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {

    List<PedidoItem> findByPedidoId(Long pedidoId);

}
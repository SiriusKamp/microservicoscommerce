package com.cloudcommerce.pedido.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pedido_itens")
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(
            name = "preco_unitario",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal precoUnitario;

    public PedidoItem() {
    }

    public PedidoItem(
            Long id,
            Pedido pedido,
            Long produtoId,
            Integer quantidade,
            BigDecimal precoUnitario
    ) {
        this.id = id;
        this.pedido = pedido;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
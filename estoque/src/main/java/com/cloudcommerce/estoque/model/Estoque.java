package com.cloudcommerce.estoque.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estoque")
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_id", nullable = false, unique = true)
    private Long produtoId;

    @Column(nullable = false)
    private Integer quantidade;

    public Estoque() {
    }

    public Estoque(
            Long id,
            Long produtoId,
            Integer quantidade
    ) {
        this.id = id;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
    }

    public Long getId() {
        return id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }
}
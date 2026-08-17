package com.sirius.cloud_commerce.Model;

public class Produto {

    private Long id;
    private String nome;
    private String descricao;
    private Double preco;
    private Integer estoque;
    private String categoria;

    public Produto(
            Long id,
            String nome,
            String descricao,
            Double preco,
            Integer estoque,
            String categoria
    ) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
        this.categoria = categoria;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Double getPreco() {
        return preco;
    }

    public Integer getEstoque() {
        return estoque;
    }

    public String getCategoria() {
        return categoria;
    }
}
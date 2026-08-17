package com.sirius.cloud_commerce.Model;

public class ItemCarrinho {

    private Produto produto;
    private Integer quantidade;

    public ItemCarrinho(Produto produto, Integer quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Produto getProduto() {
        return produto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void aumentarQuantidade() {
        this.quantidade++;
    }

    public void diminuirQuantidade() {
        if (this.quantidade > 0) {
            this.quantidade--;
        }
    }

    public Double getSubtotal() {
        return produto.getPreco() * quantidade;
    }
}
package com.sirius.cloud_commerce.Model;

import java.time.LocalDate;
import java.util.List;

public class Pedido {

    private Long id;
    private LocalDate data;
    private String status;
    private Double valorTotal;
    private List<ItemCarrinho> itens;

    public Pedido(
            Long id,
            LocalDate data,
            String status,
            Double valorTotal,
            List<ItemCarrinho> itens
    ) {
        this.id = id;
        this.data = data;
        this.status = status;
        this.valorTotal = valorTotal;
        this.itens = itens;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getData() {
        return data;
    }

    public String getStatus() {
        return status;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }
}
package com.cloudcommerce.pedido.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PedidoItem> itens = new ArrayList<>();

    public Pedido() {
    }

    public Pedido(
            Long id,
            String status,
            BigDecimal valorTotal,
            LocalDateTime criadoEm
    ) {
        this.id = id;
        this.status = status;
        this.valorTotal = valorTotal;
        this.criadoEm = criadoEm;
    }

    @PrePersist
    protected void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<PedidoItem> getItens() {
        return itens;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public void setItens(List<PedidoItem> itens) {
        this.itens = itens;
    }
}
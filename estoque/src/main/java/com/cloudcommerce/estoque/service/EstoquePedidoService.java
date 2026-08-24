package com.cloudcommerce.estoque.service;

import com.cloudcommerce.estoque.messaging.EstoqueRespostaMessage;
import com.cloudcommerce.estoque.messaging.PedidoSolicitadoMessage;
import com.cloudcommerce.estoque.model.Estoque;
import com.cloudcommerce.estoque.repository.EstoqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EstoquePedidoService {

    private static final String STATUS_PROCESSADO = "PROCESSADO";
    private static final String STATUS_SEM_ESTOQUE = "SEM_ESTOQUE";

    private final EstoqueRepository estoqueRepository;

    public EstoquePedidoService(EstoqueRepository estoqueRepository) {
        this.estoqueRepository = estoqueRepository;
    }

    // A transação mantém a verificação e a baixa de estoque como uma operação única.
    @Transactional
    public EstoqueRespostaMessage processarPedidoSolicitado(
            PedidoSolicitadoMessage mensagem
    ) {
        String motivoMensagemInvalida =
                validarMensagem(mensagem);

        if (motivoMensagemInvalida != null) {
            return criarRespostaSemEstoque(
                    obterPedidoId(mensagem),
                    motivoMensagemInvalida
            );
        }

        Map<Long, Integer> itensSolicitados =
                agruparItensPorProduto(mensagem);

        List<Estoque> estoques =
                new ArrayList<>();

        for (Map.Entry<Long, Integer> item : itensSolicitados.entrySet()) {
            Estoque estoque =
                    estoqueRepository
                            .findByProdutoIdParaAtualizar(item.getKey())
                            .orElse(null);

            if (estoque == null) {
                return criarRespostaSemEstoque(
                        obterPedidoId(mensagem),
                        "Produto " + item.getKey() + " não possui estoque cadastrado."
                );
            }

            if (estoque.getQuantidade() < item.getValue()) {
                return criarRespostaSemEstoque(
                        obterPedidoId(mensagem),
                        "Produto " + item.getKey()
                                + " possui " + estoque.getQuantidade()
                                + " unidade(s), mas o pedido solicitou "
                                + item.getValue() + "."
                );
            }

            estoques.add(estoque);
        }

        estoques.forEach(estoque -> {
            Integer quantidadeSolicitada =
                    itensSolicitados.get(estoque.getProdutoId());

            estoque.setQuantidade(
                    estoque.getQuantidade() - quantidadeSolicitada
            );
        });

        estoqueRepository.saveAll(estoques);

        return new EstoqueRespostaMessage(
                obterPedidoId(mensagem),
                true,
                STATUS_PROCESSADO,
                "Estoque baixado com sucesso.",
                LocalDateTime.now()
        );
    }

    private Map<Long, Integer> agruparItensPorProduto(
            PedidoSolicitadoMessage mensagem
    ) {
        Map<Long, Integer> itensSolicitados =
                new LinkedHashMap<>();

        mensagem.itens().forEach(item -> {
            itensSolicitados.merge(
                    item.produtoId(),
                    item.quantidade(),
                    Integer::sum
            );
        });

        return itensSolicitados;
    }

    private String validarMensagem(PedidoSolicitadoMessage mensagem) {
        if (mensagem == null) {
            return "Mensagem de pedido não informada.";
        }

        if (mensagem.pedidoId() == null) {
            return "Mensagem de pedido sem pedidoId.";
        }

        if (mensagem.itens() == null || mensagem.itens().isEmpty()) {
            return "Pedido sem itens para verificar.";
        }

        for (PedidoSolicitadoMessage.Item item : mensagem.itens()) {
            if (item.produtoId() == null) {
                return "Pedido possui item sem produtoId.";
            }

            if (item.quantidade() == null || item.quantidade() <= 0) {
                return "Pedido possui item com quantidade inválida.";
            }
        }

        return null;
    }

    private EstoqueRespostaMessage criarRespostaSemEstoque(
            Long pedidoId,
            String motivo
    ) {
        return new EstoqueRespostaMessage(
                pedidoId,
                false,
                STATUS_SEM_ESTOQUE,
                motivo,
                LocalDateTime.now()
        );
    }

    private Long obterPedidoId(PedidoSolicitadoMessage mensagem) {
        return mensagem == null
                ? null
                : mensagem.pedidoId();
    }
}

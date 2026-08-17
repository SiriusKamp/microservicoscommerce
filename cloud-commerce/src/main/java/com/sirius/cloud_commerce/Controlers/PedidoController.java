package com.sirius.cloud_commerce.Controlers;

import com.sirius.cloud_commerce.Model.ItemCarrinho;
import com.sirius.cloud_commerce.Model.Pedido;
import com.sirius.cloud_commerce.Model.Produto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class PedidoController {

    @GetMapping("/pedidos")
    public String pedidos(Model model) {

        List<Pedido> pedidos = criarPedidosMock();

        model.addAttribute("pedidos", pedidos);

        return "pedidos";
    }

    private List<Pedido> criarPedidosMock() {

        Produto mouse = new Produto(
                2L,
                "Mouse Gamer",
                "Mouse óptico de alta precisão.",
                150.00,
                25,
                "Periféricos"
        );

        Produto teclado = new Produto(
                3L,
                "Teclado Mecânico",
                "Teclado mecânico para programação e jogos.",
                350.00,
                8,
                "Periféricos"
        );

        List<ItemCarrinho> itensPedido1 = List.of(
                new ItemCarrinho(mouse, 1),
                new ItemCarrinho(teclado, 1)
        );

        Pedido pedido1 = new Pedido(
                1001L,
                LocalDate.of(2026, 8, 10),
                "FINALIZADO",
                500.00,
                itensPedido1
        );

        return List.of(pedido1);
    }
}
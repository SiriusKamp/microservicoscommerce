package com.sirius.cloud_commerce.Controlers;

import com.sirius.cloud_commerce.Model.ItemCarrinho;
import com.sirius.cloud_commerce.Model.Produto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class CarrinhoController {

    @GetMapping("/carrinho")
    public String carrinho(Model model) {

        List<ItemCarrinho> carrinho = criarCarrinhoMock();

        Double total = carrinho.stream()
                .mapToDouble(ItemCarrinho::getSubtotal)
                .sum();

        model.addAttribute("carrinho", carrinho);
        model.addAttribute("total", total);

        return "carrinho";
    }

    private List<ItemCarrinho> criarCarrinhoMock() {

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

        return List.of(
                new ItemCarrinho(mouse, 1),
                new ItemCarrinho(teclado, 1)
        );
    }
}
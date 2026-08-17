package com.sirius.cloud_commerce.Controlers;

import com.sirius.cloud_commerce.Model.Produto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class EstoqueController {

    @GetMapping("/estoque")
    public String estoque(Model model) {

        List<Produto> produtos = criarProdutosMock();

        model.addAttribute("produtos", produtos);

        return "estoque";
    }

    private List<Produto> criarProdutosMock() {

        return List.of(

                new Produto(
                        1L,
                        "Notebook Gamer",
                        "Notebook para jogos e desenvolvimento.",
                        4500.00,
                        10,
                        "Computadores"
                ),

                new Produto(
                        2L,
                        "Mouse Gamer",
                        "Mouse óptico de alta precisão.",
                        150.00,
                        25,
                        "Periféricos"
                ),

                new Produto(
                        3L,
                        "Teclado Mecânico",
                        "Teclado mecânico para programação e jogos.",
                        350.00,
                        8,
                        "Periféricos"
                ),

                new Produto(
                        4L,
                        "Monitor 27",
                        "Monitor Full HD de 27 polegadas.",
                        1200.00,
                        5,
                        "Monitores"
                )
        );
    }
}
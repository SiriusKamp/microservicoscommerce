package com.sirius.cloud_commerce.Controlers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class CarrinhoController {

    @GetMapping("/carrinho")
    public String carrinho(Model model) {



        return "carrinho";
    }

}
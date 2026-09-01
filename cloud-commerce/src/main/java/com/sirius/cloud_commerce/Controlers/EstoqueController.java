package com.sirius.cloud_commerce.Controlers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class EstoqueController {

    @GetMapping("/estoque")
    public String estoque(Model model) {


        model.addAttribute("produtos");

        return "estoque";
    }

}
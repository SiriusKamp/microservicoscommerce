package com.sirius.cloud_commerce.Controlers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PedidoController {

    @GetMapping("/pedidos")
    public String pedidos() {
        return "pedidos";
    }
}

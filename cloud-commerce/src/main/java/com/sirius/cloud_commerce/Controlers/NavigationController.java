package com.sirius.cloud_commerce.Controlers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NavigationController {

    @GetMapping("/teste")
    public String hello() {
        return "Hello Sirius World!";
    }
}

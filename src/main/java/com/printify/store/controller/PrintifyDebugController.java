package com.printify.store.controller;

import com.printify.store.service.PrintifyDebugService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/printify")
@RequiredArgsConstructor
public class PrintifyDebugController {

    private final PrintifyDebugService printifyDebugService;

    @GetMapping("/shops")
    public String getShops() {
        return printifyDebugService.getShops();
    }

    @GetMapping("/products")
    public String getProducts() {
        return printifyDebugService.getProducts();
    }
}
package com.printify.store.controller.admin;

import com.printify.store.service.PrintifyProductSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/printify")
@RequiredArgsConstructor
public class PrintifySyncController {

    private final PrintifyProductSyncService syncService;

    @PostMapping("/sync-products")
    public Map<String, Object> syncProducts() {
        int count = syncService.syncProductsFromPrintify();

        return Map.of(
                "success", true,
                "syncedProducts", count
        );
    }
}
package com.printify.store.scheduler;

import com.printify.store.service.PrintifyProductSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrintifySyncScheduler {

    private final PrintifyProductSyncService syncService;

    @Scheduled(fixedDelay = 600000)
    public void syncPrintifyProducts() {
        syncService.syncProductsFromPrintify();
    }
}
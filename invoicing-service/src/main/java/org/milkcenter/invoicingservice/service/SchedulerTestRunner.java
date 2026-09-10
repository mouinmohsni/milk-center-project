package org.milkcenter.invoicingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.milkcenter.invoicingservice.service.MonthlyInvoiceScheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("scheduler-test")
@RequiredArgsConstructor
@Slf4j
public class SchedulerTestRunner implements CommandLineRunner {

    private final MonthlyInvoiceScheduler monthlyInvoiceScheduler;

    @Override
    public void run(String... args) {
        log.info("=== DEMARRAGE DU TEST DU SCHEDULER MENSUEL ===");

        try {
            monthlyInvoiceScheduler.runNowForTest();
            log.info("=== TEST DU SCHEDULER TERMINE AVEC SUCCES ===");
        } catch (Exception exception) {
            log.error("=== ERREUR PENDANT LE TEST DU SCHEDULER ===", exception);
        }
    }
}


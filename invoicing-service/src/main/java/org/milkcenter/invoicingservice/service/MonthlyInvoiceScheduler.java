package org.milkcenter.invoicingservice.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.milkcenter.invoicingservice.dto.response.client.FarmerClientResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyInvoiceScheduler {

    private final InvoiceService invoiceService;
    private final CollectionServiceResilientClient collectionServiceClient;

    @Scheduled(cron = "0 0 0 1 * *")
    public void createCurrentMonthDraftInvoices() {
        LocalDate currentMonth = LocalDate.now();
        int month = currentMonth.getMonthValue();
        int year = currentMonth.getYear();

        List<FarmerClientResponse> farmers =
                collectionServiceClient.getAllFarmers();

        if (farmers == null) {
            return;
        }

        farmers.stream()
                .filter(farmer -> Boolean.TRUE.equals(farmer.getActive()))
                .forEach(farmer -> {
                    try {
                        invoiceService.createScheduledDraftInvoice(
                                farmer.getId(),
                                farmer.getUserId(),
                                month,
                                year
                        );
                    } catch (Exception exception) {
                        log.error(
                                "Erreur création facture farmerId={}, période={}/{}",
                                farmer.getId(),
                                month,
                                year,
                                exception
                        );
                    }
                });
    }

    public void runNowForTest() {
        createCurrentMonthDraftInvoices();
    }



}
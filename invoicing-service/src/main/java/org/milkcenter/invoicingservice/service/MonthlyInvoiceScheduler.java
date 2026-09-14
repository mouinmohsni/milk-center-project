package org.milkcenter.invoicingservice.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.milkcenter.invoicingservice.dto.response.client.FarmerClientResponse;
import org.milkcenter.invoicingservice.enums.InvoiceStatus;
import org.milkcenter.invoicingservice.event.InvoiceNotificationEventProducer;
import org.milkcenter.invoicingservice.model.Invoice;
import org.milkcenter.invoicingservice.repository.InvoiceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyInvoiceScheduler {

    private final InvoiceService invoiceService;
    private final CollectionServiceResilientClient collectionServiceClient;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNotificationEventProducer notificationEventProducer;

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

    /**
     * Le 5 de chaque mois, recherche les factures DRAFT du mois précédent
     * et publie un événement de rappel pour chaque facture non traitée.
     */
    @Scheduled(
            cron = "0 0 0 5 * *",
            zone = "Africa/Tunis"
    )
    public void sendPreviousMonthDraftInvoiceReminders() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);

        int month = previousMonth.getMonthValue();
        int year = previousMonth.getYear();

        log.info(
                "Recherche des factures DRAFT non traitées pour la période {}/{}",
                month,
                year
        );

        List<Invoice> draftInvoices = invoiceRepository
                .findByStatusAndBillingMonthAndBillingYear(
                        InvoiceStatus.DRAFT,
                        month,
                        year
                );

        if (draftInvoices == null || draftInvoices.isEmpty()) {
            log.info(
                    "Aucune facture DRAFT non traitée trouvée pour la période {}/{}",
                    month,
                    year
            );
            return;
        }

        for (Invoice invoice : draftInvoices) {
            try {
                notificationEventProducer.publishInvoiceProcessingReminder(invoice);

                log.info(
                        "Rappel de traitement publié : invoiceId={}, farmerId={}, période={}/{}",
                        invoice.getId(),
                        invoice.getFarmerId(),
                        month,
                        year
                );
            } catch (Exception exception) {
                log.error(
                        "Erreur publication rappel invoiceId={}, période={}/{}",
                        invoice.getId(),
                        month,
                        year,
                        exception
                );
            }
        }
    }

    public void runNowForTest() {
        sendPreviousMonthDraftInvoiceReminders();
    }




}
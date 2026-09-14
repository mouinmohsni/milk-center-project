package org.milkcenter.invoicingservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.milkcenter.invoicingservice.service.InvoiceService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MilkCollectionEventConsumer {

    private final InvoiceService invoiceService;

    @KafkaListener(
            topics = "${app.kafka.topics.milk-collection-events:milk-collection-events}",
            groupId = "${spring.kafka.consumer.group-id:invoicing-service-group}"
    )
    public void consume(MilkCollectionStatusChangedEvent event) {
        if (event == null) {
            log.warn("Événement Kafka ignoré : payload null");
            return;
        }

        log.info(
                "Événement Kafka reçu : eventId={}, collectionId={}, farmerId={}, status={}, quantity={}",
                event.getEventId(),
                event.getCollectionId(),
                event.getFarmerId(),
                event.getStatus(),
                event.getQuantityLiters()
        );

        if (!"ACCEPTED".equalsIgnoreCase(event.getStatus())) {
            log.info("Événement ignoré par invoicing-service : status={}", event.getStatus());
            return;
        }

        if (event.getCollectionId() == null
                || event.getFarmerId() == null
                || event.getQuantityLiters() == null) {
            log.error("Événement ACCEPTED invalide : collectionId, farmerId et quantityLiters sont obligatoires");
            throw new IllegalArgumentException("Événement de collecte incomplet");
        }

        invoiceService.processAcceptedCollection(event);
    }
}
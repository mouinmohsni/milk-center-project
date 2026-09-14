package org.milkcenter.notificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.milkcenter.notificationservice.model.NotificationType;
import org.milkcenter.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component @RequiredArgsConstructor @Slf4j
public class NotificationKafkaConsumer {
    private final ObjectMapper mapper;
    private final NotificationService service;

    @Value("${app.notification.manager-ids:}")
    private String configuredManagerIds;

    private List<Long> managerIds() {
        if (configuredManagerIds == null || configuredManagerIds.isBlank()) return List.of();
        return Arrays.stream(configuredManagerIds.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).map(Long::valueOf).toList();
    }

    @KafkaListener(topics="${app.kafka.topics.invoice-created:invoice-notification-events}", groupId="${spring.kafka.consumer.group-id:notification-service-group}")
    public void invoiceCreated(String json) throws Exception {
        var e = mapper.readTree(json);
        service.createForUsers(text(e,"eventId"), NotificationType.INVOICE_CREATED, "Nouvelle facture",
                "Une facture " + text(e,"invoiceNumber") + " a été créée.", number(e,"invoiceId"), managerIds(), "MANAGER");
    }
    @KafkaListener(topics="${app.kafka.topics.invoice-line-added:invoice-line-notification-events}", groupId="${spring.kafka.consumer.group-id:notification-service-group}")
    public void lineAdded(String json) throws Exception {
        var e = mapper.readTree(json);
        service.createForUsers(text(e,"eventId"), NotificationType.INVOICE_LINE_ADDED, "Ligne ajoutée",
                "Une collecte a été ajoutée à la facture " + text(e,"invoiceNumber") + ".", number(e,"invoiceId"), managerIds(), "MANAGER");
    }
    @KafkaListener(topics="${app.kafka.topics.invoice-reminder:invoice-processing-reminder-events}", groupId="${spring.kafka.consumer.group-id:notification-service-group}")
    public void reminder(String json) throws Exception {
        var e = mapper.readTree(json);
        service.createForUsers(text(e,"eventId"), NotificationType.INVOICE_PROCESSING_REMINDER, "Facture à traiter",
                "La facture " + text(e,"invoiceNumber") + " est toujours en DRAFT.", number(e,"invoiceId"), managerIds(), "MANAGER");
    }
    @KafkaListener(topics="${app.kafka.topics.invoice-processed:invoice-processed-events}", groupId="${spring.kafka.consumer.group-id:notification-service-group}")
    public void processed(String json) throws Exception {
        var e = mapper.readTree(json);
        service.createForUser(text(e,"eventId"), NotificationType.INVOICE_PROCESSED, "Facture traitée",
                "Votre facture " + text(e,"invoiceNumber") + " a été traitée.", number(e,"invoiceId"), number(e,"farmerUserId"), "FARMER");
    }
    @KafkaListener(topics="${app.kafka.topics.invoice-cancelled:invoice-cancelled-events}", groupId="${spring.kafka.consumer.group-id:notification-service-group}")
    public void cancelled(String json) throws Exception {
        var e = mapper.readTree(json);
        service.createForUser(text(e,"eventId"), NotificationType.INVOICE_CANCELLED, "Facture annulée",
                "Votre facture " + text(e,"invoiceNumber") + " a été annulée.", number(e,"invoiceId"), number(e,"farmerUserId"), "FARMER");
    }
    @KafkaListener(topics="${app.kafka.topics.collection-events:milk-collection-events}", groupId="${spring.kafka.consumer.group-id:notification-service-group}")
    public void collection(String json) throws Exception {
        var e = mapper.readTree(json); String status = text(e,"status");
        if (!List.of("ACCEPTED", "CORRECTED", "REJECTED", "CANCELLED").contains(status == null ? "" : status.toUpperCase())) return;
        NotificationType type = switch (status.toUpperCase()) {
            case "ACCEPTED" -> NotificationType.COLLECTION_ACCEPTED;
            case "CORRECTED" -> NotificationType.COLLECTION_CORRECTED;
            case "REJECTED" -> NotificationType.COLLECTION_REJECTED;
            case "CANCELLED" -> NotificationType.COLLECTION_CANCELLED;
            default -> throw new IllegalArgumentException("Statut de collecte inconnu: " + status);
        };
        service.createForUser(text(e,"eventId"), type, "Statut de collecte modifié",
                "La collecte " + text(e,"collectionId") + " est passée au statut " + status + ".", number(e,"collectionId"), number(e,"farmerId"), "FARMER");
    }
    private static String text(com.fasterxml.jackson.databind.JsonNode n, String f) { return n.path(f).isMissingNode() ? null : n.path(f).asText(); }
    private static Long number(com.fasterxml.jackson.databind.JsonNode n, String f) { return n.path(f).isNumber() ? n.path(f).longValue() : null; }
}

package org.milkcenter.collectionservice.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilkCollectionEventPublisher {

    public static final String TOPIC = "milk-collection-events";

    private final KafkaTemplate<String, MilkCollectionStatusChangedEvent> kafkaTemplate;

    public void publishStatusChanged(MilkCollectionStatusChangedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "L'événement de collecte ne peut pas être null"
            );
        }

        if (event.getCollectionId() == null) {
            throw new IllegalArgumentException(
                    "L'identifiant de la collecte ne peut pas être null"
            );
        }

        String key = String.valueOf(event.getCollectionId());

        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Erreur lors de la publication de l'événement Kafka pour la collecte {}",
                                event.getCollectionId(),
                                exception
                        );
                    } else {
                        log.info(
                                "Événement Kafka publié : collectionId={}, status={}, topic={}, partition={}, offset={}",
                                event.getCollectionId(),
                                event.getStatus(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}

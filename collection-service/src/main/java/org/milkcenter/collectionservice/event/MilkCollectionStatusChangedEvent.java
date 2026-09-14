package org.milkcenter.collectionservice.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilkCollectionStatusChangedEvent {

    private UUID eventId;

    private Long collectionId;

    private Long farmerId;

    private Long driverUserId;

    private Long routeStopId;

    /**
     * Valeurs attendues : ACCEPTED, CORRECTED ou REJECTED.
     * String est utilisé pour garder le contrat compatible entre les services.
     */
    private String status;

    private BigDecimal quantityLiters;

    private Date collectedAt;

    private LocalDateTime validatedAt;

    private Long validatorUserId;

    private String validationNotes;
}

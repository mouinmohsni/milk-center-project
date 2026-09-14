package org.milkcenter.invoicingservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MilkCollectionStatusChangedEvent {

    private String eventId;
    private Long collectionId;
    private Long farmerId;
    private Long driverUserId;
    private Long routeStopId;
    private String status;
    private BigDecimal quantityLiters;
    private Date collectedAt;
    private LocalDateTime validatedAt;
    private Long validatorUserId;
    private String validationNotes;
}
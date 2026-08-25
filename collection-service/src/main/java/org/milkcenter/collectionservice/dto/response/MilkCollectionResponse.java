package org.milkcenter.collectionservice.dto.response;

import lombok.*;
import org.milkcenter.collectionservice.enums.CollectionStatus;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkCollectionResponse {

    private Long id;
    private Long farmerId;
    private Long driverUserId;
    private Long routeStopId;
    private Date collectedAt;
    private BigDecimal quantityLiters;
    private CollectionStatus status;
    private String notes;
    private int correctionCount;
    private Long updatedByUserId;
    private Date createdAt;
    private Date updatedAt;
}

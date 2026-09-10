package org.milkcenter.invoicingservice.dto.response.client;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MilkCollectionClientResponse {

    private Long id;
    private Long farmerId;
    private Long driverUserId;
    private Long routeStopId;
    private Date collectedAt;
    private BigDecimal quantityLiters;
    private BigDecimal temperatureCelsius;
    private String qualityNotes;
    private String status;
    private String notes;
}

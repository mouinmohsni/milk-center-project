package org.milkcenter.invoicingservice.dto.response.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyMilkTotalClientResponse {

    private Long farmerId;
    private Integer month;
    private Integer year;
    private String status;
    private BigDecimal totalQuantityLiters;
}

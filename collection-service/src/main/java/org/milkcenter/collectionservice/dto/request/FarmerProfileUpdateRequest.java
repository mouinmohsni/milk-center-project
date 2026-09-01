package org.milkcenter.collectionservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerProfileUpdateRequest {

    @Size(max = 100)
    private String farmName;

    @Size(max = 200)
    private String address;

    private Double latitude;
    private Double longitude;

    @Min(0)
    private Integer herdSize;
}

package org.milkcenter.collectionservice.dto.response;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerProfileResponse {

    private Long id;
    private Long userId;
    private String farmName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer herdSize;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;
}

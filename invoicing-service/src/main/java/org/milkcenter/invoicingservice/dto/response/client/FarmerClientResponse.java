package org.milkcenter.invoicingservice.dto.response.client;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FarmerClientResponse {
    private Long id;
    private Long userId;
    private String farmName;
    private String address;
    private Boolean active;
}
package org.milkcenter.collectionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.milkcenter.collectionservice.enums.CollectionStatus;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyMilkTotalResponse {

    /** Identifiant local du FarmerProfile. */
    private Long farmerId;

    /** Mois concerné, compris entre 1 et 12. */
    private Integer month;

    /** Année concernée. */
    private Integer year;

    /** Statut utilisé pour le calcul, toujours ACCEPTED pour la facturation. */
    @Builder.Default
    private CollectionStatus status = CollectionStatus.ACCEPTED;

    /** Quantité totale de lait acceptée pendant la période, en litres. */
    private BigDecimal totalQuantityLiters;
}

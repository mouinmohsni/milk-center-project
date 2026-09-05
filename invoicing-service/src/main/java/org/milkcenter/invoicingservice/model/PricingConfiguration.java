package org.milkcenter.invoicingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Type de facturation : achat de lait ou vente d’aliment. */
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    private InvoiceType invoiceType;

    /** Nom du produit, par exemple Lait cru ou Aliment vaches laitières. */
    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    /** Unité commerciale du prix : litre, kilogramme ou sac. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_unit", nullable = false, length = 20)
    private SaleUnit saleUnit;

    /**
     * Poids du conditionnement en kilogrammes.
     * Obligatoire pour un sac et null pour le lait vendu au litre.
     */
    @Column(name = "package_weight_kg", precision = 10, scale = 3)
    private BigDecimal packageWeightKg;

    /** Prix correspondant à une unité commerciale. */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 3)
    private BigDecimal unitPrice;

    /** Taux de taxe en pourcentage, par exemple 19.00. */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** Date à partir de laquelle cette configuration est applicable. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Permet de désactiver un tarif sans le supprimer. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Indicateur de suppression logique. */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /** Date de suppression logique. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

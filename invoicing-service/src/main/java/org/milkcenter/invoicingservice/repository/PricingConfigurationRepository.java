package org.milkcenter.invoicingservice.repository;

import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;
import org.milkcenter.invoicingservice.model.PricingConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


@Repository
public interface PricingConfigurationRepository
        extends JpaRepository<PricingConfiguration, Long> {

    /**
     * Recherche les configurations non supprimées, triées de la plus récente
     * à la plus ancienne.
     */
    List<PricingConfiguration> findByDeletedFalseOrderByEffectiveFromDesc();

    /**
     * Recherche une configuration non supprimée par son identifiant.
     */
    Optional<PricingConfiguration> findByIdAndDeletedFalse(Long id);



    /**
     * Recherche le tarif actif d’un produit donné à une date donnée.
     * Le poids du conditionnement permet de distinguer un sac de 25 kg
     * d’un sac de 50 kg.
     */
    Optional<PricingConfiguration>
    findFirstByInvoiceTypeAndProductNameAndSaleUnitAndPackageWeightKgAndActiveTrueAndDeletedFalseAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            InvoiceType invoiceType,
            String productName,
            SaleUnit saleUnit,
            BigDecimal packageWeightKg,
            LocalDate date
    );

    /**
     * Vérifie si une configuration identique existe déjà pour la même date.
     */
    boolean existsByInvoiceTypeAndProductNameAndSaleUnitAndPackageWeightKgAndEffectiveFromAndDeletedFalse(
            InvoiceType invoiceType,
            String productName,
            SaleUnit saleUnit,
            BigDecimal packageWeightKg,
            LocalDate effectiveFrom
    );



    /**
     * Recherche toutes les configurations d’un type donné qui ne sont pas
     * supprimées logiquement.
     */
    List<PricingConfiguration> findByInvoiceTypeAndDeletedFalseOrderByProductNameAscEffectiveFromDesc(
            InvoiceType invoiceType
    );
}

package org.milkcenter.invoicingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.dto.request.PricingConfigurationCreateRequest;
import org.milkcenter.invoicingservice.dto.request.PricingConfigurationPatchRequest;
import org.milkcenter.invoicingservice.dto.response.PricingConfigurationResponse;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.service.PricingConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-configurations" )
@RequiredArgsConstructor
public class PricingConfigurationController {

    private final PricingConfigurationService pricingConfigurationService;

    /**
     * Crée une configuration tarifaire.
     * MANAGER uniquement, vérifié dans le service.
     */
    @PostMapping
    public ResponseEntity<PricingConfigurationResponse> create(
            @Valid @RequestBody PricingConfigurationCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pricingConfigurationService.create(request));
    }

    /**
     * Retourne toutes les configurations non supprimées logiquement.
     */
    @GetMapping
    public ResponseEntity<List<PricingConfigurationResponse>> getAll() {
        return ResponseEntity.ok(
                pricingConfigurationService.getAll()
        );
    }

    /**
     * Retourne une configuration par son identifiant.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PricingConfigurationResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                pricingConfigurationService.getById(id)
        );
    }

    /**
     * Retourne les configurations non supprimées d’un type de facture.
     * Exemple : /api/pricing-configurations/type/MILK_PURCHASE
     */
    @GetMapping("/type/{invoiceType}")
    public ResponseEntity<List<PricingConfigurationResponse>> getByInvoiceType(
            @PathVariable InvoiceType invoiceType
    ) {
        return ResponseEntity.ok(
                pricingConfigurationService.getByInvoiceType(invoiceType)
        );
    }

    /**
     * Modification partielle d’une configuration.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PricingConfigurationResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody PricingConfigurationPatchRequest request
    ) {
        return ResponseEntity.ok(
                pricingConfigurationService.patch(id, request)
        );
    }

    /**
     * Suppression logique : la configuration reste dans la base,
     * mais elle n’est plus utilisée pour les nouvelles factures.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<PricingConfigurationResponse> softDelete(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                pricingConfigurationService.softDelete(id)
        );
    }

    /**
     * Suppression physique définitive.
     * L’utilisation de hard=true doit rester réservée au MANAGER.
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(
            @PathVariable Long id
    ) {
        pricingConfigurationService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}

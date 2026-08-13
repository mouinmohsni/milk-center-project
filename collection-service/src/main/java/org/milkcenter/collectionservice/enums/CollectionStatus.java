package org.milkcenter.collectionservice.enums;

public enum CollectionStatus {
    PENDING,
    ACCEPTED,     // Collecte validée, intégrée au calcul financier
    REJECTED,     // Collecte refusée (qualité non conforme, problème de mesure)
    CORRECTED,    // Quantité ou statut modifié après enregistrement initial
    CANCELLED     // Collecte annulée (erreur de saisie, tournée annulée)
}

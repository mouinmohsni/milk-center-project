package org.milkcenter.collectionservice.dto.response;

import org.milkcenter.collectionservice.dto.response.MilkCollectionResponse;

public record CollectionCreationResult(
        MilkCollectionResponse response,
        boolean replayed
) {
}
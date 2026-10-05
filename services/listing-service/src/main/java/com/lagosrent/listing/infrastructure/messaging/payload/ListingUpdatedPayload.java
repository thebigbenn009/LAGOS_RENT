package com.lagosrent.listing.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ListingUpdatedPayload(
        UUID listingId,
        String title,
        String description,
        BigDecimal rentAmount,
        BigDecimal moveInTotal,
        LocalDate availableFrom,
        List<ListingPublishedPayload.FeeItemPayload> fees,
        List<ListingPublishedPayload.MediaItemPayload> media
) {
}

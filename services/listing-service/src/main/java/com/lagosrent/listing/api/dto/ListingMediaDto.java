package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.ListingMedia;
import com.lagosrent.listing.domain.model.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ListingMediaDto(
        UUID id,

        @NotNull
        MediaType mediaType,

        @NotBlank
        String s3Key,

        @NotBlank
        String filename,

        Integer orderIndex,
        String caption,
        boolean isPrimary
) {
    public static ListingMediaDto from(ListingMedia media) {
        return new ListingMediaDto(
                media.getId(),
                media.getMediaType(),
                media.getS3Key(),
                media.getFilename(),
                media.getOrderIndex(),
                media.getCaption(),
                media.isPrimary()

        );
    }
}

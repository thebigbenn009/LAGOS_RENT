package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.ListingMedia;
import com.lagosrent.listing.domain.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "ListingMediaDto", description = "Media asset attached to a listing (photo, floor plan, video tour)")
public record ListingMediaDto(
        @Schema(description = "Media asset ID", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID id,

        @Schema(description = "Type of media", example = "IMAGE")
        @NotNull
        MediaType mediaType,

        @Schema(description = "S3 object storage key", example = "listings/3fa85f64-5717-4562-b3fc-2c963f66afa6/living-room.jpg")
        @NotBlank
        String s3Key,

        @Schema(description = "Original uploaded filename", example = "living-room.jpg")
        @NotBlank
        String filename,

        @Schema(description = "Display display ordering index", example = "1")
        Integer orderIndex,

        @Schema(description = "Media caption or description", example = "Spacious living room with marble floors")
        String caption,

        @Schema(description = "Whether this is the primary cover image for the listing", example = "true")
        Boolean isPrimary
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

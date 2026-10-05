package com.lagosrent.listing.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "CreatePropertyRequest", description = "Payload for registering a new property building under landlord ownership")
public record CreatePropertyRequest(
        @Schema(description = "Property building or compound title", example = "Silvercrest Court")
        @NotBlank(message = "Property title is required")
        @Size(max = 255)
        String title,

        @Schema(description = "Lagos Local Government Area", example = "Eti-Osa")
        @NotBlank(message = "LGA is required")
        String lga,

        @Schema(description = "Neighborhood or specific district", example = "Lekki Phase 1")
        @NotBlank(message = "Area/Neighborhood is required")
        String area,

        @Schema(description = "Primary street address (private, visible only after application/inspection approval)", example = "14 Admiralty Way")
        @NotBlank(message = "Address Line 1 is required")
        String addressLine1,

        @Schema(description = "Secondary street address details", example = "Opposite Lekki Coliseum")
        String addressLine2,

        @Schema(description = "Privacy-safe public landmark for listing search", example = "Near Admiralty Toll Gate")
        @NotBlank(message = "Landmark is required for privacy-safe public search")
        String landmark,

        @Schema(description = "Property geographical latitude", example = "6.4474")
        BigDecimal latitude,

        @Schema(description = "Property geographical longitude", example = "3.4723")
        BigDecimal longitude
) {
}

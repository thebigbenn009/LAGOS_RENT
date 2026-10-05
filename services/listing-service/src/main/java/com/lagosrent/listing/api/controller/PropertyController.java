package com.lagosrent.listing.api.controller;

import com.lagosrent.listing.api.dto.CreatePropertyRequest;
import com.lagosrent.listing.api.dto.CreateRentalUnitRequest;
import com.lagosrent.listing.api.dto.PropertyResponse;
import com.lagosrent.listing.api.dto.RentalUnitResponse;
import com.lagosrent.listing.api.error.ErrorResponse;
import com.lagosrent.listing.application.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Properties", description = "Landlord property and rental unit management")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
public class PropertyController {
    private final PropertyService propertyService;

    @Operation(
            summary = "Create Property",
            description = "Registers a new physical property or compound under the authenticated landlord's account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Property created successfully",
                    content = @Content(schema = @Schema(implementation = PropertyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<PropertyResponse> createProperty(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePropertyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.createProperty(jwt.getSubject(), request));
    }

    @Operation(
            summary = "Get Landlord Properties",
            description = "Retrieves all properties owned by the authenticated landlord."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Properties retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<PropertyResponse>> getLandlordProperties(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(propertyService.getLandlordProperties(jwt.getSubject()));
    }

    @Operation(
            summary = "Get Property by ID",
            description = "Retrieves details of a specific property by its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Property found",
                    content = @Content(schema = @Schema(implementation = PropertyResponse.class))),
            @ApiResponse(responseCode = "404", description = "Property not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{propertyId}")
    public ResponseEntity<PropertyResponse> getPropertyById(
            @Parameter(description = "Property unique ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @PathVariable UUID propertyId
    ) {
        return ResponseEntity.ok(propertyService.getPropertyById(propertyId));
    }

    @Operation(
            summary = "Add Rental Unit to Property",
            description = "Adds a specific rental unit (flat, maisonette, penthouse) to an existing property."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rental unit created successfully",
                    content = @Content(schema = @Schema(implementation = RentalUnitResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — User does not own this property",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Property not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{propertyId}/units")
    public ResponseEntity<RentalUnitResponse> addRentalUnit(
            @Parameter(description = "Property unique ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @PathVariable UUID propertyId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRentalUnitRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.addRentalUnit(propertyId, jwt.getSubject(), request));
    }

    @Operation(
            summary = "Get Property Rental Units",
            description = "Lists all rental units associated with a specific property."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rental units retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Property not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{propertyId}/units")
    public ResponseEntity<List<RentalUnitResponse>> getPropertyUnits(
            @Parameter(description = "Property unique ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @PathVariable UUID propertyId
    ) {
        return ResponseEntity.ok(propertyService.getPropertyUnits(propertyId));
    }
}

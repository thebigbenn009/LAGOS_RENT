package com.lagosrent.search.infrastructure.persistence;

import com.lagosrent.search.domain.model.SearchListing;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SearchSpecification {

    public static Specification<SearchListing> filter(
            String area,
            String lga,
            String unitType,
            BigDecimal minRent,
            BigDecimal maxRent,
            Integer bedrooms,
            String status
    ) {
        return ((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (area != null && !area.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("area")), area.trim().toLowerCase()));
            }

            if (lga != null && !lga.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("lga")), lga.trim().toLowerCase()));
            }

            if (unitType != null && !unitType.isBlank()) {
                predicates.add(cb.equal(root.get("unitType"), unitType.trim()));
            }

            if (minRent != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rentAmount"), minRent));
            }

            if (maxRent != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rentAmount"), maxRent));
            }

            if (bedrooms != null) {
                predicates.add(cb.equal(root.get("bedrooms"), bedrooms));
            }

            // Default to only PUBLISHED listings if not specified
            String finalStatus = (status != null && !status.isBlank()) ? status.trim() : "PUBLISHED";
            predicates.add(cb.equal(root.get("status"), finalStatus));

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}

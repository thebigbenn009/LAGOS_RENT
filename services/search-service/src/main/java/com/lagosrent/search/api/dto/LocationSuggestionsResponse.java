package com.lagosrent.search.api.dto;

import java.util.List;

public record LocationSuggestionsResponse(
        List<String> lgas,
        List<String> areas
) {}

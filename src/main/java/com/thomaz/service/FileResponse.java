package com.thomaz.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileResponse(
        @JsonProperty("_id")
        String id,
        String hash,
        String contentType
) {}

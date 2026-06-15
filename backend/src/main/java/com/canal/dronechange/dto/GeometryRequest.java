package com.canal.dronechange.dto;

import jakarta.validation.constraints.NotBlank;

public record GeometryRequest(@NotBlank String geometryGeoJson) {
}

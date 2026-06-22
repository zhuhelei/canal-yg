package com.canal.dronechange.dto;

import java.math.BigDecimal;

public record PlotInfoRequest(
        String regionName,
        BigDecimal longitude,
        BigDecimal latitude,
        String changeImageUrl
) {
}

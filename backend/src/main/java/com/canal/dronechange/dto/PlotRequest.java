package com.canal.dronechange.dto;

import java.math.BigDecimal;

public record PlotRequest(
    Long imageId,
    String plotName,
    BigDecimal area,
    String landType,
    String geometry,
    BigDecimal centerLongitude,
    BigDecimal centerLatitude,
    String remark
) {
}

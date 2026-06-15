package com.canal.dronechange.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeoCoordinateService {
    private static final Pattern LON_LAT_PATTERN = Pattern.compile(
            "(?<!\\d)(-?\\d{2,3}\\.\\d+)[,，_\\s]+(-?\\d{1,2}\\.\\d+)(?!\\d)"
    );

    public GeoPoint calculateCenter(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return new GeoPoint(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return new GeoPoint(longitude, latitude);
    }

    public String resolveRegion(BigDecimal longitude, BigDecimal latitude, String fallbackRegion) {
        if (fallbackRegion != null && !fallbackRegion.isBlank()) {
            return fallbackRegion;
        }
        if (longitude == null || latitude == null) {
            return "未识别区域";
        }
        return "默认巡查区";
    }

    public String resolveRegionFromZipPath(String zipPath) {
        String normalizedPath = zipPath.replace('\\', '/');
        String[] parts = normalizedPath.split("/");
        for (String part : parts) {
            if (!part.isBlank() && !part.contains(".")) {
                return part;
            }
        }
        if (parts.length > 0) {
            String fileName = parts[parts.length - 1];
            int dotIndex = fileName.lastIndexOf('.');
            return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        }
        return "未识别区域";
    }

    public Optional<GeoPoint> parseCenterFromZipPath(String zipPath) {
        Matcher matcher = LON_LAT_PATTERN.matcher(zipPath);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new GeoPoint(new BigDecimal(matcher.group(1)), new BigDecimal(matcher.group(2))));
    }

    public record GeoPoint(BigDecimal longitude, BigDecimal latitude) {
    }
}

package com.canal.dronechange.controller;

import com.canal.dronechange.dto.GeometryRequest;
import com.canal.dronechange.entity.DroneChangeImage;
import com.canal.dronechange.service.DroneChangeImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/drone-change")
public class DroneChangeImageController {
    private final DroneChangeImageService service;

    public DroneChangeImageController(DroneChangeImageService service) {
        this.service = service;
    }

    @GetMapping
    public List<DroneChangeImage> list(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String regionName,
            @RequestParam(required = false) String status
    ) {
        return service.list(month, regionName, status);
    }

    @GetMapping("/{id}")
    public DroneChangeImage detail(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DroneChangeImage upload(
            @RequestParam(required = false) Long projectId,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式应为 yyyy-MM") String month,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) String regionName,
            @RequestPart(required = false) MultipartFile previousImage,
            @RequestPart(required = false) MultipartFile currentImage,
            @RequestPart MultipartFile changeImage
    ) throws IOException {
        return service.create(projectId, month, longitude, latitude, regionName, previousImage, currentImage, changeImage);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<DroneChangeImage> importZip(
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式应为 yyyy-MM") String month,
            @RequestPart MultipartFile zipFile
    ) throws IOException {
        return service.importZip(month, zipFile);
    }

    @PutMapping("/{id}/geometry")
    public DroneChangeImage updateGeometry(@PathVariable Long id, @Valid @RequestBody GeometryRequest request) {
        return service.updateGeometry(id, request.geometryGeoJson());
    }

    @PutMapping("/{id}/approve")
    public DroneChangeImage approve(@PathVariable Long id) {
        return service.approve(id);
    }
}

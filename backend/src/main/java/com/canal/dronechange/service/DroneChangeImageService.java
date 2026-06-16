package com.canal.dronechange.service;

import com.canal.dronechange.entity.DroneChangeImage;
import com.canal.dronechange.repository.DroneChangeImageRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DroneChangeImageService {
    private final DroneChangeImageRepository repository;
    private final FileStorageService fileStorageService;
    private final GeoCoordinateService geoCoordinateService;

    public DroneChangeImageService(
            DroneChangeImageRepository repository,
            FileStorageService fileStorageService,
            GeoCoordinateService geoCoordinateService
    ) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.geoCoordinateService = geoCoordinateService;
    }

    public List<DroneChangeImage> list(String month, String regionName, String status) {
        boolean hasMonth = month != null && !month.isBlank();
        boolean hasRegion = regionName != null && !regionName.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        if (hasMonth && hasRegion && hasStatus) {
            return repository.findByMonthAndRegionNameContainingAndStatusOrderByCreatedAtDesc(month, regionName, status);
        }
        if (hasMonth && hasRegion) {
            return repository.findByMonthAndRegionNameContainingOrderByCreatedAtDesc(month, regionName);
        }
        if (hasMonth && hasStatus) {
            return repository.findByMonthAndStatusOrderByCreatedAtDesc(month, status);
        }
        if (hasRegion && hasStatus) {
            return repository.findByRegionNameContainingAndStatusOrderByCreatedAtDesc(regionName, status);
        }
        if (hasMonth) {
            return repository.findByMonthOrderByCreatedAtDesc(month);
        }
        if (hasRegion) {
            return repository.findByRegionNameContainingOrderByCreatedAtDesc(regionName);
        }
        if (hasStatus) {
            return repository.findByStatusOrderByCreatedAtDesc(status);
        }
        return repository.findAll();
    }

    public DroneChangeImage get(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Change image not found: " + id));
    }

    @Transactional
    public DroneChangeImage create(
            Long projectId,
            String month,
            BigDecimal longitude,
            BigDecimal latitude,
            String regionName,
            MultipartFile previousImage,
            MultipartFile currentImage,
            MultipartFile changeImage
    ) throws IOException {
        if (changeImage == null || changeImage.isEmpty()) {
            throw new IllegalArgumentException("Change image is required");
        }

        GeoCoordinateService.GeoPoint geoPoint = geoCoordinateService.calculateCenter(longitude, latitude);
        DroneChangeImage image = new DroneChangeImage();
        image.setProjectId(projectId);
        image.setMonth(month);
        image.setLongitude(geoPoint.longitude());
        image.setLatitude(geoPoint.latitude());
        image.setRegionName(geoCoordinateService.resolveRegion(geoPoint.longitude(), geoPoint.latitude(), regionName));
        image.setPreviousImageUrl(fileStorageService.store(previousImage, month));
        image.setCurrentImageUrl(fileStorageService.store(currentImage, month));
        image.setChangeImageUrl(fileStorageService.store(changeImage, month));
        image.setStatus("待核查");
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        return repository.save(image);
    }

    @Transactional
    public List<DroneChangeImage> importZip(String month, MultipartFile zipFile) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Zip file is required");
        }

        List<DroneChangeImage> importedImages = new ArrayList<>();
        try (InputStream inputStream = zipFile.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !fileStorageService.isImagePath(entry.getName())) {
                    zipInputStream.closeEntry();
                    continue;
                }

                byte[] bytes = readEntryBytes(zipInputStream);
                String changeImageUrl = fileStorageService.storeBytes(bytes, month, entry.getName());
                Optional<GeoCoordinateService.GeoPoint> center = geoCoordinateService.parseCenterFromZipPath(entry.getName());
                BigDecimal longitude = center.map(GeoCoordinateService.GeoPoint::longitude).orElse(BigDecimal.ZERO);
                BigDecimal latitude = center.map(GeoCoordinateService.GeoPoint::latitude).orElse(BigDecimal.ZERO);

                DroneChangeImage image = new DroneChangeImage();
                image.setMonth(month);
                image.setLongitude(longitude);
                image.setLatitude(latitude);
                image.setRegionName(geoCoordinateService.resolveRegionFromZipPath(entry.getName()));
                image.setChangeImageUrl(changeImageUrl);
                image.setStatus("待核查");
                image.setCreatedAt(LocalDateTime.now());
                image.setUpdatedAt(LocalDateTime.now());
                importedImages.add(repository.save(image));
                zipInputStream.closeEntry();
            }
        }
        return importedImages;
    }

    @Transactional
    public DroneChangeImage updateGeometry(Long id, String geometryGeoJson) {
        DroneChangeImage image = get(id);
        image.setGeometryGeoJson(geometryGeoJson);
        image.setStatus("有变化");
        image.setUpdatedAt(LocalDateTime.now());
        return repository.save(image);
    }

    @Transactional
    public DroneChangeImage approve(Long id) {
        DroneChangeImage image = get(id);
        image.setStatus("有变化");
        image.setUpdatedAt(LocalDateTime.now());
        return repository.save(image);
    }

    @Transactional
    public DroneChangeImage markNoChange(Long id) {
        DroneChangeImage image = get(id);
        image.setGeometryGeoJson(null);
        image.setStatus("无变化");
        image.setUpdatedAt(LocalDateTime.now());
        return repository.save(image);
    }

    private byte[] readEntryBytes(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = zipInputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }
}

package com.canal.dronechange.service;

import com.canal.dronechange.entity.DroneChangeImage;
import com.canal.dronechange.dto.PlotInfoRequest;
import com.canal.dronechange.repository.DroneChangeImageRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        image.setChangeImageUrl(fileStorageService.store(changeImage, month));
        image.setStatus("待核查");
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        return repository.save(image);
    }

    @Transactional
    public List<DroneChangeImage> importFile(String month, MultipartFile zipFile) throws IOException {
        String fileName = zipFile.getOriginalFilename() == null ? "" : zipFile.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            return importExcel(month, zipFile);
        }
        return importZip(month, zipFile);
    }

    @Transactional
    public List<DroneChangeImage> importZip(String month, MultipartFile zipFile) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Import file is required");
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
    public List<DroneChangeImage> importExcel(String month, MultipartFile excelFile) throws IOException {
        if (excelFile == null || excelFile.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        List<DroneChangeImage> importedImages = new ArrayList<>();
        try (InputStream inputStream = excelFile.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) {
                return importedImages;
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> headers = readHeaderMap(sheet.getRow(sheet.getFirstRowNum()), formatter);
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                DroneChangeImage image = new DroneChangeImage();
                image.setMonth(readCell(row, headers, formatter, "month", month));
                image.setLongitude(readDecimal(row, headers, formatter, "longitude", BigDecimal.ZERO));
                image.setLatitude(readDecimal(row, headers, formatter, "latitude", BigDecimal.ZERO));
                image.setRegionName(readCell(row, headers, formatter, "regionName", "未命名区域"));
                image.setChangeImageUrl(readCell(row, headers, formatter, "changeImageUrl", ""));
                image.setStatus("待核查");
                image.setCreatedAt(LocalDateTime.now());
                image.setUpdatedAt(LocalDateTime.now());
                importedImages.add(repository.save(image));
            }
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to import Excel file", exception);
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
    public DroneChangeImage updatePlotInfo(Long id, PlotInfoRequest request) {
        DroneChangeImage image = get(id);
        if (request.regionName() != null) {
            image.setRegionName(request.regionName());
        }
        if (request.longitude() != null) {
            image.setLongitude(request.longitude());
        }
        if (request.latitude() != null) {
            image.setLatitude(request.latitude());
        }
        if (request.changeImageUrl() != null) {
            image.setChangeImageUrl(request.changeImageUrl());
        }
        image.setStatus("有变化");
        image.setUpdatedAt(LocalDateTime.now());
        return repository.save(image);
    }

    @Transactional
    public DroneChangeImage deleteGeometry(Long id) {
        DroneChangeImage image = get(id);
        image.setGeometryGeoJson(null);
        image.setStatus("待核查");
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

    private Map<String, Integer> readHeaderMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headers = new HashMap<>();
        if (headerRow == null) {
            return headers;
        }
        for (int cellIndex = headerRow.getFirstCellNum(); cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            String header = normalizeHeader(formatter.formatCellValue(headerRow.getCell(cellIndex)));
            if (!header.isBlank()) {
                headers.put(header, cellIndex);
            }
        }
        return headers;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            if (!formatter.formatCellValue(row.getCell(cellIndex)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row, Map<String, Integer> headers, DataFormatter formatter, String field, String defaultValue) {
        for (String alias : aliases(field)) {
            Integer cellIndex = headers.get(normalizeHeader(alias));
            if (cellIndex == null) {
                continue;
            }
            String value = formatter.formatCellValue(row.getCell(cellIndex)).trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
    }

    private BigDecimal readDecimal(Row row, Map<String, Integer> headers, DataFormatter formatter, String field, BigDecimal defaultValue) {
        String value = readCell(row, headers, formatter, field, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace(" ", "").replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private List<String> aliases(String field) {
        return switch (field) {
            case "month" -> List.of("月份", "month", "imageMonth", "影像月份");
            case "regionName" -> List.of("所属区域", "区域", "区县", "县市区名称", "regionName", "region");
            case "longitude" -> List.of("中心经度", "经度", "longitude", "lng", "lon");
            case "latitude" -> List.of("中心纬度", "纬度", "latitude", "lat");
            case "changeImageUrl" -> List.of("变化影像", "变化图片", "影像路径", "changeImageUrl", "changeImage", "imageUrl");
            default -> List.of(field);
        };
    }
}

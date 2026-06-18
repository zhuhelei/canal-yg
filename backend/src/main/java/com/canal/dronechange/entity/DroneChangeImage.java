package com.canal.dronechange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "drone_change_images")
@SequenceGenerator(name = "drone_change_images_seq", sequenceName = "SEQ_DRONE_CHANGE_IMAGES", allocationSize = 1)
public class DroneChangeImage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "drone_change_images_seq")
    private Long id;

    private Long projectId;

    @Column(name = "image_month", length = 7, nullable = false)
    private String month;

    @Column(length = 500)
    private String previousImageUrl;

    @Column(length = 500)
    private String currentImageUrl;

    @Column(length = 500, nullable = false)
    private String changeImageUrl;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(length = 120)
    private String regionName;

    @Lob
    private String geometryGeoJson;

    @Column(length = 40)
    private String status = "待核查";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getPreviousImageUrl() {
        return previousImageUrl;
    }

    public void setPreviousImageUrl(String previousImageUrl) {
        this.previousImageUrl = previousImageUrl;
    }

    public String getCurrentImageUrl() {
        return currentImageUrl;
    }

    public void setCurrentImageUrl(String currentImageUrl) {
        this.currentImageUrl = currentImageUrl;
    }

    public String getChangeImageUrl() {
        return changeImageUrl;
    }

    public void setChangeImageUrl(String changeImageUrl) {
        this.changeImageUrl = changeImageUrl;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getGeometryGeoJson() {
        return geometryGeoJson;
    }

    public void setGeometryGeoJson(String geometryGeoJson) {
        this.geometryGeoJson = geometryGeoJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

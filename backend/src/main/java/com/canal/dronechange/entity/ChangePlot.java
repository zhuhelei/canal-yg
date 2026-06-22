package com.canal.dronechange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "change_plots")
@SequenceGenerator(name = "change_plots_seq", sequenceName = "SEQ_CHANGE_PLOTS", allocationSize = 1)
public class ChangePlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "change_plots_seq")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private DroneChangeImage image;
    
    @Column(length = 100)
    private String plotName;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal area;
    
    @Column(length = 50)
    private String landType;
    
    @Lob
    @Column(columnDefinition = "CLOB")
    private String geometry;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal centerLongitude;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal centerLatitude;
    
    @Column(length = 200)
    private String remark;
    
    @Column(length = 40)
    private String status = "待核查";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public DroneChangeImage getImage() {
        return image;
    }
    
    public void setImage(DroneChangeImage image) {
        this.image = image;
    }
    
    public String getPlotName() {
        return plotName;
    }
    
    public void setPlotName(String plotName) {
        this.plotName = plotName;
    }
    
    public BigDecimal getArea() {
        return area;
    }
    
    public void setArea(BigDecimal area) {
        this.area = area;
    }
    
    public String getLandType() {
        return landType;
    }
    
    public void setLandType(String landType) {
        this.landType = landType;
    }
    
    public String getGeometry() {
        return geometry;
    }
    
    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }
    
    public BigDecimal getCenterLongitude() {
        return centerLongitude;
    }
    
    public void setCenterLongitude(BigDecimal centerLongitude) {
        this.centerLongitude = centerLongitude;
    }
    
    public BigDecimal getCenterLatitude() {
        return centerLatitude;
    }
    
    public void setCenterLatitude(BigDecimal centerLatitude) {
        this.centerLatitude = centerLatitude;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
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

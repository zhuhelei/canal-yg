package com.canal.dronechange.service;

import com.canal.dronechange.dto.PlotRequest;
import com.canal.dronechange.entity.ChangePlot;
import com.canal.dronechange.entity.DroneChangeImage;
import com.canal.dronechange.repository.ChangePlotRepository;
import com.canal.dronechange.repository.DroneChangeImageRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChangePlotService {
    
    private final ChangePlotRepository plotRepository;
    private final DroneChangeImageRepository imageRepository;
    
    public ChangePlotService(ChangePlotRepository plotRepository, DroneChangeImageRepository imageRepository) {
        this.plotRepository = plotRepository;
        this.imageRepository = imageRepository;
    }
    
    public List<ChangePlot> list() {
        return plotRepository.findAll();
    }
    
    public List<ChangePlot> listByImage(Long imageId) {
        return plotRepository.findByImageIdOrderByCreatedAtDesc(imageId);
    }
    
    public ChangePlot get(Long id) {
        return plotRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Plot not found: " + id));
    }
    
    @Transactional
    public ChangePlot create(PlotRequest request) {
        DroneChangeImage image = imageRepository.findById(request.imageId())
            .orElseThrow(() -> new EntityNotFoundException("Image not found: " + request.imageId()));
        
        ChangePlot plot = new ChangePlot();
        plot.setImage(image);
        plot.setPlotName(request.plotName());
        plot.setArea(request.area());
        plot.setLandType(request.landType());
        plot.setGeometry(request.geometry());
        plot.setCenterLongitude(request.centerLongitude());
        plot.setCenterLatitude(request.centerLatitude());
        plot.setRemark(request.remark());
        plot.setStatus("待核查");
        plot.setCreatedAt(LocalDateTime.now());
        plot.setUpdatedAt(LocalDateTime.now());
        
        return plotRepository.save(plot);
    }
    
    @Transactional
    public ChangePlot update(Long id, PlotRequest request) {
        ChangePlot plot = get(id);
        
        if (request.plotName() != null) {
            plot.setPlotName(request.plotName());
        }
        if (request.area() != null) {
            plot.setArea(request.area());
        }
        if (request.landType() != null) {
            plot.setLandType(request.landType());
        }
        if (request.geometry() != null) {
            plot.setGeometry(request.geometry());
        }
        if (request.centerLongitude() != null) {
            plot.setCenterLongitude(request.centerLongitude());
        }
        if (request.centerLatitude() != null) {
            plot.setCenterLatitude(request.centerLatitude());
        }
        if (request.remark() != null) {
            plot.setRemark(request.remark());
        }
        
        plot.setUpdatedAt(LocalDateTime.now());
        return plotRepository.save(plot);
    }
    
    @Transactional
    public void delete(Long id) {
        ChangePlot plot = get(id);
        plotRepository.delete(plot);
    }
    
    @Transactional
    public ChangePlot approve(Long id) {
        ChangePlot plot = get(id);
        plot.setStatus("已确认");
        plot.setUpdatedAt(LocalDateTime.now());
        return plotRepository.save(plot);
    }
    
    @Transactional
    public ChangePlot markInvalid(Long id) {
        ChangePlot plot = get(id);
        plot.setStatus("无效");
        plot.setUpdatedAt(LocalDateTime.now());
        return plotRepository.save(plot);
    }
}

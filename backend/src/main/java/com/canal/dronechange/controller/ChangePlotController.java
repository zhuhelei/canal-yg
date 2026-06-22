package com.canal.dronechange.controller;

import com.canal.dronechange.dto.PlotRequest;
import com.canal.dronechange.entity.ChangePlot;
import com.canal.dronechange.service.ChangePlotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plots")
@CrossOrigin(origins = "*")
public class ChangePlotController {
    
    private final ChangePlotService plotService;
    
    public ChangePlotController(ChangePlotService plotService) {
        this.plotService = plotService;
    }
    
    @GetMapping
    public List<ChangePlot> list() {
        return plotService.list();
    }
    
    @GetMapping("/image/{imageId}")
    public List<ChangePlot> listByImage(@PathVariable Long imageId) {
        return plotService.listByImage(imageId);
    }
    
    @GetMapping("/{id}")
    public ChangePlot get(@PathVariable Long id) {
        return plotService.get(id);
    }
    
    @PostMapping
    public ChangePlot create(@Valid @RequestBody PlotRequest request) {
        return plotService.create(request);
    }
    
    @PutMapping("/{id}")
    public ChangePlot update(@PathVariable Long id, @Valid @RequestBody PlotRequest request) {
        return plotService.update(id, request);
    }
    
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        plotService.delete(id);
    }
    
    @PutMapping("/{id}/approve")
    public ChangePlot approve(@PathVariable Long id) {
        return plotService.approve(id);
    }
    
    @PutMapping("/{id}/invalid")
    public ChangePlot markInvalid(@PathVariable Long id) {
        return plotService.markInvalid(id);
    }
}

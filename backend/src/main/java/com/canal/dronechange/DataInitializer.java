package com.canal.dronechange;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.canal.dronechange.entity.DroneChangeImage;
import com.canal.dronechange.repository.DroneChangeImageRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private DroneChangeImageRepository repository;
    
    @Override
    public void run(String... args) throws Exception {
        if (repository.count() > 0) {
            System.out.println("数据库已有数据，跳过初始化");
            return;
        }
        
        System.out.println("开始初始化测试数据...");
        
        // 2025-05 数据
        repository.save(createImage(1L, "2026-06", 120.153576, 30.287459, "西湖区", "待核查"));
        repository.save(createImage(1L, "2026-06", 120.161234, 30.279123, "拱墅区", "有变化"));
        repository.save(createImage(1L, "2026-06", 120.178456, 30.291567, "余杭区", "无变化"));
        repository.save(createImage(2L, "2026-06", 120.145678, 30.268234, "滨江区", "待核查"));
        repository.save(createImage(2L, "2026-06", 120.189012, 30.305678, "萧山区", "有变化"));
        
        // 2025-04 数据
        repository.save(createImage(1L, "2026-06", 120.123456, 30.256789, "西湖区", "待核查"));
        repository.save(createImage(3L, "2026-06", 120.134567, 30.267890, "拱墅区", "无变化"));
        repository.save(createImage(3L, "2026-06", 120.156789, 30.278901, "余杭区", "有变化"));
        repository.save(createImage(2L, "2026-06", 120.167890, 30.289012, "滨江区", "待核查"));
        repository.save(createImage(1L, "2026-06", 120.178901, 30.300123, "萧山区", "有变化"));
        
        // 2025-03 数据
        repository.save(createImage(3L, "2026-06", 120.145234, 30.265432, "西湖区", "无变化"));
        repository.save(createImage(2L, "2026-06", 120.156345, 30.276543, "拱墅区", "待核查"));
        repository.save(createImage(1L, "2026-06", 120.167456, 30.287654, "余杭区", "有变化"));
        repository.save(createImage(3L, "2026-06", 120.178567, 30.298765, "滨江区", "无变化"));
        repository.save(createImage(2L, "2026-06", 120.189678, 30.309876, "萧山区", "待核查"));
        
        System.out.println("测试数据初始化完成，共插入 " + repository.count() + " 条记录");
    }
    
    private DroneChangeImage createImage(Long projectId, String month, double lon, double lat, String region, String status) {
        DroneChangeImage image = new DroneChangeImage();
        image.setProjectId(projectId);
        image.setMonth(month);
        image.setChangeImageUrl("/uploads/" + month + "/change.png");
        image.setLongitude(BigDecimal.valueOf(lon));
        image.setLatitude(BigDecimal.valueOf(lat));
        image.setRegionName(region);
        image.setStatus(status);
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        return image;
    }
}

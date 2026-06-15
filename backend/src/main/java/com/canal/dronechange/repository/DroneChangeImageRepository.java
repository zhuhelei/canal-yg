package com.canal.dronechange.repository;

import com.canal.dronechange.entity.DroneChangeImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DroneChangeImageRepository extends JpaRepository<DroneChangeImage, Long> {
    List<DroneChangeImage> findByMonthOrderByCreatedAtDesc(String month);

    List<DroneChangeImage> findByMonthAndRegionNameContainingOrderByCreatedAtDesc(String month, String regionName);

    List<DroneChangeImage> findByMonthAndStatusOrderByCreatedAtDesc(String month, String status);

    List<DroneChangeImage> findByMonthAndRegionNameContainingAndStatusOrderByCreatedAtDesc(String month, String regionName, String status);

    List<DroneChangeImage> findByRegionNameContainingOrderByCreatedAtDesc(String regionName);

    List<DroneChangeImage> findByStatusOrderByCreatedAtDesc(String status);

    List<DroneChangeImage> findByRegionNameContainingAndStatusOrderByCreatedAtDesc(String regionName, String status);
}

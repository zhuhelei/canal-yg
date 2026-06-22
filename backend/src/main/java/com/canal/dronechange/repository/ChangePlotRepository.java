package com.canal.dronechange.repository;

import com.canal.dronechange.entity.ChangePlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangePlotRepository extends JpaRepository<ChangePlot, Long> {
    
    List<ChangePlot> findByImageIdOrderByCreatedAtDesc(Long imageId);
    
    List<ChangePlot> findByLandTypeOrderByCreatedAtDesc(String landType);
    
    List<ChangePlot> findByStatusOrderByCreatedAtDesc(String status);
}

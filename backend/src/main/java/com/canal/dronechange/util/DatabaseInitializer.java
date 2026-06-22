package com.canal.dronechange.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void main(String[] args) {
        String url = "jdbc:dm://111.0.82.17:8890/DEV_DB";
        String user = "DEV_DB";
        String password = "bwt@1qaz!QAZ";
        
        String[] sqls = {
            // 建表
            "CREATE SEQUENCE SEQ_DRONE_CHANGE_IMAGES START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
            "CREATE TABLE drone_change_images (id BIGINT PRIMARY KEY, project_id BIGINT, image_month VARCHAR(7) NOT NULL, previous_image_url VARCHAR(500), current_image_url VARCHAR(500), change_image_url VARCHAR(500) NOT NULL, longitude DECIMAL(10, 7), latitude DECIMAL(10, 7), region_name VARCHAR(120), geometry_geo_json CLOB, status VARCHAR(40) DEFAULT '待核查', created_at TIMESTAMP, updated_at TIMESTAMP)",
            "CREATE INDEX idx_drone_change_month ON drone_change_images(image_month)",
            "CREATE INDEX idx_drone_change_region ON drone_change_images(region_name)",
            "CREATE INDEX idx_drone_change_status ON drone_change_images(status)",
            "CREATE INDEX idx_drone_change_created ON drone_change_images(created_at DESC)",
            // 测试数据
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 1, '2025-05', '/uploads/2025-05/before_001.png', '/uploads/2025-05/after_001.png', '/uploads/2025-05/change_001.png', 120.153576, 30.287459, '西湖区', '待核查', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 1, '2025-05', '/uploads/2025-05/before_002.png', '/uploads/2025-05/after_002.png', '/uploads/2025-05/change_002.png', 120.161234, 30.279123, '拱墅区', '有变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 1, '2025-05', '/uploads/2025-05/before_003.png', '/uploads/2025-05/after_003.png', '/uploads/2025-05/change_003.png', 120.178456, 30.291567, '余杭区', '无变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 2, '2025-05', '/uploads/2025-05/before_004.png', '/uploads/2025-05/after_004.png', '/uploads/2025-05/change_004.png', 120.145678, 30.268234, '滨江区', '待核查', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 2, '2025-05', '/uploads/2025-05/before_005.png', '/uploads/2025-05/after_005.png', '/uploads/2025-05/change_005.png', 120.189012, 30.305678, '萧山区', '有变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 1, '2025-04', '/uploads/2025-04/before_006.png', '/uploads/2025-04/after_006.png', '/uploads/2025-04/change_006.png', 120.123456, 30.256789, '西湖区', '待核查', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 3, '2025-04', '/uploads/2025-04/before_007.png', '/uploads/2025-04/after_007.png', '/uploads/2025-04/change_007.png', 120.134567, 30.267890, '拱墅区', '无变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 3, '2025-04', '/uploads/2025-04/before_008.png', '/uploads/2025-04/after_008.png', '/uploads/2025-04/change_008.png', 120.156789, 30.278901, '余杭区', '有变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 2, '2025-04', '/uploads/2025-04/before_009.png', '/uploads/2025-04/after_009.png', '/uploads/2025-04/change_009.png', 120.167890, 30.289012, '滨江区', '待核查', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 1, '2025-04', '/uploads/2025-04/before_010.png', '/uploads/2025-04/after_010.png', '/uploads/2025-04/change_010.png', 120.178901, 30.300123, '萧山区', '有变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 3, '2025-03', '/uploads/2025-03/before_011.png', '/uploads/2025-03/after_011.png', '/uploads/2025-03/change_011.png', 120.145234, 30.265432, '西湖区', '无变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 2, '2025-03', '/uploads/2025-03/before_012.png', '/uploads/2025-03/after_012.png', '/uploads/2025-03/change_012.png', 120.156345, 30.276543, '拱墅区', '待核查', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 1, '2025-03', '/uploads/2025-03/before_013.png', '/uploads/2025-03/after_013.png', '/uploads/2025-03/change_013.png', 120.167456, 30.287654, '余杭区', '有变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 3, '2025-03', '/uploads/2025-03/before_014.png', '/uploads/2025-03/after_014.png', '/uploads/2025-03/change_014.png', 120.178567, 30.298765, '滨江区', '无变化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "INSERT INTO drone_change_images (id, project_id, image_month, previous_image_url, current_image_url, change_image_url, longitude, latitude, region_name, status, created_at, updated_at) VALUES (SEQ_DRONE_CHANGE_IMAGES.NEXTVAL, 2, '2025-03', '/uploads/2025-03/before_015.png', '/uploads/2025-03/after_015.png', '/uploads/2025-03/change_015.png', 120.189678, 30.309876, '萧山区', '待核查', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            "COMMIT"
        };
        
        try {
            Class.forName("dm.jdbc.driver.DmDriver");
            System.out.println("正在连接达梦数据库...");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("连接成功！");
            Statement stmt = conn.createStatement();
            
            for (int i = 0; i < sqls.length; i++) {
                try {
                    stmt.execute(sqls[i]);
                    System.out.println("执行成功: " + sqls[i].substring(0, Math.min(50, sqls[i].length())) + "...");
                } catch (Exception e) {
                    System.out.println("执行失败: " + sqls[i].substring(0, Math.min(50, sqls[i].length())) + "...");
                    System.out.println("  原因: " + e.getMessage());
                }
            }
            
            stmt.close();
            conn.close();
            System.out.println("数据库初始化完成！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

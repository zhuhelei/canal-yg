-- 达梦数据库建表脚本
-- 数据库：DEV_DB
-- 创建时间：2026-06-18

-- 创建序列（用于主键自增）
CREATE SEQUENCE SEQ_DRONE_CHANGE_IMAGES
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- 创建无人机变化影像表
CREATE TABLE drone_change_images (
    id BIGINT PRIMARY KEY,
    project_id BIGINT,
    image_month VARCHAR(7) NOT NULL,
    previous_image_url VARCHAR(500),
    current_image_url VARCHAR(500),
    change_image_url VARCHAR(500) NOT NULL,
    longitude DECIMAL(10, 7),
    latitude DECIMAL(10, 7),
    region_name VARCHAR(120),
    geometry_geo_json CLOB,
    status VARCHAR(40) DEFAULT '待核查',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 创建索引（提升查询性能）
CREATE INDEX idx_drone_change_month ON drone_change_images(image_month);
CREATE INDEX idx_drone_change_region ON drone_change_images(region_name);
CREATE INDEX idx_drone_change_status ON drone_change_images(status);
CREATE INDEX idx_drone_change_created ON drone_change_images(created_at DESC);

-- 添加注释
COMMENT ON TABLE drone_change_images IS '无人机变化影像记录表';
COMMENT ON COLUMN drone_change_images.id IS '主键ID';
COMMENT ON COLUMN drone_change_images.project_id IS '项目ID';
COMMENT ON COLUMN drone_change_images.image_month IS '影像月份（格式：YYYY-MM）';
COMMENT ON COLUMN drone_change_images.previous_image_url IS '前时相影像URL';
COMMENT ON COLUMN drone_change_images.current_image_url IS '后时相影像URL';
COMMENT ON COLUMN drone_change_images.change_image_url IS '变化影像URL';
COMMENT ON COLUMN drone_change_images.longitude IS '中心经度';
COMMENT ON COLUMN drone_change_images.latitude IS '中心纬度';
COMMENT ON COLUMN drone_change_images.region_name IS '所属区域';
COMMENT ON COLUMN drone_change_images.geometry_geo_json IS '变化范围GeoJSON';
COMMENT ON COLUMN drone_change_images.status IS '状态（待核查/有变化/无变化）';
COMMENT ON COLUMN drone_change_images.created_at IS '创建时间';
COMMENT ON COLUMN drone_change_images.updated_at IS '更新时间';

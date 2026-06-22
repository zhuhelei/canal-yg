-- 达梦数据库建表脚本
-- 数据库：DEV_DB
-- 创建时间：2026-06-18

-- 创建序列（用于主键自增）
CREATE SEQUENCE SEQ_DRONE_CHANGE_IMAGES
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE SEQUENCE SEQ_CHANGE_PLOTS
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- 创建无人机变化影像表
CREATE TABLE drone_change_images (
    id BIGINT PRIMARY KEY,
    project_id BIGINT,
    image_month VARCHAR(7) NOT NULL,
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
COMMENT ON COLUMN drone_change_images.change_image_url IS '变化影像URL';
COMMENT ON COLUMN drone_change_images.longitude IS '中心经度';
COMMENT ON COLUMN drone_change_images.latitude IS '中心纬度';
COMMENT ON COLUMN drone_change_images.region_name IS '所属区域';
COMMENT ON COLUMN drone_change_images.geometry_geo_json IS '变化范围GeoJSON';
COMMENT ON COLUMN drone_change_images.status IS '状态（待核查/有变化/无变化）';
COMMENT ON COLUMN drone_change_images.created_at IS '创建时间';
COMMENT ON COLUMN drone_change_images.updated_at IS '更新时间';

-- 创建图斑表
CREATE TABLE change_plots (
    id BIGINT PRIMARY KEY,
    image_id BIGINT NOT NULL,
    plot_name VARCHAR(100),
    area DECIMAL(10, 2),
    land_type VARCHAR(50),
    geometry CLOB,
    center_longitude DECIMAL(10, 7),
    center_latitude DECIMAL(10, 7),
    remark VARCHAR(200),
    status VARCHAR(40) DEFAULT '待核查',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (image_id) REFERENCES drone_change_images(id)
);

-- 创建图斑索引
CREATE INDEX idx_change_plots_image ON change_plots(image_id);
CREATE INDEX idx_change_plots_land_type ON change_plots(land_type);
CREATE INDEX idx_change_plots_status ON change_plots(status);

-- 添加图斑表注释
COMMENT ON TABLE change_plots IS '变化图斑记录表';
COMMENT ON COLUMN change_plots.id IS '主键ID';
COMMENT ON COLUMN change_plots.image_id IS '所属影像ID';
COMMENT ON COLUMN change_plots.plot_name IS '图斑名称';
COMMENT ON COLUMN change_plots.area IS '图斑面积（平方米）';
COMMENT ON COLUMN change_plots.land_type IS '地类类型';
COMMENT ON COLUMN change_plots.geometry IS '图斑几何形状GeoJSON';
COMMENT ON COLUMN change_plots.center_longitude IS '中心经度';
COMMENT ON COLUMN change_plots.center_latitude IS '中心纬度';
COMMENT ON COLUMN change_plots.remark IS '备注';
COMMENT ON COLUMN change_plots.status IS '状态（待核查/已确认/无效）';
COMMENT ON COLUMN change_plots.created_at IS '创建时间';
COMMENT ON COLUMN change_plots.updated_at IS '更新时间';

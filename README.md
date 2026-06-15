# 无人机影像变化比对系统

技术栈：

- 后端：Java 17、Spring Boot 3、Spring Data JPA
- 前端：Vue 3、Vite、OpenLayers
- 数据库：本地默认 H2，生产/部署使用达梦数据库

## 功能

- 按月上传上月影像、本月影像、变化图片
- 按月份导入 ZIP 包，批量生成变化影像核查记录
- 保存图片地址、月份、经纬度、所属区域、变化范围 GeoJSON
- 查询变化图片列表
- 查看详情：上月影像、本月影像、变化图片、地图范围
- 在地图上绘制变化范围并保存

## ZIP 导入规则

首页点击“导入”，选择月份并上传 ZIP 包。后台会解压 ZIP，识别其中的图片文件，并为每张图片生成一条待核查记录。

建议 ZIP 目录格式：

```text
区域名称/116.397128_39.909216_变化图.jpg
区域名称/116.398000,39.910000_变化图.png
```

解析规则：

- 第一层目录作为所属区域，例如 `区域名称`
- 文件路径或文件名中出现 `经度_纬度`、`经度,纬度` 时，作为中心经纬度
- 支持图片格式：jpg、jpeg、png、tif、tiff、webp
- 导入接口：`POST /api/drone-change/import`

## 后端本地运行

默认使用 H2，适合本机快速演示：

```bash
cd backend
mvn spring-boot:run
```

## 后端连接达梦

如果 Maven 无法直接拉取达梦 JDBC 驱动，需要先把达梦安装目录里的驱动包安装到本地 Maven 仓库。驱动文件通常在达梦安装目录的 `drivers/jdbc` 下，例如 `DmJdbcDriver18.jar`。

```bash
mvn install:install-file \
  -Dfile=/path/to/DmJdbcDriver18.jar \
  -DgroupId=com.dameng \
  -DartifactId=DmJdbcDriver18 \
  -Dversion=8.1.2.192 \
  -Dpackaging=jar
```

Windows PowerShell 示例：

```powershell
mvn install:install-file `
  -Dfile="D:\dmdbms\drivers\jdbc\DmJdbcDriver18.jar" `
  -DgroupId=com.dameng `
  -DartifactId=DmJdbcDriver18 `
  -Dversion=8.1.2.192 `
  -Dpackaging=jar
```

启动达梦配置：

```bash
cd backend
mvn spring-boot:run -Pdm -Dspring-boot.run.profiles=dm
```

可通过环境变量修改连接：

```bash
DM_DATASOURCE_URL=jdbc:dm://localhost:5236/DRONE_CHANGE
DM_DATASOURCE_USERNAME=SYSDBA
DM_DATASOURCE_PASSWORD=SYSDBA
JPA_DDL_AUTO=update
APP_UPLOAD_DIR=uploads
```

默认达梦连接配置见 `backend/src/main/resources/application-dm.yml`。

## 前端运行

```bash
cd frontend
npm install
npm run dev
```

前端默认请求 `http://localhost:8080/api`。

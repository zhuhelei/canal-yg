<template>
  <main class="page shell-page">
    <header class="topbar app-header">
      <div>
        <h1>无人机影像变化核查</h1>
        <p>左侧选择变化记录，右侧核查影像地图和变化范围</p>
      </div>
    </header>

    <section class="main-layout">
      <aside class="record-rail">
        <section class="rail-filters">
          <label>
            <span>月份</span>
            <input v-model="query.month" type="month" />
          </label>
          <label>
            <span>所属区域</span>
            <input v-model="query.regionName" placeholder="请输入" />
          </label>
          <label>
            <span>核查状态</span>
            <select v-model="query.status">
              <option value="">全部</option>
              <option value="待核查">待核查</option>
              <option value="有变化">有变化</option>
              <option value="无变化">无变化</option>
            </select>
          </label>
          <div class="rail-actions">
            <button type="button" class="secondary" @click="resetQuery">重置</button>
            <button type="button" @click="loadList">查询</button>
          </div>
          <button type="button" class="ghost-action" @click="showImportDialog = true">导入变化影像</button>
        </section>

        <section class="rail-list">
          <button
            v-for="item in pagedItems"
            :key="item.id"
            class="record-tile"
            :class="{ active: detail?.id === item.id }"
            type="button"
            :title="`${item.regionName} ${item.month}`"
            @click="openDetail(item.id)"
          >
            <img :src="previewImage(item)" alt="" />
            <span class="tile-info">
              <strong>{{ item.regionName || '未命名区域' }}</strong>
              <small>{{ item.month }} · {{ normalizeStatus(item.status) }}</small>
              <small>面积：{{ formatArea(item) }}</small>
              <small>绘制状态：{{ drawingStatus(item) }}</small>
              <small>{{ formatCoordinate(item.longitude) }}, {{ formatCoordinate(item.latitude) }}</small>
            </span>
          </button>
          <div v-if="!items.length" class="rail-empty">暂无变化影像记录</div>
        </section>

        <section v-if="items.length" class="rail-pagination">
          <button type="button" class="secondary" :disabled="currentPage === 1" @click="currentPage -= 1">上一页</button>
          <span>{{ currentPage }} / {{ totalPages }}</span>
          <button type="button" class="secondary" :disabled="currentPage === totalPages" @click="currentPage += 1">下一页</button>
        </section>
      </aside>

      <section class="workspace">
        <section v-if="detail" class="detail-page">
          <header class="detail-header">
            <div>
              <h2>{{ detail.regionName || '未命名区域' }}</h2>
              <p>{{ detail.month }} · 中心点 {{ formatCoordinate(detail.longitude) }}, {{ formatCoordinate(detail.latitude) }} · {{ normalizeStatus(detail.status) }}</p>
            </div>
            <div class="detail-actions">
              <button type="button" class="secondary" @click="markNoChange">无变化</button>
              <button type="button" @click="saveGeometry">有变化</button>
            </div>
          </header>

          <section class="image-map-grid">
            <figure>
              <figcaption>{{ previousMonth }} 影像地图</figcaption>
              <div ref="previousMapEl" class="preview-map"></div>
            </figure>
            <figure>
              <figcaption>{{ detail.month }} 影像地图</figcaption>
              <div ref="currentMapEl" class="preview-map"></div>
            </figure>
          </section>

          <section class="map-panel">
            <div ref="drawMapEl" class="map"></div>
            <div class="map-actions">
              <button type="button" @click="startDraw">绘制范围</button>
              <button type="button" class="secondary" @click="clearGeometry">清空</button>
            </div>
            <p class="map-hint">标记“有变化”前，请先在地图上点击绘制变化范围；点击已有范围可查看图斑详情。</p>
          </section>
        </section>

        <div v-else class="empty-state">左侧选择一条变化影像记录开始核查</div>
      </section>
    </section>

    <div v-if="showImportDialog" class="dialog-mask">
      <form class="dialog import-dialog" @submit.prevent="importZip">
        <header>
          <h2>导入变化影像</h2>
          <button type="button" class="icon-button" @click="showImportDialog = false">×</button>
        </header>
        <label>
          <span>月份</span>
          <input v-model="importForm.month" type="month" required />
        </label>
        <label>
          <span>ZIP 包</span>
          <input type="file" accept=".zip,application/zip" required @change="pickZip" />
        </label>
        <p class="hint">ZIP 内第一层目录会作为所属区域；图片路径或文件名中包含 `116.397_39.909`、`116.397,39.909` 这类格式时，会自动解析中心经纬度。</p>
        <footer>
          <button type="button" class="secondary" @click="showImportDialog = false">取消</button>
          <button type="submit" :disabled="importing">{{ importing ? '导入中' : '开始导入' }}</button>
        </footer>
      </form>
    </div>

    <div v-if="plotDialogVisible" class="dialog-mask plot-mask" @click.self="closePlotDialog">
      <section class="plot-dialog">
        <button type="button" class="plot-close" aria-label="关闭" @click="closePlotDialog">×</button>
        <header class="plot-title">
          <span class="title-mark"></span>
          <strong>{{ plotCode }}</strong>
        </header>

        <section class="plot-info-table">
          <div class="info-label">遗产名称</div>
          <div>浙江省大运河世界文化遗产</div>
          <div class="info-label">图斑编号</div>
          <div>{{ plotCode }}</div>
          <div class="info-label">前时相时间</div>
          <div>{{ previousDate }}</div>
          <div class="info-label">后时相时间</div>
          <div>{{ currentDate }}</div>
          <div class="info-label">前类型</div>
          <div>耕地</div>
          <div class="info-label">后类型</div>
          <div>建筑工地</div>
          <div class="info-label">中心经度</div>
          <div>{{ formatCoordinate(detail?.longitude) }}</div>
          <div class="info-label">中心纬度</div>
          <div>{{ formatCoordinate(detail?.latitude) }}</div>
          <div class="info-label">监测面积</div>
          <div>{{ plotArea }}</div>
          <div class="info-label">设区市名称</div>
          <div>杭州市</div>
          <div class="info-label">县市区名称</div>
          <div>{{ detail?.regionName || '拱墅区' }}</div>
          <div class="info-label">乡镇名称</div>
          <div>上塘街道</div>
        </section>

        <section class="plot-compare">
          <h3><span class="title-mark"></span>格局监测对比</h3>
          <div class="compare-images">
            <figure>
              <img :src="comparisonImage('previous')" alt="" />
              <figcaption>前时相时间 {{ previousDate }}</figcaption>
            </figure>
            <figure>
              <img :src="comparisonImage('current')" alt="" />
              <figcaption>后时相时间 {{ currentDate }}</figcaption>
            </figure>
          </div>
        </section>

        <footer class="plot-footer">
          <button type="button" class="secondary wide-button" @click="closePlotDialog">取消</button>
          <button type="button" class="wide-button" @click="closePlotDialog">取消绑定</button>
        </footer>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { api, imageUrl } from './api'

const L = window.L

const mockImageSets = [
  { mask: '/mock-images/R0002a804/C000d573b_mask.png', previous: '/mock-images/R0002a804/C000d573b_7.png', current: '/mock-images/R0002a804/C000d573b_9.png' },
  { mask: '/mock-images/R0002a804/C000d573c_mask.png', previous: '/mock-images/R0002a804/C000d573c_7.png', current: '/mock-images/R0002a804/C000d573c_9.png' },
  { mask: '/mock-images/R0002a824/C000d573e_mask.png', previous: '/mock-images/R0002a824/C000d573e_7.png', current: '/mock-images/R0002a824/C000d573e_9.png' },
  { mask: '/mock-images/R0002a824/C000d573f_mask.png', previous: '/mock-images/R0002a824/C000d573f_7.png', current: '/mock-images/R0002a824/C000d573f_9.png' },
  { mask: '/mock-images/R0002a825/C000d573d_mask.png', previous: '/mock-images/R0002a825/C000d573d_7.png', current: '/mock-images/R0002a825/C000d573d_9.png' },
  { mask: '/mock-images/R0002a825/C000d573e_mask.png', previous: '/mock-images/R0002a825/C000d573e_7.png', current: '/mock-images/R0002a825/C000d573e_9.png' },
  { mask: '/mock-images/R0002a826/C000d573d_mask.png', previous: '/mock-images/R0002a826/C000d573d_7.png', current: '/mock-images/R0002a826/C000d573d_9.png' },
  { mask: '/mock-images/R0002a826/C000d573e_mask.png', previous: '/mock-images/R0002a826/C000d573e_7.png', current: '/mock-images/R0002a826/C000d573e_9.png' },
  { mask: '/mock-images/R0002a850/C000d570e_mask.png', previous: '/mock-images/R0002a850/C000d570e_7.png', current: '/mock-images/R0002a850/C000d570e_9.png' },
  { mask: '/mock-images/R0002a850/C000d570f_mask.png', previous: '/mock-images/R0002a850/C000d570f_7.png', current: '/mock-images/R0002a850/C000d570f_9.png' },
  { mask: '/mock-images/R0002a851/C000d570e_mask.png', previous: '/mock-images/R0002a851/C000d570e_7.png', current: '/mock-images/R0002a851/C000d570e_9.png' },
  { mask: '/mock-images/R0002a851/C000d570f_mask.png', previous: '/mock-images/R0002a851/C000d570f_7.png', current: '/mock-images/R0002a851/C000d570f_9.png' },
  { mask: '/mock-images/R0002a858/C000d5703_mask.png', previous: '/mock-images/R0002a858/C000d5703_7.png', current: '/mock-images/R0002a858/C000d5703_9.png' },
  { mask: '/mock-images/R0002a858/C000d5704_mask.png', previous: '/mock-images/R0002a858/C000d5704_7.png', current: '/mock-images/R0002a858/C000d5704_9.png' },
  { mask: '/mock-images/R0002a859/C000d56f4_mask.png', previous: '/mock-images/R0002a859/C000d56f4_7.png', current: '/mock-images/R0002a859/C000d56f4_9.png' },
  { mask: '/mock-images/R0002a85b/C000d56fa_mask.png', previous: '/mock-images/R0002a85b/C000d56fa_7.png', current: '/mock-images/R0002a85b/C000d56fa_9.png' },
  { mask: '/mock-images/R0002a85c/C000d56f5_mask.png', previous: '/mock-images/R0002a85c/C000d56f5_7.png', current: '/mock-images/R0002a85c/C000d56f5_9.png' },
  { mask: '/mock-images/R0002a85c/C000d56f6_mask.png', previous: '/mock-images/R0002a85c/C000d56f6_7.png', current: '/mock-images/R0002a85c/C000d56f6_9.png' },
  { mask: '/mock-images/R0002a85d/C000d56f5_mask.png', previous: '/mock-images/R0002a85d/C000d56f5_7.png', current: '/mock-images/R0002a85d/C000d56f5_9.png' },
  { mask: '/mock-images/R0002a85d/C000d56f6_mask.png', previous: '/mock-images/R0002a85d/C000d56f6_7.png', current: '/mock-images/R0002a85d/C000d56f6_9.png' }
]

const items = ref([])
const detail = ref(null)
const currentPage = ref(1)
const pageSize = 4
const previousMapEl = ref(null)
const currentMapEl = ref(null)
const drawMapEl = ref(null)
const showImportDialog = ref(false)
const importing = ref(false)
const plotDialogVisible = ref(false)

const query = reactive({
  month: '2025-06',
  regionName: '',
  status: ''
})

const importForm = reactive({
  month: '2025-06',
  zipFile: null
})

const previousMonth = computed(() => getPreviousMonth(detail.value?.month))
const previousDate = computed(() => `${previousMonth.value || '2025-05'}-06`)
const currentDate = computed(() => `${detail.value?.month || '2025-06'}-05`)
const plotCode = computed(() => `WB${String(detail.value?.id || 0).padStart(16, '0')}`)
const plotArea = computed(() => formatArea(detail.value))
const satelliteTileUrl = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'
const totalPages = computed(() => Math.max(1, Math.ceil(items.value.length / pageSize)))
const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return items.value.slice(start, start + pageSize)
})

let previousMap
let currentMap
let drawMap
let drawnLayer
let drawingMode = false

function pickZip(event) {
  importForm.zipFile = event.target.files[0]
}

function resetQuery() {
  query.month = '2025-06'
  query.regionName = ''
  query.status = ''
  loadList()
}

async function loadList() {
  const response = await api.get('/drone-change', { params: query })
  items.value = response.data.map((item, index) => withMockImages(item, index))
  currentPage.value = 1
  if (!items.value.length) {
    detail.value = null
    destroyMaps()
    return
  }

  const selectedStillExists = detail.value && items.value.some((item) => item.id === detail.value.id)
  if (!selectedStillExists) {
    await openDetail(items.value[0].id)
  }
}

async function openDetail(id) {
  const response = await api.get(`/drone-change/${id}`)
  const index = items.value.findIndex((item) => item.id === id)
  detail.value = withMockImages(response.data, index >= 0 ? index : 0)
  await nextTick()
  initDetailMaps()
}

async function importZip() {
  if (!importForm.zipFile) return
  importing.value = true
  try {
    const data = new FormData()
    data.append('month', importForm.month)
    data.append('zipFile', importForm.zipFile)
    await api.post('/drone-change/import', data)
    showImportDialog.value = false
    query.month = importForm.month
    await loadList()
  } finally {
    importing.value = false
  }
}

function initDetailMaps() {
  if (!detail.value) return
  destroyMaps()
  previousMap = createLeafletMap(previousMapEl.value, false)
  currentMap = createLeafletMap(currentMapEl.value, false)
  drawMap = createLeafletMap(drawMapEl.value, true)
}

function createLeafletMap(target, enableGeometry) {
  if (!target) return null

  const center = getCenterLatLng()
  const map = L.map(target, {
    center,
    zoom: hasValidCenter() ? 16 : 10,
    minZoom: 3,
    maxZoom: 21,
    zoomControl: false,
    attributionControl: false
  })

  createSatelliteLayer().addTo(map)
  L.control.zoom({ position: 'topright' }).addTo(map)
  L.circleMarker(center, {
    radius: 7,
    color: '#ffffff',
    weight: 3,
    fillColor: '#f4c044',
    fillOpacity: 1
  }).addTo(map)

  if (enableGeometry) {
    initDrawMap(map)
  }

  requestAnimationFrame(() => map.invalidateSize())
  return map
}

function createSatelliteLayer() {
  return L.tileLayer(satelliteTileUrl, {
    maxZoom: 21,
    minZoom: 3,
    crossOrigin: true
  })
}

function initDrawMap(map) {
  if (detail.value.geometryGeoJson) {
    drawnLayer = L.geoJSON(JSON.parse(detail.value.geometryGeoJson), {
      style: plotStyle,
      onEachFeature: attachPlotClick
    }).addTo(map)
    map.fitBounds(drawnLayer.getBounds(), { maxZoom: 18 })
  }

  map.on('click', (event) => {
    if (!drawingMode && drawnLayer && hasDrawablePolygon()) {
      openPlotDialog()
      return
    }
    if (!drawingMode) return

    if (!drawnLayer) {
      drawnLayer = L.polygon([], plotStyle()).addTo(map)
      attachPlotClick(null, drawnLayer)
    }

    const latLngs = drawnLayer.getLatLngs()[0] || []
    latLngs.push(event.latlng)
    drawnLayer.setLatLngs(latLngs)
  })
}

function plotStyle() {
  return {
    color: '#f04438',
    weight: 2,
    fillColor: '#f04438',
    fillOpacity: 0.12
  }
}

function attachPlotClick(feature, layer) {
  layer.on('click', (event) => {
    L.DomEvent.stopPropagation(event)
    openPlotDialog()
  })
}

function destroyMaps() {
  previousMap?.remove()
  currentMap?.remove()
  drawMap?.remove()
  previousMap = null
  currentMap = null
  drawMap = null
  drawnLayer = null
  drawingMode = false
}

function getCenterLatLng() {
  return [
    Number(detail.value?.latitude || 39.909),
    Number(detail.value?.longitude || 116.397)
  ]
}

function hasValidCenter() {
  return Number(detail.value?.longitude) !== 0 && Number(detail.value?.latitude) !== 0
}

function getPreviousMonth(month) {
  if (!month) return ''
  const [year, monthValue] = month.split('-').map(Number)
  const date = new Date(year, monthValue - 2, 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function startDraw() {
  if (!drawMap) return
  clearGeometry()
  drawingMode = true
}

function clearGeometry() {
  if (!drawnLayer || !drawMap) return
  drawMap.removeLayer(drawnLayer)
  drawnLayer = null
}

async function saveGeometry() {
  if (!detail.value) return
  if (!hasDrawablePolygon()) {
    window.alert('标记有变化前，请先在地图上绘制变化范围。')
    return
  }
  const geometryGeoJson = JSON.stringify(drawnLayer.toGeoJSON())
  const response = await api.put(`/drone-change/${detail.value.id}/geometry`, { geometryGeoJson })
  drawingMode = false
  detail.value = withMockImages(response.data, items.value.findIndex((item) => item.id === response.data.id))
  await loadList()
}

function hasDrawablePolygon() {
  if (!drawnLayer?.getLatLngs) return false
  const points = drawnLayer.getLatLngs()[0] || []
  return points.length >= 3
}

async function markNoChange() {
  if (!detail.value) return
  const response = await api.put(`/drone-change/${detail.value.id}/no-change`)
  detail.value = withMockImages(response.data, items.value.findIndex((item) => item.id === response.data.id))
  await loadList()
}

function openPlotDialog() {
  plotDialogVisible.value = true
}

function closePlotDialog() {
  plotDialogVisible.value = false
}

function normalizeStatus(status) {
  if (status === '已圈定' || status === '审核通过') return '有变化'
  return status || '待核查'
}

function formatCoordinate(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '-'
  return Number(number.toFixed(8)).toString()
}

function previewImage(item) {
  return imageUrl(item?.mockMaskImageUrl || item?.changeImageUrl)
}

function comparisonImage(type) {
  if (type === 'previous') {
    return imageUrl(detail.value?.mockPreviousImageUrl || detail.value?.previousImageUrl || detail.value?.changeImageUrl)
  }
  return imageUrl(detail.value?.mockCurrentImageUrl || detail.value?.currentImageUrl || detail.value?.changeImageUrl)
}

function drawingStatus(item) {
  return item?.geometryGeoJson ? '已绘制' : '未绘制'
}

function formatArea(item) {
  const area = calculateGeoJsonArea(item?.geometryGeoJson)
  if (!area) return '-'
  if (area >= 10000) return `${(area / 10000).toFixed(2)} 公顷`
  return `${area.toFixed(2)} 平方米`
}

function calculateGeoJsonArea(geometryGeoJson) {
  if (!geometryGeoJson) return 0
  try {
    const parsed = JSON.parse(geometryGeoJson)
    const coordinates = getPolygonCoordinates(parsed)
    if (!coordinates || coordinates.length < 3) return 0
    const averageLatitude = coordinates.reduce((sum, point) => sum + point[1], 0) / coordinates.length
    const metersPerDegreeLat = 111320
    const metersPerDegreeLng = 111320 * Math.cos((averageLatitude * Math.PI) / 180)
    const projected = coordinates.map(([lng, lat]) => [lng * metersPerDegreeLng, lat * metersPerDegreeLat])
    let area = 0
    for (let index = 0; index < projected.length; index += 1) {
      const [x1, y1] = projected[index]
      const [x2, y2] = projected[(index + 1) % projected.length]
      area += x1 * y2 - x2 * y1
    }
    return Math.abs(area) / 2
  } catch {
    return 0
  }
}

function getPolygonCoordinates(geoJson) {
  const geometry = geoJson?.type === 'Feature' ? geoJson.geometry : geoJson
  if (geometry?.type === 'Polygon') return geometry.coordinates?.[0]
  if (geometry?.type === 'MultiPolygon') return geometry.coordinates?.[0]?.[0]
  if (geoJson?.type === 'FeatureCollection') {
    const polygonFeature = geoJson.features?.find((feature) => {
      const type = feature?.geometry?.type
      return type === 'Polygon' || type === 'MultiPolygon'
    })
    return getPolygonCoordinates(polygonFeature)
  }
  return null
}

function withMockImages(item, index) {
  const safeIndex = index >= 0 ? index : 0
  const imageSet = mockImageSets[safeIndex % mockImageSets.length]
  return {
    ...item,
    mockMaskImageUrl: imageSet?.mask || null,
    mockPreviousImageUrl: imageSet?.previous || null,
    mockCurrentImageUrl: imageSet?.current || null
  }
}

onMounted(loadList)
</script>

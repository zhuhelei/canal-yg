<template>
  <main class="page shell-page">
    <header class="topbar app-header">
      <div>
        <h1>无人机影像变化核验</h1>
        <p>左侧选择变化记录，右侧核验变化范围</p>
      </div>
      <button type="button" class="ghost-action header-import" @click="showImportDialog = true">导入</button>
    </header>

    <section class="main-layout">
      <aside class="record-rail">
        <section class="rail-filters">
          <label>
            <span>月份</span>
            <input v-model="query.month" type="month" required @change="normalizeQueryMonth" @blur="normalizeQueryMonth" />
          </label>
          <label>
            <span>所属区域</span>
            <select v-model="query.regionName">
              <option value="">全部</option>
              <option v-for="region in hangzhouRegions" :key="region" :value="region">{{ region }}</option>
            </select>
          </label>
          <label>
            <span>核验状态</span>
            <select v-model="query.status">
              <option value="">全部</option>
              <option value="待核查">待核验</option>
              <option value="有变化">有变化</option>
              <option value="无变化">无变化</option>
            </select>
          </label>
          <div class="rail-actions">
            <button type="button" class="secondary" @click="resetQuery">重置</button>
            <button type="button" @click="loadList">查询</button>
          </div>
        </section>

        <section class="rail-list">
          <button
            v-for="item in pagedItems"
            :key="item.id"
            class="record-tile"
            :class="{ active: detail?.id === item.id }"
            type="button"
            :title="displayPlaceName(item)"
            @click="openDetail(item.id)"
          >
            <img :src="previewImage(item)" alt="" />
            <span class="tile-info">
              <strong>{{ displayPlaceName(item) }}</strong>
              <span class="tile-badges">
                <span class="status-pill" :class="statusClass(item.status)">{{ normalizeStatus(item.status) }}</span>
                <span class="draw-pill" :class="drawingClass(item)">{{ drawingStatus(item) }}</span>
              </span>
              <small>面积：{{ formatArea(item) }}</small>
              <small class="tile-region">所属区域：{{ displayRegionName(item) }}</small>
              <small class="coordinate-line"><span class="coord-icon">⌖</span>{{ formatCoordinate(item.longitude) }}, {{ formatCoordinate(item.latitude) }}</small>
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
              <h2>{{ displayPlaceName(detail) }}</h2>
              <p class="detail-meta">
                <span class="coordinate-line"><span class="coord-icon">⌖</span>{{ formatCoordinate(detail.longitude) }}, {{ formatCoordinate(detail.latitude) }}</span>
                <span class="status-pill" :class="statusClass(detail.status)">{{ normalizeStatus(detail.status) }}</span>
              </p>
            </div>
            <div class="detail-actions">
              <button type="button" class="secondary" @click="markNoChange">无变化</button>
              <button type="button" @click="saveGeometry">有变化</button>
            </div>
          </header>

          <section class="image-map-grid">
            <figure>
              <figcaption>{{ formatMonthLabel(previousMonth) }} 正射影像</figcaption>
              <div ref="previousMapEl" class="preview-map"></div>
            </figure>
            <figure>
              <figcaption>{{ formatMonthLabel(detail.month) }} 正射影像</figcaption>
              <div ref="currentMapEl" class="preview-map"></div>
            </figure>
          </section>

          <section class="map-panel">
            <div ref="drawMapEl" class="map"></div>
            <div class="map-actions">
              <button type="button" @click="startDraw">绘制范围</button>
              <button type="button" class="secondary" @click="clearGeometry">清空</button>
            </div>
            <p class="map-hint">标记“有变化”前，请先在地图上点击绘制范围；点击已有范围可查看图斑详情。</p>
          </section>
        </section>

        <div v-else class="empty-state">左侧选择一条变化影像记录开始核验</div>
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
          <input v-model="importForm.month" type="month" required @change="normalizeImportMonth" @blur="normalizeImportMonth" />
        </label>
        <label>
          <span>Excel 文件</span>
          <input type="file" accept=".xlsx,.xls,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" required @change="pickZip" />
        </label>
        <p class="hint">请导入变化影像清单 Excel，月份不可为空；清单中的区域、中心点和影像信息会作为变化记录来源。</p>
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
          <strong>图斑信息</strong>
        </header>

        <section class="plot-section">
          <h3><span class="title-mark"></span>基本信息</h3>
          <div class="plot-info-table">
            <div class="info-label">图斑位置</div>
            <div class="plot-location-value"><input v-model="plotForm.heritageName" /></div>
            <div class="info-label">前时相时间</div>
            <div><input v-model="plotForm.previousDate" type="date" /></div>
            <div class="info-label">后时相时间</div>
            <div><input v-model="plotForm.currentDate" type="date" /></div>
            <div class="info-label">前类型</div>
            <div>
              <select v-model="plotForm.previousType">
                <option v-for="type in plotTypeOptions" :key="type" :value="type">{{ type }}</option>
              </select>
            </div>
            <div class="info-label">后类型</div>
            <div>
              <select v-model="plotForm.currentType">
                <option v-for="type in plotTypeOptions" :key="type" :value="type">{{ type }}</option>
              </select>
            </div>
            <div class="info-label">中心经度</div>
            <div><input v-model="plotForm.longitude" /></div>
            <div class="info-label">中心纬度</div>
            <div><input v-model="plotForm.latitude" /></div>
            <div class="info-label">监测面积</div>
            <div><input v-model="plotForm.area" /></div>
            <div class="info-label">设区市名称</div>
            <div><input v-model="plotForm.cityName" /></div>
            <div class="info-label">县市区名称</div>
            <div><input v-model="plotForm.regionName" /></div>
            <div class="info-label">乡镇名称</div>
            <div><input v-model="plotForm.townName" /></div>
          </div>
        </section>

        <section class="plot-compare plot-section">
          <h3><span class="title-mark"></span>影像对比</h3>
          <div class="compare-images">
            <figure>
              <div ref="previousCompareMapEl" class="compare-map"></div>
              <figcaption>前时相时间 {{ formatDateLabel(plotForm.previousDate) }}</figcaption>
            </figure>
            <figure>
              <div ref="currentCompareMapEl" class="compare-map"></div>
              <figcaption>后时相时间 {{ formatDateLabel(plotForm.currentDate) }}</figcaption>
            </figure>
          </div>
        </section>

        <footer class="plot-footer">
          <button type="button" class="secondary wide-button" @click="closePlotDialog">取消</button>
          <button v-if="hasSavedPlotInfo" type="button" class="danger-button wide-button" @click="deletePlotGeometry">删除</button>
          <button type="button" class="wide-button" @click="savePlotDialog">保存</button>
        </footer>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { api, imageUrl } from './api'

const L = window.L
const hangzhouRegions = ['上城区', '拱墅区', '西湖区', '滨江区', '萧山区', '余杭区', '临平区', '钱塘区', '富阳区', '临安区', '桐庐县', '淳安县', '建德市']

const mockRows = [
  ['R0002a804', 'C000d573b', 486.32, '上城区'],
  ['R0002a804', 'C000d573c', 438.76, '拱墅区'],
  ['R0002a824', 'C000d573e', 421.58, '西湖区'],
  ['R0002a824', 'C000d573f', 396.44, '滨江区'],
  ['R0002a825', 'C000d573d', 358.9, '萧山区'],
  ['R0002a825', 'C000d573e', 472.16, '余杭区'],
  ['R0002a826', 'C000d573d', 337.25, '临平区'],
  ['R0002a826', 'C000d573e', 449.67, '钱塘区'],
  ['R0002a850', 'C000d570e', 286.41, '富阳区'],
  ['R0002a850', 'C000d570f', 314.88, '临安区'],
  ['R0002a851', 'C000d570e', 265.73, '桐庐县'],
  ['R0002a851', 'C000d570f', 302.54, '淳安县'],
  ['R0002a858', 'C000d5703', 198.32, '建德市'],
  ['R0002a858', 'C000d5704', 176.84, '上城区'],
  ['R0002a859', 'C000d56f4', 224.2, '拱墅区'],
  ['R0002a85b', 'C000d56fa', 463.95, '西湖区'],
  ['R0002a85c', 'C000d56f5', 241.67, '滨江区'],
  ['R0002a85c', 'C000d56f6', 219.44, '萧山区'],
  ['R0002a85d', 'C000d56f5', 188.12, '余杭区'],
  ['R0002a85d', 'C000d56f6', 206.35, '临平区']
]

const mockImageSets = mockRows.map(([dir, code, area, regionName]) => ({
  mask: `/mock-images/${dir}/${code}_mask.png`,
  previous: `/mock-images/${dir}/${code}_7.png`,
  current: `/mock-images/${dir}/${code}_9.png`,
  area,
  regionName
}))

const defaultMonth = getCurrentMonth()
const items = ref([])
const monthItems = ref([])
const detail = ref(null)
const currentPage = ref(1)
const pageSize = 10
const previousMapEl = ref(null)
const currentMapEl = ref(null)
const drawMapEl = ref(null)
const previousCompareMapEl = ref(null)
const currentCompareMapEl = ref(null)
const showImportDialog = ref(false)
const importing = ref(false)
const plotDialogVisible = ref(false)

const query = reactive({ month: defaultMonth, regionName: '', status: '' })
const importForm = reactive({ month: defaultMonth, zipFile: null })
const plotTypeOptions = ['耕地', '建筑工地', '绿地', '水域', '道路', '裸地', '房屋建筑', '其他']
const plotForm = reactive({
  heritageName: '',
  plotCode: '',
  previousDate: '',
  currentDate: '',
  previousType: '',
  currentType: '',
  longitude: '',
  latitude: '',
  area: '',
  cityName: '',
  regionName: '',
  townName: ''
})

const previousMonth = computed(() => getPreviousMonth(detail.value?.month))
const previousDate = computed(() => `${previousMonth.value || getPreviousMonth(defaultMonth)}-06`)
const currentDate = computed(() => `${detail.value?.month || defaultMonth}-05`)
const plotCode = computed(() => `WB${String(detail.value?.id || 0).padStart(16, '0')}`)
const plotArea = computed(() => formatArea(detail.value))
const hasSavedPlotInfo = computed(() => Boolean(detail.value?.geometryGeoJson))
const satelliteTileUrl = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'
const totalPages = computed(() => Math.max(1, Math.ceil(items.value.length / pageSize)))
const pagedItems = computed(() => items.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))

let previousMap
let currentMap
let drawMap
let previousCompareMap
let currentCompareMap
let drawnLayer
let monthlyRangeLayer
let drawingMode = false
let syncingMaps = false

function getCurrentMonth() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function pickZip(event) {
  importForm.zipFile = event.target.files[0]
}

function resetQuery() {
  query.month = defaultMonth
  query.regionName = ''
  query.status = ''
  loadList()
}

function normalizeQueryMonth() {
  if (!query.month) query.month = defaultMonth
}

function normalizeImportMonth() {
  if (!importForm.month) importForm.month = query.month || defaultMonth
}

async function loadList() {
  normalizeQueryMonth()
  const [response, monthResponse] = await Promise.all([
    api.get('/drone-change', { params: { month: query.month, status: query.status } }),
    api.get('/drone-change', { params: { month: query.month } })
  ])
  const rows = response.data.map((item, index) => withMockImages(item, index))
  const allMonthRows = monthResponse.data.map((item, index) => withMockImages(item, index))
  items.value = query.regionName ? rows.filter((item) => displayRegionName(item) === query.regionName) : rows
  monthItems.value = query.regionName ? allMonthRows.filter((item) => displayRegionName(item) === query.regionName) : allMonthRows
  currentPage.value = 1
  if (!items.value.length) {
    detail.value = null
    destroyMaps()
    return
  }
  if (!detail.value || !items.value.some((item) => item.id === detail.value.id)) {
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
  normalizeImportMonth()
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
  syncAllMaps([previousMap, currentMap, drawMap].filter(Boolean))
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

  L.tileLayer(satelliteTileUrl, { maxZoom: 21, minZoom: 3, crossOrigin: true }).addTo(map)
  L.control.zoom({ position: 'topright' }).addTo(map)
  L.circleMarker(center, { radius: 7, color: '#f04438', weight: 2, fillColor: '#f04438', fillOpacity: 1 }).addTo(map)
  if (enableGeometry) initDrawMap(map)
  requestAnimationFrame(() => map.invalidateSize())
  return map
}

function createCompareMap(target, sourceMap) {
  if (!target) return null
  const center = sourceMap?.getCenter?.() || L.latLng(getCenterLatLng())
  const zoom = sourceMap?.getZoom?.() || (hasValidCenter() ? 16 : 10)
  const map = L.map(target, {
    center,
    zoom,
    minZoom: 3,
    maxZoom: 21,
    zoomControl: false,
    attributionControl: false,
    dragging: false,
    scrollWheelZoom: false,
    doubleClickZoom: false,
    boxZoom: false,
    keyboard: false,
    tap: false
  })
  L.tileLayer(satelliteTileUrl, { maxZoom: 21, minZoom: 3, crossOrigin: true }).addTo(map)
  requestAnimationFrame(() => map.invalidateSize())
  return map
}

function syncAllMaps(maps) {
  maps.forEach((sourceMap) => {
    sourceMap.on('moveend zoomend', () => {
      if (syncingMaps) return
      syncingMaps = true
      const center = sourceMap.getCenter()
      const zoom = sourceMap.getZoom()
      maps.forEach((targetMap) => {
        if (targetMap !== sourceMap) targetMap.setView(center, zoom, { animate: false })
      })
      syncingMaps = false
    })
  })
}

function initDrawMap(map) {
  renderMonthlyRanges(map)

  map.on('click', (event) => {
    if (!drawingMode) return
    if (!drawnLayer) {
      drawnLayer = L.polygon([], plotStyle()).addTo(map)
      attachPlotClick(null, drawnLayer, detail.value?.id)
    }
    const latLngs = drawnLayer.getLatLngs()[0] || []
    latLngs.push(event.latlng)
    drawnLayer.setLatLngs(latLngs)
  })
}

function plotStyle() {
  return { color: '#f04438', weight: 2, fillColor: '#f04438', fillOpacity: 0.12 }
}

function monthlyRangeStyle(item) {
  const isCurrent = item?.id === detail.value?.id
  return {
    color: '#f04438',
    weight: isCurrent ? 4 : 2,
    fillColor: '#f04438',
    fillOpacity: isCurrent ? 0.2 : 0.12
  }
}

function renderMonthlyRanges(map) {
  if (!map) return
  if (monthlyRangeLayer) map.removeLayer(monthlyRangeLayer)
  monthlyRangeLayer = L.layerGroup().addTo(map)

  const selectedBounds = []
  const monthBounds = []
  monthItems.value.forEach((item) => {
    if (!item.geometryGeoJson) return
    try {
      const layer = L.geoJSON(JSON.parse(item.geometryGeoJson), {
        style: () => monthlyRangeStyle(item),
        onEachFeature: (feature, featureLayer) => attachPlotClick(feature, featureLayer, item.id)
      }).addTo(monthlyRangeLayer)
      const bounds = layer.getBounds()
      if (bounds?.isValid?.()) {
        monthBounds.push(bounds)
        if (item.id === detail.value?.id) selectedBounds.push(bounds)
      }
    } catch {
      // Ignore malformed stored geometry so one bad record does not break the map.
    }
  })

  const targetBounds = selectedBounds.length ? mergeBounds(selectedBounds) : mergeBounds(monthBounds)
  if (targetBounds?.isValid?.()) {
    map.fitBounds(targetBounds, { maxZoom: 18, padding: [24, 24] })
  }
}

function mergeBounds(boundsList) {
  if (!boundsList.length) return null
  const bounds = boundsList[0]
  boundsList.slice(1).forEach((nextBounds) => bounds.extend(nextBounds))
  return bounds
}

function attachPlotClick(feature, layer, recordId = detail.value?.id) {
  const openDialog = (event) => {
    L.DomEvent.stopPropagation(event)
    if (recordId && recordId !== detail.value?.id) {
      openDetail(recordId).then(openPlotDialog)
      return
    }
    openPlotDialog()
  }
  layer.on('click', openDialog)
  layer.on('contextmenu', openDialog)
}

function destroyMaps() {
  previousMap?.remove()
  currentMap?.remove()
  drawMap?.remove()
  destroyCompareMaps()
  previousMap = null
  currentMap = null
  drawMap = null
  drawnLayer = null
  monthlyRangeLayer = null
  drawingMode = false
  syncingMaps = false
}

function destroyCompareMaps() {
  previousCompareMap?.remove()
  currentCompareMap?.remove()
  previousCompareMap = null
  currentCompareMap = null
}

function getCenterLatLng() {
  return [Number(detail.value?.latitude || 39.909), Number(detail.value?.longitude || 116.397)]
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

function formatMonthLabel(month) {
  if (!month) return ''
  const [year, monthValue] = month.split('-')
  return `${year}年${monthValue}月`
}

function formatDateLabel(dateText) {
  if (!dateText) return ''
  const [year, monthValue, day] = dateText.split('-')
  return `${year}年${monthValue}月${day}日`
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
  if (items.value.some((item) => item.id === response.data.id)) {
    await openDetail(response.data.id)
  }
}

function hasDrawablePolygon() {
  return (drawnLayer?.getLatLngs?.()[0] || []).length >= 3
}

async function markNoChange() {
  if (!detail.value) return
  const response = await api.put(`/drone-change/${detail.value.id}/no-change`)
  detail.value = withMockImages(response.data, items.value.findIndex((item) => item.id === response.data.id))
  await loadList()
  if (items.value.some((item) => item.id === response.data.id)) {
    await openDetail(response.data.id)
  }
}

async function openPlotDialog() {
  initPlotForm()
  plotDialogVisible.value = true
  await nextTick()
  initCompareMaps()
}

function closePlotDialog() {
  plotDialogVisible.value = false
  destroyCompareMaps()
}

function initPlotForm() {
  plotForm.heritageName = displayPlaceName(detail.value)
  plotForm.plotCode = plotCode.value
  plotForm.previousDate = previousDate.value
  plotForm.currentDate = currentDate.value
  plotForm.previousType = '耕地'
  plotForm.currentType = '建筑工地'
  plotForm.longitude = formatCoordinate(detail.value?.longitude)
  plotForm.latitude = formatCoordinate(detail.value?.latitude)
  plotForm.area = plotArea.value
  plotForm.cityName = '杭州市'
  plotForm.regionName = displayRegionName(detail.value)
  plotForm.townName = '上塘街道'
}

function initCompareMaps() {
  destroyCompareMaps()
  previousCompareMap = createCompareMap(previousCompareMapEl.value, previousMap)
  currentCompareMap = createCompareMap(currentCompareMapEl.value, currentMap)
}

async function savePlotDialog() {
  if (!detail.value) return
  if (drawnLayer && hasDrawablePolygon()) {
    const geometryResponse = await api.put(`/drone-change/${detail.value.id}/geometry`, {
      geometryGeoJson: JSON.stringify(drawnLayer.toGeoJSON())
    })
    detail.value = withMockImages(geometryResponse.data, items.value.findIndex((item) => item.id === geometryResponse.data.id))
    drawingMode = false
  }
  const response = await api.put(`/drone-change/${detail.value.id}/plot-info`, {
    regionName: plotForm.heritageName,
    longitude: Number(plotForm.longitude),
    latitude: Number(plotForm.latitude),
    previousImageUrl: detail.value.previousImageUrl,
    currentImageUrl: detail.value.currentImageUrl,
    changeImageUrl: detail.value.changeImageUrl
  })
  detail.value = withMockImages(response.data, items.value.findIndex((item) => item.id === response.data.id))
  closePlotDialog()
  await loadList()
  if (items.value.some((item) => item.id === response.data.id)) {
    await openDetail(response.data.id)
  }
}

async function deletePlotGeometry() {
  if (!detail.value?.id) return
  if (!window.confirm('确认删除该图斑信息和已绘制范围吗？')) return
  const response = await api.delete(`/drone-change/${detail.value.id}/geometry`)
  detail.value = withMockImages(response.data, items.value.findIndex((item) => item.id === response.data.id))
  closePlotDialog()
  await loadList()
  if (items.value.some((item) => item.id === response.data.id)) {
    await openDetail(response.data.id)
  }
}

function normalizeStatus(status) {
  if (status === '已圈定' || status === '审核通过' || status === '有变化') return '有变化'
  if (status === '无变化') return '无变化'
  return '待核验'
}

function statusClass(status) {
  const normalized = normalizeStatus(status)
  if (normalized === '有变化') return 'status-changed'
  if (normalized === '无变化') return 'status-unchanged'
  return 'status-pending'
}

function drawingClass(item) {
  return item?.geometryGeoJson ? 'draw-done' : 'draw-empty'
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
  if (type === 'previous') return imageUrl(detail.value?.mockPreviousImageUrl || detail.value?.previousImageUrl || detail.value?.changeImageUrl)
  return imageUrl(detail.value?.mockCurrentImageUrl || detail.value?.currentImageUrl || detail.value?.changeImageUrl)
}

function drawingStatus(item) {
  return item?.geometryGeoJson ? '已绘制' : '未绘制'
}

function formatArea(item) {
  const area = calculateGeoJsonArea(item?.geometryGeoJson) || Number(item?.mockArea || 0)
  return `${area.toFixed(2)} 平方米`
}

function calculateGeoJsonArea(geometryGeoJson) {
  if (!geometryGeoJson) return 0
  try {
    const coordinates = getPolygonCoordinates(JSON.parse(geometryGeoJson))
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
  if (geoJson?.type === 'FeatureCollection') return getPolygonCoordinates(geoJson.features?.find((feature) => ['Polygon', 'MultiPolygon'].includes(feature?.geometry?.type)))
  return null
}

function displayRegionName(item) {
  return item?.mockRegionName || item?.regionName || '未命名区域'
}

function displayPlaceName(item) {
  const name = item?.regionName || ''
  if (isReadableChineseName(name)) return name
  return `变化图斑${String(item?.id || '').padStart(3, '0')}`
}

function isReadableChineseName(name) {
  if (!name) return false
  if (/[�]/.test(name)) return false
  if (/[一-龥]/.test(name)) return true
  return false
}

function withMockImages(item, index) {
  const imageSet = mockImageSets[(index >= 0 ? index : 0) % mockImageSets.length]
  return {
    ...item,
    mockMaskImageUrl: imageSet?.mask || null,
    mockPreviousImageUrl: imageSet?.previous || null,
    mockCurrentImageUrl: imageSet?.current || null,
    mockArea: imageSet?.area || 300,
    mockRegionName: imageSet?.regionName || null
  }
}

onMounted(loadList)
</script>

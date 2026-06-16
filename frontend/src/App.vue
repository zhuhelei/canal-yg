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
            月份
            <input v-model="query.month" type="month" />
          </label>
          <label>
            所属区域
            <input v-model="query.regionName" placeholder="输入区域名称" />
          </label>
          <label>
            核查状态
            <select v-model="query.status">
              <option value="">全部</option>
              <option value="待核查">待核查</option>
              <option value="已圈定">已圈定</option>
              <option value="审核通过">审核通过</option>
            </select>
          </label>
          <div class="rail-actions">
            <button type="button" @click="loadList">查询</button>
            <button type="button" class="secondary" @click="showImportDialog = true">导入</button>
          </div>
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
            <img :src="imageUrl(item.changeImageUrl)" alt="" />
            <span class="tile-info">
              <strong>{{ item.regionName }}</strong>
              <small>{{ item.month }} · {{ item.status }}</small>
              <small>{{ item.longitude }}, {{ item.latitude }}</small>
            </span>
          </button>
          <div v-if="!items.length" class="rail-empty">暂无</div>
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
            <h2>{{ detail.regionName }}</h2>
            <p>{{ detail.month }} · 中心点 {{ detail.longitude }}, {{ detail.latitude }} · {{ detail.status }}</p>
          </div>
          <div class="detail-actions">
            <button type="button" class="secondary" @click="approveRecord">审核通过</button>
            <button type="button" @click="saveGeometry">保存范围</button>
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
        </section>
      </section>

      <div v-else class="empty-state">
        左侧选择一条变化影像记录开始核查
      </div>
    </section>
    </section>

    <div v-if="showImportDialog" class="dialog-mask">
      <form class="dialog" @submit.prevent="importZip">
        <header>
          <h2>导入变化影像</h2>
          <button type="button" class="icon-button" @click="showImportDialog = false">×</button>
        </header>
        <label>
          月份
          <input v-model="importForm.month" type="month" required />
        </label>
        <label>
          ZIP 包
          <input type="file" accept=".zip,application/zip" required @change="pickZip" />
        </label>
        <p class="hint">
          ZIP 内第一层目录会作为所属区域；图片路径或文件名中包含 116.397_39.909、116.397,39.909 这类格式时，会自动解析中心经纬度。
        </p>
        <footer>
          <button type="button" class="secondary" @click="showImportDialog = false">取消</button>
          <button type="submit" :disabled="importing">{{ importing ? '导入中' : '开始导入' }}</button>
        </footer>
      </form>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { api, imageUrl } from './api'

const L = window.L

const items = ref([])
const detail = ref(null)
const currentPage = ref(1)
const pageSize = 4
const previousMapEl = ref(null)
const currentMapEl = ref(null)
const drawMapEl = ref(null)
const showImportDialog = ref(false)
const importing = ref(false)

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
const satelliteTileUrl = 'https://ditu.zjzwfw.gov.cn/services/wmts/imgmap/default/oss?service=WMTS&request=GetTile&version=1.0.0&layer=mapSatellite&style=&tilematrixset=imgmap&format=image%2Fjpeg&height=256&width=256&tilematrix={z}&tilerow={y}&tilecol={x}&token=051eb92d-6fba-426f-9de7-14e786d3fa9a'
const totalPages = computed(() => Math.max(1, Math.ceil(items.value.length / pageSize)))
const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return items.value.slice(start, start + pageSize)
})

let previousMap
let currentMap
let drawMap
let drawnLayer

function pickZip(event) {
  importForm.zipFile = event.target.files[0]
}

async function loadList() {
  const response = await api.get('/drone-change', { params: query })
  items.value = response.data
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
  detail.value = response.data
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
    minZoom: 10,
    maxZoom: 21,
    zoomControl: false
  })

  createSatelliteLayer().addTo(map)
  L.control.zoom({ position: 'topright' }).addTo(map)
  L.circleMarker(center, {
    radius: 7,
    color: '#ffffff',
    weight: 3,
    fillColor: '#e11d48',
    fillOpacity: 1
  }).addTo(map)

  if (enableGeometry) {
    initDrawMap(map)
  }

  requestAnimationFrame(() => map.invalidateSize())
  return map
}

function createSatelliteLayer() {
  ensureArcgisTileLayer()
  return new L.TileLayer.ArcGIS(satelliteTileUrl, {
    zoomOffset: 0,
    maxZoom: 21,
    minZoom: 10,
    noWrap: true,
    tms: false
  })
}

function ensureArcgisTileLayer() {
  if (L.TileLayer.ArcGIS) return
  L.TileLayer.ArcGIS = L.TileLayer.extend({
    getTileUrl(coords) {
      return L.Util.template(this._url, {
        x: toHex(coords.x, 8),
        y: toHex(coords.y, 8),
        z: this._map.getZoom()
      })
    }
  })
}

function toHex(value, length) {
  return value.toString(16).padStart(length, '0')
}

function initDrawMap(map) {
  if (detail.value.geometryGeoJson) {
    drawnLayer = L.geoJSON(JSON.parse(detail.value.geometryGeoJson)).addTo(map)
    map.fitBounds(drawnLayer.getBounds(), { maxZoom: 18 })
  }

  map.on('click', (event) => {
    if (!drawnLayer) {
      drawnLayer = L.polygon([], {
        color: '#2364aa',
        weight: 2,
        fillColor: '#2364aa',
        fillOpacity: 0.18
      }).addTo(map)
    }
    const latLngs = drawnLayer.getLatLngs()[0] || []
    latLngs.push(event.latlng)
    drawnLayer.setLatLngs(latLngs)
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
}

function clearGeometry() {
  if (drawnLayer) {
    drawMap.removeLayer(drawnLayer)
    drawnLayer = null
  }
}

async function saveGeometry() {
  if (!detail.value || !drawnLayer) return
  const geometryGeoJson = JSON.stringify(drawnLayer.toGeoJSON())
  const response = await api.put(`/drone-change/${detail.value.id}/geometry`, { geometryGeoJson })
  detail.value = response.data
  await loadList()
}

async function approveRecord() {
  if (!detail.value) return
  const response = await api.put(`/drone-change/${detail.value.id}/approve`)
  detail.value = response.data
  await loadList()
}

onMounted(loadList)
</script>

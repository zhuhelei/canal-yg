const fs = require('node:fs')
const http = require('node:http')
const path = require('node:path')

const root = __dirname
const port = 8099
const backend = 'http://localhost:8080'

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.map': 'application/json; charset=utf-8'
}

function sendFile(response, filePath) {
  fs.readFile(filePath, (error, content) => {
    if (error) {
      response.writeHead(404)
      response.end('Not found')
      return
    }
    response.writeHead(200, {
      'Content-Type': mimeTypes[path.extname(filePath).toLowerCase()] || 'application/octet-stream'
    })
    response.end(content)
  })
}

function proxy(request, response) {
  const target = new URL(request.url, backend)
  const proxyRequest = http.request(target, {
    method: request.method,
    headers: {
      ...request.headers,
      host: target.host
    }
  }, (proxyResponse) => {
    response.writeHead(proxyResponse.statusCode || 500, proxyResponse.headers)
    proxyResponse.pipe(response)
  })

  proxyRequest.on('error', () => {
    response.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' })
    response.end('Backend service unavailable')
  })

  request.pipe(proxyRequest)
}

function resolveStatic(urlPath) {
  if (urlPath === '/') return path.join(root, 'preview-index.html')
  if (urlPath === '/preview-app.js') return path.join(root, 'preview-app.js')
  if (urlPath === '/vendor/vue.global.prod.js') return path.join(root, 'node_modules', 'vue', 'dist', 'vue.global.prod.js')
  if (urlPath === '/vendor/axios.min.js') return path.join(root, 'node_modules', 'axios', 'dist', 'axios.min.js')

  const normalized = path.normalize(decodeURIComponent(urlPath)).replace(/^([/\\])+/, '')
  const filePath = path.join(root, normalized)
  return filePath.startsWith(root) ? filePath : null
}

const server = http.createServer((request, response) => {
  const urlPath = new URL(request.url, `http://127.0.0.1:${port}`).pathname
  if (urlPath.startsWith('/api/') || urlPath.startsWith('/uploads/')) {
    proxy(request, response)
    return
  }

  const filePath = resolveStatic(urlPath)
  if (!filePath) {
    response.writeHead(403)
    response.end('Forbidden')
    return
  }
  sendFile(response, filePath)
})

server.listen(port, '0.0.0.0', () => {
  console.log(`Preview server running at http://127.0.0.1:${port}/`)
})

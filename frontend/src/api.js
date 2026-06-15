import axios from 'axios'

export const api = axios.create({
  baseURL: '/api'
})

export function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return path
}

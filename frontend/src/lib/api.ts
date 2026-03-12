import axios from 'axios'
import type { AuthResponse, ReportRequest, ReportResponse } from '../types/report'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
})

const LOCAL_KEY = 'pod_reports_local_v1'
const LOCAL_SEED_FLAG = 'pod_reports_local_seeded_v1'

function nowIso() {
  return new Date().toISOString()
}

function readLocalReports(): ReportResponse[] {
  const raw = localStorage.getItem(LOCAL_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as ReportResponse[]
  } catch {
    return []
  }
}

function writeLocalReports(reports: ReportResponse[]) {
  localStorage.setItem(LOCAL_KEY, JSON.stringify(reports))
}

function ensureDummySeed() {
  if (localStorage.getItem(LOCAL_SEED_FLAG) === '1') return
  const reports = readLocalReports()
  if (reports.length === 0) {
    const dummy: ReportResponse = {
      id: 1,
      podName: 'AI Delivery POD',
      date: new Date().toISOString().slice(0, 10),
      sprintNumber: 12,
      dayOfSprint: 6,
      goal: 'Stabilize validation and prep for deployment.',
      scrumMasterName: 'Dummy Scrum Master',
      managerName: 'Dummy Manager',
      sprintBurndown: {
        totalStories: 20,
        completed: 12,
        inProgress: 6,
        remainingSp: 24,
        idealSp: 20,
        variance: -4,
        trend: 'At Risk',
      },
      doraMetrics: {
        deploymentFrequencyToday: 1,
        deploymentFrequencySprintAvg: 0.8,
        deploymentFrequencyTarget: 1,
        leadTimeToday: 18,
        leadTimeAvg: 22,
        leadTimeTarget: 24,
        changeFailureRateToday: 12,
        changeFailureRateAvg: 10,
        changeFailureRateTarget: 8,
        mttrToday: 45,
        mttrAvg: 52,
        mttrTarget: 60,
      },
      pipelineTrackers: [
        { itemId: 'AI-100', dataPrep: 'COMPLETE', modelDev: 'COMPLETE', integration: 'COMPLETE', validation: 'IN_PROGRESS', deployment: 'NOT_STARTED', notes: 'Validation in progress' },
      ],
      workItems: [
        { memberName: 'Dev A', storyId: 'AI-100', taskDescription: 'Validation fixes', status: 'IN_PROGRESS', ageDays: 2, flag: '' },
      ],
      blockersDependencies: [
        { itemId: 'B1', type: 'BLOCKER', description: 'Awaiting infra token', owner: 'Platform Team', actionNeeded: 'Provide prod token' },
      ],
      risksObservations: [
        { description: 'Validation accuracy slightly below target.' },
      ],
      focusOutlook: {
        todayFocus: 'Close validation gaps and rerun checks.',
        tomorrowOutlook: 'Prepare release candidate.',
        decisionsNeeded: 'Manager approval for rollout window.',
      },
      versionNumber: 1,
      warningText: 'Burndown is off track',
      createdBy: 'local',
      createdAt: nowIso(),
    }
    writeLocalReports([dummy])
  }
  localStorage.setItem(LOCAL_SEED_FLAG, '1')
}

function isNetworkError(error: unknown) {
  return axios.isAxiosError(error) && (!error.response || error.code === 'ERR_NETWORK')
}

export function getApiErrorMessage(error: unknown, fallback = 'Request failed') {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { error?: string; message?: string } | undefined
    return data?.error || data?.message || error.message || fallback
  }
  if (error instanceof Error) return error.message
  return fallback
}

function normalizeDate(input: string) {
  if (/^\d{4}-\d{2}-\d{2}$/.test(input)) return input
  const m = input.match(/^(\d{2})-(\d{2})-(\d{4})$/)
  if (m) return `${m[3]}-${m[2]}-${m[1]}`
  return input
}

function localList(params?: { sprint?: number; date?: string }) {
  ensureDummySeed()
  let reports = readLocalReports()
  if (params?.sprint != null) {
    reports = reports.filter((r) => r.sprintNumber === params.sprint)
  }
  if (params?.date) {
    const d = normalizeDate(params.date)
    reports = reports.filter((r) => r.date === d)
  }
  return reports.sort((a, b) => b.id - a.id)
}

function localGet(id: string) {
  ensureDummySeed()
  const report = readLocalReports().find((r) => String(r.id) === String(id))
  if (!report) throw new Error('Report not found')
  return report
}

function localCreate(payload: ReportRequest): ReportResponse {
  ensureDummySeed()
  const reports = readLocalReports()
  const nextId = reports.length ? Math.max(...reports.map((r) => r.id)) + 1 : 1
  const saved: ReportResponse = {
    ...payload,
    date: normalizeDate(payload.date),
    id: nextId,
    versionNumber: 1,
    warningText: '',
    createdBy: 'local',
    createdAt: nowIso(),
  }
  reports.push(saved)
  writeLocalReports(reports)
  return saved
}

function localUpdate(id: string, payload: ReportRequest): ReportResponse {
  ensureDummySeed()
  const reports = readLocalReports()
  const idx = reports.findIndex((r) => String(r.id) === String(id))
  if (idx < 0) throw new Error('Report not found')
  const current = reports[idx]
  const saved: ReportResponse = {
    ...payload,
    date: normalizeDate(payload.date),
    id: current.id,
    versionNumber: (current.versionNumber || 1) + 1,
    warningText: current.warningText || '',
    createdBy: current.createdBy || 'local',
    createdAt: current.createdAt || nowIso(),
  }
  reports[idx] = saved
  writeLocalReports(reports)
  return saved
}

export async function login(email: string, password: string) {
  const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
  return data
}

export async function register(name: string, email: string, password: string, role: string) {
  const { data } = await api.post<AuthResponse>('/auth/register', { name, email, password, role })
  return data
}

export async function listReports(params?: { sprint?: number; date?: string }) {
  try {
    const { data } = await api.get<ReportResponse[]>('/reports', { params })
    return data
  } catch (error) {
    if (isNetworkError(error)) return localList(params)
    throw error
  }
}

export async function getReport(id: string) {
  try {
    const { data } = await api.get<ReportResponse>(`/reports/${id}`)
    return data
  } catch (error) {
    if (isNetworkError(error)) return localGet(id)
    throw error
  }
}

export async function createReport(payload: ReportRequest) {
  try {
    const { data } = await api.post<ReportResponse>('/reports', payload)
    return data
  } catch (error) {
    if (isNetworkError(error)) return localCreate(payload)
    throw error
  }
}

export async function updateReport(id: string, payload: ReportRequest) {
  try {
    const { data } = await api.put<ReportResponse>(`/reports/${id}`, payload)
    return data
  } catch (error) {
    if (isNetworkError(error)) return localUpdate(id, payload)
    throw error
  }
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

export async function downloadDocx(id: number) {
  try {
    const response = await api.post(`/reports/${id}/generate-docx`, {}, { responseType: 'blob' })
    downloadBlob(response.data, `daily-pod-status-${id}.docx`)
  } catch (error) {
    if (isNetworkError(error)) {
      throw new Error('DOCX export needs backend. Start backend on http://localhost:8080.')
    }
    throw error
  }
}

export async function downloadPdf(id: number) {
  try {
    const response = await api.post(`/reports/${id}/generate-pdf`, {}, { responseType: 'blob' })
    downloadBlob(response.data, `daily-pod-status-${id}.pdf`)
  } catch (error) {
    if (isNetworkError(error)) {
      throw new Error('PDF export needs backend. Start backend on http://localhost:8080.')
    }
    throw error
  }
}

export function printableHtml(id: number) {
  return `${api.defaults.baseURL}/reports/${id}/printable-html`
}

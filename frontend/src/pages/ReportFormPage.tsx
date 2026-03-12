import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createReport, getApiErrorMessage, getReport, updateReport } from '../lib/api'
import { Button } from '../components/ui/button'
import { Input } from '../components/ui/input'
import { Textarea } from '../components/ui/textarea'
import type { BlockerDependency, PipelineTracker, ReportRequest, RiskObservation, WorkItem } from '../types/report'

const CUSTOM_TEMPLATE_KEY = 'pod_custom_templates_v1'
const DEFAULT_GOOGLE_API_KEY = 'AIzaSyBp3ljxy8tmFKDYP8XZG9HrehnTcFEtMDM'
const GEMINI_MODELS = ['gemini-2.5-flash-lite', 'gemini-2.5-flash', 'gemini-2.0-flash'] as const

const stageStatuses = ['COMPLETE', 'IN_PROGRESS', 'NOT_STARTED', 'BLOCKED'] as const
const workStatuses = ['DONE', 'IN_PROGRESS', 'TODO', 'BLOCKED'] as const

type SavedTemplate = {
  id: string
  name: string
  payload: ReportRequest
  createdAt: string
}

type TemplateOption = {
  id: string
  name: string
  payload: ReportRequest
  source: 'builtin' | 'custom'
}

function defaultForm(): ReportRequest {
  return {
    podName: '',
    date: new Date().toISOString().slice(0, 10),
    sprintNumber: 1,
    dayOfSprint: 1,
    goal: '',
    scrumMasterName: localStorage.getItem('name') || '',
    managerName: '',
    sprintBurndown: {},
    doraMetrics: {},
    pipelineTrackers: [{ itemId: '', dataPrep: 'NOT_STARTED', modelDev: 'NOT_STARTED', integration: 'NOT_STARTED', validation: 'NOT_STARTED', deployment: 'NOT_STARTED' }],
    workItems: [{ memberName: '', status: 'TODO' }],
    blockersDependencies: [{ itemId: '', type: 'BLOCKER' }],
    risksObservations: [{ description: '' }],
    focusOutlook: {},
  }
}

function builtinTemplates(): TemplateOption[] {
  return [
    {
      id: 'builtin-daily-standup',
      name: 'Daily Standup',
      source: 'builtin',
      payload: {
        ...defaultForm(),
        goal: 'Complete planned sprint tasks and resolve blockers.',
      },
    },
    {
      id: 'builtin-release',
      name: 'Release Day',
      source: 'builtin',
      payload: {
        ...defaultForm(),
        goal: 'Deploy release candidate and monitor stability.',
        pipelineTrackers: [{ itemId: 'REL-1', dataPrep: 'COMPLETE', modelDev: 'COMPLETE', integration: 'COMPLETE', validation: 'IN_PROGRESS', deployment: 'NOT_STARTED', notes: 'Final checks in progress' }],
      },
    },
    {
      id: 'builtin-risk',
      name: 'Risk Review',
      source: 'builtin',
      payload: {
        ...defaultForm(),
        goal: 'Review risks and assign concrete actions.',
        blockersDependencies: [{ itemId: 'RISK-1', type: 'BLOCKER', description: 'Pending dependency', owner: 'Owner Name', actionNeeded: 'Escalate and close by EOD' }],
        risksObservations: [{ description: 'High-priority dependency requires management attention.' }],
      },
    },
  ]
}

function hasText(value?: string) {
  return Boolean(value && value.trim().length > 0)
}

function parseOptionalNumber(value: string) {
  if (value.trim() === '') return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function normalizeDate(input: string) {
  if (/^\d{4}-\d{2}-\d{2}$/.test(input)) return input
  const match = input.match(/^(\d{2})-(\d{2})-(\d{4})$/)
  return match ? `${match[3]}-${match[2]}-${match[1]}` : input
}

function sanitizePayload(form: ReportRequest): ReportRequest {
  return {
    ...form,
    podName: form.podName.trim(),
    date: normalizeDate(form.date),
    goal: form.goal?.trim(),
    scrumMasterName: form.scrumMasterName?.trim(),
    managerName: form.managerName?.trim(),
    sprintBurndown: form.sprintBurndown || {},
    doraMetrics: form.doraMetrics || {},
    focusOutlook: {
      todayFocus: form.focusOutlook?.todayFocus?.trim(),
      tomorrowOutlook: form.focusOutlook?.tomorrowOutlook?.trim(),
      decisionsNeeded: form.focusOutlook?.decisionsNeeded?.trim(),
    },
    pipelineTrackers: form.pipelineTrackers
      .filter((p) => hasText(p.itemId) || hasText(p.notes) || p.dataPrep || p.modelDev || p.integration || p.validation || p.deployment)
      .map((p) => ({
        ...p,
        itemId: p.itemId.trim(),
        notes: p.notes?.trim(),
        dataPrep: p.dataPrep || 'NOT_STARTED',
        modelDev: p.modelDev || 'NOT_STARTED',
        integration: p.integration || 'NOT_STARTED',
        validation: p.validation || 'NOT_STARTED',
        deployment: p.deployment || 'NOT_STARTED',
      })),
    workItems: form.workItems
      .filter((w) => hasText(w.memberName) || hasText(w.storyId) || hasText(w.taskDescription) || w.status || w.ageDays != null || hasText(w.flag))
      .map((w) => ({
        ...w,
        memberName: w.memberName.trim(),
        storyId: w.storyId?.trim(),
        taskDescription: w.taskDescription?.trim(),
        flag: w.flag?.trim(),
        status: w.status || 'TODO',
      })),
    blockersDependencies: form.blockersDependencies
      .filter((b) => hasText(b.itemId) || hasText(b.description) || hasText(b.owner) || hasText(b.actionNeeded))
      .map((b) => ({
        ...b,
        itemId: b.itemId?.trim(),
        description: b.description?.trim(),
        owner: b.owner?.trim(),
        actionNeeded: b.actionNeeded?.trim(),
        type: b.type || 'BLOCKER',
      })),
    risksObservations: form.risksObservations.filter((r) => hasText(r.description)).map((r) => ({ description: r.description?.trim() })),
  }
}

function readCustomTemplates(): SavedTemplate[] {
  const raw = localStorage.getItem(CUSTOM_TEMPLATE_KEY)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as SavedTemplate[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writeCustomTemplates(templates: SavedTemplate[]) {
  localStorage.setItem(CUSTOM_TEMPLATE_KEY, JSON.stringify(templates))
}

function extractJson(text: string) {
  const fenced = text.match(/```json\s*([\s\S]*?)```/i)
  if (fenced?.[1]) return fenced[1].trim()
  const start = text.indexOf('{')
  const end = text.lastIndexOf('}')
  if (start >= 0 && end > start) return text.slice(start, end + 1)
  return text
}

function parseGoogleError(raw: string) {
  try {
    const data = JSON.parse(raw) as {
      error?: {
        code?: number
        message?: string
        status?: string
      }
    }
    return {
      code: data.error?.code,
      status: data.error?.status,
      message: data.error?.message || raw,
    }
  } catch {
    return { message: raw }
  }
}

function normalizeEnum<T extends string>(input: unknown, allowed: readonly T[], fallback: T): T {
  if (typeof input !== 'string') return fallback
  const candidate = input.trim().toUpperCase() as T
  return allowed.includes(candidate) ? candidate : fallback
}

function normalizeStageStatus(input?: string): PipelineTracker['dataPrep'] {
  const v = (input || '').trim().toUpperCase().replace(/\s+/g, '_')
  if (v.includes('COMPLETE')) return 'COMPLETE'
  if (v.includes('IN_PROGRESS') || v === 'INPROGRESS') return 'IN_PROGRESS'
  if (v.includes('BLOCK')) return 'BLOCKED'
  if (v.includes('NOT_STARTED') || v === 'TODO' || v === 'PENDING') return 'NOT_STARTED'
  return 'NOT_STARTED'
}

function normalizeWorkStatus(input?: string): WorkItem['status'] {
  const v = (input || '').trim().toUpperCase().replace(/\s+/g, '_')
  if (v === 'DONE' || v === 'COMPLETED' || v === 'COMPLETE') return 'DONE'
  if (v === 'IN_PROGRESS' || v === 'INPROGRESS') return 'IN_PROGRESS'
  if (v === 'BLOCKED') return 'BLOCKED'
  if (v === 'TODO' || v === 'NOT_STARTED' || v === 'PENDING') return 'TODO'
  return 'TODO'
}

function extractSection(text: string, titlePattern: RegExp) {
  const lines = text.split(/\r?\n/)
  const start = lines.findIndex((line) => titlePattern.test(line.trim()))
  if (start < 0) return ''
  let end = lines.length
  for (let i = start + 1; i < lines.length; i++) {
    const t = lines[i].trim()
    if (/^[A-Z][A-Za-z0-9'() /&.-]{2,}:\s*$/.test(t) || /^\d+\.\s+[A-Z]/.test(t)) {
      end = i
      break
    }
  }
  return lines.slice(start + 1, end).join('\n')
}

function parseWorkItemsFromText(text: string): WorkItem[] {
  const section = extractSection(text, /work items?/i)
  if (!section) return []
  const out: WorkItem[] = []
  for (const raw of section.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line || (!line.startsWith('-') && !/^\d+[\).]/.test(line))) continue
    const name = line.match(/name\s*:\s*([^|,]+)/i)?.[1]?.trim() || line.match(/^\d+[\).]\s*([^|,]+)/)?.[1]?.trim()
    const storyId = line.match(/story\s*id\s*:\s*([^|,]+)/i)?.[1]?.trim()
    const taskDescription = line.match(/task\s*:\s*([^|,]+)/i)?.[1]?.trim()
    const statusRaw = line.match(/status\s*:\s*([^|,]+)/i)?.[1]?.trim()
    const ageDaysRaw = line.match(/age(?:\s*days?)?\s*:\s*([^|,]+)/i)?.[1]?.trim()
    const flag = line.match(/flag\s*:\s*([^|,]+)/i)?.[1]?.trim()
    if (!name && !storyId && !taskDescription) continue
    out.push({
      memberName: name || '',
      storyId: storyId || '',
      taskDescription: taskDescription || '',
      status: normalizeWorkStatus(statusRaw),
      ageDays: ageDaysRaw ? Number(ageDaysRaw) : undefined,
      flag: flag || '',
    })
  }
  return out
}

function parsePipelineFromText(text: string): PipelineTracker[] {
  const section = extractSection(text, /pipeline/i)
  if (!section) return []
  const out: PipelineTracker[] = []
  for (const raw of section.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line.startsWith('-')) continue
    const itemId = line.match(/^\-\s*([A-Za-z0-9._-]+)/)?.[1]?.trim() || ''
    const notes = line.match(/notes?\s*:\s*(.+)$/i)?.[1]?.trim()
    const dataPrep = normalizeStageStatus(line.match(/data\s*prep\s*([A-Z_ ]+)/i)?.[1])
    const modelDev = normalizeStageStatus(line.match(/model\s*dev\s*([A-Z_ ]+)/i)?.[1])
    const integration = normalizeStageStatus(line.match(/integration\s*([A-Z_ ]+)/i)?.[1])
    const validation = normalizeStageStatus(line.match(/validation\s*([A-Z_ ]+)/i)?.[1])
    const deployment = normalizeStageStatus(line.match(/deployment\s*([A-Z_ ]+)/i)?.[1])
    if (!itemId && !notes) continue
    out.push({ itemId, dataPrep, modelDev, integration, validation, deployment, notes: notes || '' })
  }
  return out
}

function parseBlockersFromText(text: string): BlockerDependency[] {
  const section = extractSection(text, /(blockers?|dependencies?)/i)
  if (!section) return []
  const out: BlockerDependency[] = []
  for (const raw of section.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line.startsWith('-')) continue
    const parts = line.replace(/^\-\s*/, '').split('|').map((p) => p.trim())
    const itemId = parts[0] || ''
    const type = /depend/i.test(parts[1] || '') ? 'DEPENDENCY' : 'BLOCKER'
    const description = parts[2] || line.match(/description\s*:\s*([^|]+)/i)?.[1]?.trim() || ''
    const owner = line.match(/owner\s*:\s*([^|]+)/i)?.[1]?.trim() || ''
    const actionNeeded = line.match(/action\s*needed\s*:\s*([^|]+)/i)?.[1]?.trim() || ''
    if (!itemId && !description) continue
    out.push({ itemId, type, description, owner, actionNeeded })
  }
  return out
}

function parseRisksFromText(text: string): RiskObservation[] {
  const section = extractSection(text, /risks?|observations?/i)
  if (!section) return []
  const out: RiskObservation[] = []
  for (const raw of section.split(/\r?\n/)) {
    const line = raw.trim().replace(/^\-\s*/, '')
    if (!line) continue
    if (/^(today focus|tomorrow outlook|decisions needed)/i.test(line)) break
    out.push({ description: line })
  }
  return out
}

function chooseFullerArray<T>(primary: T[], fallback: T[]): T[] {
  return fallback.length > primary.length ? fallback : primary
}

function aiToForm(raw: unknown): ReportRequest {
  const data = (raw || {}) as Partial<ReportRequest>
  return {
    ...defaultForm(),
    ...data,
    podName: data.podName || '',
    date: normalizeDate(data.date || new Date().toISOString().slice(0, 10)),
    sprintNumber: Number(data.sprintNumber) || 1,
    dayOfSprint: Number(data.dayOfSprint) || 1,
    pipelineTrackers: (data.pipelineTrackers || []).map((p) => ({
      ...p,
      itemId: p.itemId || '',
      dataPrep: normalizeEnum(p.dataPrep, stageStatuses, 'NOT_STARTED'),
      modelDev: normalizeEnum(p.modelDev, stageStatuses, 'NOT_STARTED'),
      integration: normalizeEnum(p.integration, stageStatuses, 'NOT_STARTED'),
      validation: normalizeEnum(p.validation, stageStatuses, 'NOT_STARTED'),
      deployment: normalizeEnum(p.deployment, stageStatuses, 'NOT_STARTED'),
    })),
    workItems: (data.workItems || []).map((w) => ({
      ...w,
      memberName: w.memberName || '',
      status: normalizeEnum(w.status, workStatuses, 'TODO'),
      ageDays: w.ageDays == null ? undefined : Number(w.ageDays),
    })),
    blockersDependencies: (data.blockersDependencies || []).map((b) => ({
      ...b,
      type: normalizeEnum(b.type, ['BLOCKER', 'DEPENDENCY'] as const, 'BLOCKER'),
    })),
    risksObservations: (data.risksObservations || []).map((r) => ({ description: r.description || '' })),
  }
}

export function ReportFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const [form, setForm] = useState<ReportRequest>(defaultForm())
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [analyzing, setAnalyzing] = useState(false)
  const [templateName, setTemplateName] = useState('')
  const [selectedTemplateId, setSelectedTemplateId] = useState('')
  const [customTemplates, setCustomTemplates] = useState<SavedTemplate[]>([])
  const [pasteText, setPasteText] = useState('')
  const [showInstructions, setShowInstructions] = useState(false)
  const navigate = useNavigate()
  const draftKey = useMemo(() => `daily-pod-draft-${id ?? 'new'}`, [id])

  const templateOptions = useMemo<TemplateOption[]>(
    () => [
      ...builtinTemplates(),
      ...customTemplates.map((t) => ({ id: t.id, name: t.name, payload: t.payload, source: 'custom' as const })),
    ],
    [customTemplates]
  )

  useEffect(() => {
    setCustomTemplates(readCustomTemplates())
  }, [])

  useEffect(() => {
    if (isEdit) return
    const draft = localStorage.getItem(draftKey)
    if (draft) {
      try {
        setForm(JSON.parse(draft))
      } catch {
        localStorage.removeItem(draftKey)
      }
    }
  }, [draftKey, isEdit])

  useEffect(() => {
    if (isEdit) return
    localStorage.setItem(draftKey, JSON.stringify(form))
  }, [form, draftKey, isEdit])

  useEffect(() => {
    if (!id) return
    getReport(id).then(setForm).catch(() => setError('Failed to load report'))
  }, [id])

  function applySelectedTemplate() {
    const selected = templateOptions.find((t) => t.id === selectedTemplateId)
    if (!selected) {
      setError('Select a template first.')
      return
    }
    setForm({ ...selected.payload, date: new Date().toISOString().slice(0, 10) })
    setMessage(`Template applied: ${selected.name}`)
    setError('')
  }

  async function createFromSelectedTemplate() {
    const selected = templateOptions.find((t) => t.id === selectedTemplateId)
    if (!selected) {
      setError('Select a template first.')
      return
    }
    try {
      const payload = sanitizePayload({ ...selected.payload, date: new Date().toISOString().slice(0, 10) })
      const saved = await createReport(payload)
      navigate(`/reports/${saved.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Template create failed.'))
    }
  }

  function saveCurrentAsTemplate() {
    const name = templateName.trim()
    if (!name) {
      setError('Template name is required.')
      return
    }
    const nextTemplate: SavedTemplate = {
      id: `custom-${Date.now()}`,
      name,
      payload: sanitizePayload(form),
      createdAt: new Date().toISOString(),
    }
    const next = [nextTemplate, ...customTemplates]
    setCustomTemplates(next)
    writeCustomTemplates(next)
    setTemplateName('')
    setSelectedTemplateId(nextTemplate.id)
    setMessage(`Template saved: ${name}`)
    setError('')
  }

  function deleteSelectedCustomTemplate() {
    const selected = templateOptions.find((t) => t.id === selectedTemplateId)
    if (!selected || selected.source !== 'custom') {
      setError('Select a custom template to delete.')
      return
    }
    const next = customTemplates.filter((t) => t.id !== selected.id)
    setCustomTemplates(next)
    writeCustomTemplates(next)
    setSelectedTemplateId('')
    setMessage(`Template deleted: ${selected.name}`)
    setError('')
  }

  async function analyzeAndAutofill() {
    const apiKey = (import.meta.env.VITE_GOOGLE_API_KEY || DEFAULT_GOOGLE_API_KEY).trim()
    if (!apiKey) {
      setError('Google API key is not configured.')
      return
    }
    if (!pasteText.trim()) {
      setError('Paste your source text first.')
      return
    }

    setAnalyzing(true)
    setError('')
    setMessage('')
    try {
      const schemaHint = {
        podName: 'string',
        date: 'yyyy-MM-dd',
        sprintNumber: 'number',
        dayOfSprint: 'number',
        goal: 'string',
        scrumMasterName: 'string',
        managerName: 'string',
        sprintBurndown: {
          totalStories: 'number',
          completed: 'number',
          inProgress: 'number',
          remainingSp: 'number',
          idealSp: 'number',
          variance: 'number',
          trend: 'string',
        },
        doraMetrics: {
          deploymentFrequencyToday: 'number',
          deploymentFrequencySprintAvg: 'number',
          deploymentFrequencyTarget: 'number',
          leadTimeToday: 'number',
          leadTimeAvg: 'number',
          leadTimeTarget: 'number',
          changeFailureRateToday: 'number',
          changeFailureRateAvg: 'number',
          changeFailureRateTarget: 'number',
          mttrToday: 'number',
          mttrAvg: 'number',
          mttrTarget: 'number',
        },
        pipelineTrackers: [{ itemId: 'string', dataPrep: 'COMPLETE|IN_PROGRESS|NOT_STARTED|BLOCKED', modelDev: 'COMPLETE|IN_PROGRESS|NOT_STARTED|BLOCKED', integration: 'COMPLETE|IN_PROGRESS|NOT_STARTED|BLOCKED', validation: 'COMPLETE|IN_PROGRESS|NOT_STARTED|BLOCKED', deployment: 'COMPLETE|IN_PROGRESS|NOT_STARTED|BLOCKED', notes: 'string' }],
        workItems: [{ memberName: 'string', storyId: 'string', taskDescription: 'string', status: 'DONE|IN_PROGRESS|TODO|BLOCKED', ageDays: 'number', flag: 'string' }],
        blockersDependencies: [{ itemId: 'string', type: 'BLOCKER|DEPENDENCY', description: 'string', owner: 'string', actionNeeded: 'string' }],
        risksObservations: [{ description: 'string' }],
        focusOutlook: { todayFocus: 'string', tomorrowOutlook: 'string', decisionsNeeded: 'string' },
      }

      const prompt = [
        'Extract a Daily POD report JSON from this text.',
        'Return ONLY JSON, no markdown.',
        'CRITICAL: include every row/item from input; do not summarize or limit list sizes.',
        'Use this schema:',
        JSON.stringify(schemaHint),
        'If fields are missing, keep them empty or omit optional values.',
        'Input:',
        pasteText,
      ].join('\n')

      let result:
        | {
            candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>
          }
        | null = null
      let lastError = 'Google API request failed.'

      for (const model of GEMINI_MODELS) {
        const response = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(apiKey)}`,
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: [{ role: 'user', parts: [{ text: prompt }] }],
              generationConfig: { temperature: 0.1 },
            }),
          }
        )

        if (response.ok) {
          result = (await response.json()) as {
            candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>
          }
          break
        }

        const txt = await response.text()
        const parsed = parseGoogleError(txt)
        lastError = parsed.message || 'Google API request failed.'
        const isQuota = response.status === 429 || parsed.status === 'RESOURCE_EXHAUSTED'

        if (!isQuota) {
          break
        }
      }

      if (!result) {
        if (lastError.includes('RESOURCE_EXHAUSTED') || lastError.toLowerCase().includes('quota')) {
          throw new Error('Google AI quota exceeded. Please try again later or increase your Gemini quota/billing.')
        }
        throw new Error(`Google API error: ${lastError}`)
      }

      const text = result.candidates?.[0]?.content?.parts?.map((p) => p.text || '').join('\n') || ''
      if (!text.trim()) {
        throw new Error('No analyzable content returned from model.')
      }
      const parsed = JSON.parse(extractJson(text))
      const aiForm = aiToForm(parsed)
      const nextForm: ReportRequest = {
        ...aiForm,
        pipelineTrackers: chooseFullerArray(aiForm.pipelineTrackers, parsePipelineFromText(pasteText)),
        workItems: chooseFullerArray(aiForm.workItems, parseWorkItemsFromText(pasteText)),
        blockersDependencies: chooseFullerArray(aiForm.blockersDependencies, parseBlockersFromText(pasteText)),
        risksObservations: chooseFullerArray(aiForm.risksObservations, parseRisksFromText(pasteText)),
      }
      setForm(nextForm)
      setMessage(
        `Data analyzed and autofilled successfully. Rows -> Pipeline: ${nextForm.pipelineTrackers.length}, Work: ${nextForm.workItems.length}, Blockers: ${nextForm.blockersDependencies.length}, Risks: ${nextForm.risksObservations.length}`
      )
    } catch (err) {
      setError(getApiErrorMessage(err, 'Failed to analyze and autofill.'))
    } finally {
      setAnalyzing(false)
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setMessage('')
    if (!form.podName || !form.date || !form.sprintNumber || !form.dayOfSprint) {
      setError('POD Name, Date, Sprint and Day are required.')
      return
    }

    const payload = sanitizePayload(form)
    setSaving(true)
    try {
      const saved = isEdit ? await updateReport(id!, payload) : await createReport(payload)
      localStorage.removeItem(draftKey)
      navigate(`/reports/${saved.id}`)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Save failed.'))
    } finally {
      setSaving(false)
    }
  }

  function updatePipeline(index: number, patch: Partial<PipelineTracker>) {
    const next = [...form.pipelineTrackers]
    next[index] = { ...next[index], ...patch }
    setForm({ ...form, pipelineTrackers: next })
  }

  function updateWork(index: number, patch: Partial<WorkItem>) {
    const next = [...form.workItems]
    next[index] = { ...next[index], ...patch }
    setForm({ ...form, workItems: next })
  }

  function updateBlocker(index: number, patch: Partial<BlockerDependency>) {
    const next = [...form.blockersDependencies]
    next[index] = { ...next[index], ...patch }
    setForm({ ...form, blockersDependencies: next })
  }

  function updateRisk(index: number, patch: Partial<RiskObservation>) {
    const next = [...form.risksObservations]
    next[index] = { ...next[index], ...patch }
    setForm({ ...form, risksObservations: next })
  }

  return (
    <div className="container py-6 space-y-4">
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 className="text-2xl font-semibold">{isEdit ? 'Edit Report' : 'Create Report'}</h1>
          <p className="text-sm text-gray-600">Paste your content once, auto-fill all cells, then create.</p>
        </div>
        <div className="flex gap-2">
          <Button type="button" className="bg-indigo-700" onClick={() => setShowInstructions((v) => !v)}>
            {showInstructions ? 'Hide Instructions' : 'Instructions'}
          </Button>
          <Link to="/history"><Button className="bg-slate-700">History</Button></Link>
        </div>
      </div>

      {error ? <div className="text-red-700 text-sm card p-3 border-red-200 bg-red-50">{error}</div> : null}
      {message ? <div className="text-emerald-800 text-sm card p-3 border-emerald-200 bg-emerald-50">{message}</div> : null}
      {showInstructions ? (
        <section className="card p-4 space-y-3 text-sm text-slate-700">
          <h2 className="font-semibold text-slate-900">How to Fill Cells</h2>
          <div className="grid md:grid-cols-2 gap-3">
            <div>
              <p className="font-medium text-slate-900">A. Basic Information</p>
              <p>`POD Name`, `Date`, `Sprint`, `Day` are required to create report.</p>
              <p>`Goal`, `Scrum Master Name`, `Manager Name` are shown in final DOC/PDF header/footer.</p>
            </div>
            <div>
              <p className="font-medium text-slate-900">B. Sprint Burndown</p>
              <p>Use integer values for `Total Stories`, `Completed`, `In Progress`, `Remaining SP`, `Ideal SP`, `Variance`.</p>
              <p>`Trend` is free text (example: `On track`, `Slightly behind`).</p>
            </div>
            <div>
              <p className="font-medium text-slate-900">C. DORA Metrics</p>
              <p>Fill numeric values for deployment frequency, lead time, change failure rate, and MTTR.</p>
            </div>
            <div>
              <p className="font-medium text-slate-900">D/E/F Tables</p>
              <p>`Add Row` to include more items; all rows are exported in DOC table.</p>
              <p>Delete with `X` only if you want to remove that row from output.</p>
            </div>
            <div>
              <p className="font-medium text-slate-900">G. Risks & Observations</p>
              <p>Each risk line is exported. Add one risk per row for clean formatting.</p>
            </div>
            <div>
              <p className="font-medium text-slate-900">H. Focus & Outlook</p>
              <p>`Today Focus`, `Tomorrow Outlook`, and `Decisions Needed` map directly to final document cells.</p>
            </div>
          </div>
          <div className="rounded-xl border border-slate-200 p-3 bg-slate-50">
            <p className="font-medium text-slate-900">Pipeline Status Meaning</p>
            <p><span className="font-semibold text-green-700">COMPLETE</span>: stage finished</p>
            <p><span className="font-semibold text-amber-700">IN_PROGRESS</span>: work currently active</p>
            <p><span className="font-semibold text-slate-600">NOT_STARTED</span>: not started yet</p>
            <p><span className="font-semibold text-red-700">BLOCKED</span>: blocked and needs action</p>
          </div>
        </section>
      ) : null}

      <section className="card p-4 space-y-3">
        <h2 className="font-semibold">AI Autofill</h2>
        <Textarea
          placeholder="Paste your daily updates, meeting notes, jira summary, or raw text here..."
          value={pasteText}
          onChange={(e) => setPasteText(e.target.value)}
          className="min-h-36"
        />
        <Button type="button" onClick={analyzeAndAutofill} disabled={analyzing}>
          {analyzing ? 'Analyzing...' : 'Analyze and Autofill'}
        </Button>
      </section>

      {!isEdit ? (
        <section className="card p-4 space-y-3">
          <h2 className="font-semibold">Create Templates</h2>
          <div className="grid md:grid-cols-4 gap-2">
            <select
              className="w-full border rounded-md p-2 bg-white"
              value={selectedTemplateId}
              onChange={(e) => setSelectedTemplateId(e.target.value)}
            >
              <option value="">Select template</option>
              {templateOptions.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} {t.source === 'custom' ? '(Custom)' : ''}
                </option>
              ))}
            </select>
            <Button type="button" className="bg-slate-700" onClick={applySelectedTemplate}>Apply Template</Button>
            <Button type="button" onClick={createFromSelectedTemplate}>Create from Template</Button>
            <Button type="button" className="bg-red-700" onClick={deleteSelectedCustomTemplate}>Delete Custom</Button>
          </div>
          <div className="grid md:grid-cols-4 gap-2">
            <Input placeholder="Custom template name" value={templateName} onChange={(e) => setTemplateName(e.target.value)} />
            <Button type="button" className="bg-emerald-700" onClick={saveCurrentAsTemplate}>Save Current as Template</Button>
          </div>
        </section>
      ) : null}

      <form onSubmit={onSubmit} className="space-y-5">
        <section className="card p-4 grid md:grid-cols-3 gap-3">
          <h2 className="md:col-span-3 font-semibold">A. Basic Information</h2>
          <Input placeholder="POD Name" value={form.podName} onChange={(e) => setForm({ ...form, podName: e.target.value })} />
          <Input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
          <Input type="number" placeholder="Sprint" value={form.sprintNumber} onChange={(e) => setForm({ ...form, sprintNumber: Number(e.target.value) })} />
          <Input type="number" placeholder="Day" value={form.dayOfSprint} onChange={(e) => setForm({ ...form, dayOfSprint: Number(e.target.value) })} />
          <Input placeholder="Scrum Master Name" value={form.scrumMasterName || ''} onChange={(e) => setForm({ ...form, scrumMasterName: e.target.value })} />
          <Input placeholder="Manager Name" value={form.managerName || ''} onChange={(e) => setForm({ ...form, managerName: e.target.value })} />
          <div className="md:col-span-3"><Textarea placeholder="Goal" value={form.goal || ''} onChange={(e) => setForm({ ...form, goal: e.target.value })} /></div>
        </section>

        <section className="card p-4 grid md:grid-cols-7 gap-2">
          <h2 className="md:col-span-7 font-semibold">B. Sprint Burndown Snapshot</h2>
          <Input placeholder="Total Stories" value={String(form.sprintBurndown?.totalStories ?? '')} onChange={(e) => setForm({ ...form, sprintBurndown: { ...form.sprintBurndown, totalStories: parseOptionalNumber(e.target.value) } })} />
          <Input placeholder="Completed" value={String(form.sprintBurndown?.completed ?? '')} onChange={(e) => setForm({ ...form, sprintBurndown: { ...form.sprintBurndown, completed: parseOptionalNumber(e.target.value) } })} />
          <Input placeholder="In Progress" value={String(form.sprintBurndown?.inProgress ?? '')} onChange={(e) => setForm({ ...form, sprintBurndown: { ...form.sprintBurndown, inProgress: parseOptionalNumber(e.target.value) } })} />
          <Input placeholder="Remaining SP" value={String(form.sprintBurndown?.remainingSp ?? '')} onChange={(e) => setForm({ ...form, sprintBurndown: { ...form.sprintBurndown, remainingSp: parseOptionalNumber(e.target.value) } })} />
          <Input placeholder="Ideal SP" value={String(form.sprintBurndown?.idealSp ?? '')} onChange={(e) => setForm({ ...form, sprintBurndown: { ...form.sprintBurndown, idealSp: parseOptionalNumber(e.target.value) } })} />
          <Input placeholder="Variance" value={String(form.sprintBurndown?.variance ?? '')} onChange={(e) => setForm({ ...form, sprintBurndown: { ...form.sprintBurndown, variance: parseOptionalNumber(e.target.value) } })} />
          <Input placeholder="Trend" value={form.sprintBurndown?.trend || ''} onChange={(e) => setForm({ ...form, sprintBurndown: { ...form.sprintBurndown, trend: e.target.value } })} />
        </section>

        <section className="card p-4 grid md:grid-cols-4 gap-2">
          <h2 className="md:col-span-4 font-semibold">C. DORA Metrics Table</h2>
          {[
            'deploymentFrequencyToday',
            'deploymentFrequencySprintAvg',
            'deploymentFrequencyTarget',
            'leadTimeToday',
            'leadTimeAvg',
            'leadTimeTarget',
            'changeFailureRateToday',
            'changeFailureRateAvg',
            'changeFailureRateTarget',
            'mttrToday',
            'mttrAvg',
            'mttrTarget',
          ].map((k) => (
            <Input
              key={k}
              placeholder={k}
              value={String((form.doraMetrics as Record<string, number | undefined>)?.[k] ?? '')}
              onChange={(e) => setForm({ ...form, doraMetrics: { ...form.doraMetrics, [k]: parseOptionalNumber(e.target.value) } })}
            />
          ))}
        </section>

        <section className="card p-4 space-y-2 overflow-auto">
          <h2 className="font-semibold">D. Pipeline Stage Tracker</h2>
          <table className="w-full text-sm">
            <thead><tr><th>Item ID</th><th>Data Prep</th><th>Model Dev</th><th>Integration</th><th>Validation</th><th>Deployment</th><th>Notes</th><th /></tr></thead>
            <tbody>
              {form.pipelineTrackers.map((p, i) => (
                <tr key={i} className="border-t">
                  <td><Input value={p.itemId} onChange={(e) => updatePipeline(i, { itemId: e.target.value })} /></td>
                  <td><select className="w-full border rounded-md p-2 bg-white" value={p.dataPrep || 'NOT_STARTED'} onChange={(e) => updatePipeline(i, { dataPrep: e.target.value as PipelineTracker['dataPrep'] })}>{stageStatuses.map(s => <option key={s}>{s}</option>)}</select></td>
                  <td><select className="w-full border rounded-md p-2 bg-white" value={p.modelDev || 'NOT_STARTED'} onChange={(e) => updatePipeline(i, { modelDev: e.target.value as PipelineTracker['modelDev'] })}>{stageStatuses.map(s => <option key={s}>{s}</option>)}</select></td>
                  <td><select className="w-full border rounded-md p-2 bg-white" value={p.integration || 'NOT_STARTED'} onChange={(e) => updatePipeline(i, { integration: e.target.value as PipelineTracker['integration'] })}>{stageStatuses.map(s => <option key={s}>{s}</option>)}</select></td>
                  <td><select className="w-full border rounded-md p-2 bg-white" value={p.validation || 'NOT_STARTED'} onChange={(e) => updatePipeline(i, { validation: e.target.value as PipelineTracker['validation'] })}>{stageStatuses.map(s => <option key={s}>{s}</option>)}</select></td>
                  <td><select className="w-full border rounded-md p-2 bg-white" value={p.deployment || 'NOT_STARTED'} onChange={(e) => updatePipeline(i, { deployment: e.target.value as PipelineTracker['deployment'] })}>{stageStatuses.map(s => <option key={s}>{s}</option>)}</select></td>
                  <td><Input value={p.notes || ''} onChange={(e) => updatePipeline(i, { notes: e.target.value })} /></td>
                  <td><Button type="button" className="bg-red-600" onClick={() => setForm({ ...form, pipelineTrackers: form.pipelineTrackers.filter((_, idx) => idx !== i) })}>X</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <Button type="button" onClick={() => setForm({ ...form, pipelineTrackers: [...form.pipelineTrackers, { itemId: '', dataPrep: 'NOT_STARTED', modelDev: 'NOT_STARTED', integration: 'NOT_STARTED', validation: 'NOT_STARTED', deployment: 'NOT_STARTED' }] })}>Add Row</Button>
        </section>

        <section className="card p-4 space-y-2 overflow-auto">
          <h2 className="font-semibold">E. Work Item Detail</h2>
          <table className="w-full text-sm">
            <thead><tr><th>Member</th><th>Story ID</th><th>Task</th><th>Status</th><th>Age</th><th>Flag</th><th /></tr></thead>
            <tbody>
              {form.workItems.map((w, i) => (
                <tr key={i} className="border-t">
                  <td><Input value={w.memberName} onChange={(e) => updateWork(i, { memberName: e.target.value })} /></td>
                  <td><Input value={w.storyId || ''} onChange={(e) => updateWork(i, { storyId: e.target.value })} /></td>
                  <td><Input value={w.taskDescription || ''} onChange={(e) => updateWork(i, { taskDescription: e.target.value })} /></td>
                  <td><select className="w-full border rounded-md p-2 bg-white" value={w.status || 'TODO'} onChange={(e) => updateWork(i, { status: e.target.value as WorkItem['status'] })}>{workStatuses.map(s => <option key={s}>{s}</option>)}</select></td>
                  <td><Input type="number" value={String(w.ageDays ?? '')} onChange={(e) => updateWork(i, { ageDays: parseOptionalNumber(e.target.value) })} /></td>
                  <td><Input value={w.flag || ''} onChange={(e) => updateWork(i, { flag: e.target.value })} /></td>
                  <td><Button type="button" className="bg-red-600" onClick={() => setForm({ ...form, workItems: form.workItems.filter((_, idx) => idx !== i) })}>X</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <Button type="button" onClick={() => setForm({ ...form, workItems: [...form.workItems, { memberName: '', status: 'TODO' }] })}>Add Row</Button>
        </section>

        <section className="card p-4 space-y-2 overflow-auto">
          <h2 className="font-semibold">F. Blockers and Dependencies</h2>
          <table className="w-full text-sm">
            <thead><tr><th>ID</th><th>Type</th><th>Description</th><th>Owner</th><th>Action Needed</th><th /></tr></thead>
            <tbody>
              {form.blockersDependencies.map((b, i) => (
                <tr key={i} className="border-t">
                  <td><Input value={b.itemId || ''} onChange={(e) => updateBlocker(i, { itemId: e.target.value })} /></td>
                  <td><select className="w-full border rounded-md p-2 bg-white" value={b.type || 'BLOCKER'} onChange={(e) => updateBlocker(i, { type: e.target.value as BlockerDependency['type'] })}><option>BLOCKER</option><option>DEPENDENCY</option></select></td>
                  <td><Input value={b.description || ''} onChange={(e) => updateBlocker(i, { description: e.target.value })} /></td>
                  <td><Input value={b.owner || ''} onChange={(e) => updateBlocker(i, { owner: e.target.value })} /></td>
                  <td><Input value={b.actionNeeded || ''} onChange={(e) => updateBlocker(i, { actionNeeded: e.target.value })} /></td>
                  <td><Button type="button" className="bg-red-600" onClick={() => setForm({ ...form, blockersDependencies: form.blockersDependencies.filter((_, idx) => idx !== i) })}>X</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <Button type="button" onClick={() => setForm({ ...form, blockersDependencies: [...form.blockersDependencies, { itemId: '', type: 'BLOCKER' }] })}>Add Row</Button>
        </section>

        <section className="card p-4 space-y-2">
          <h2 className="font-semibold">G. Risks & Observations</h2>
          {form.risksObservations.map((r, i) => (
            <div key={i} className="flex gap-2">
              <Textarea value={r.description || ''} onChange={(e) => updateRisk(i, { description: e.target.value })} />
              <Button type="button" className="bg-red-600 h-fit" onClick={() => setForm({ ...form, risksObservations: form.risksObservations.filter((_, idx) => idx !== i) })}>X</Button>
            </div>
          ))}
          <Button type="button" onClick={() => setForm({ ...form, risksObservations: [...form.risksObservations, { description: '' }] })}>Add Risk</Button>
        </section>

        <section className="card p-4 grid md:grid-cols-3 gap-3">
          <h2 className="md:col-span-3 font-semibold">H. Focus & Outlook</h2>
          <Textarea placeholder="Today's Focus" value={form.focusOutlook?.todayFocus || ''} onChange={(e) => setForm({ ...form, focusOutlook: { ...form.focusOutlook, todayFocus: e.target.value } })} />
          <Textarea placeholder="Tomorrow's Outlook" value={form.focusOutlook?.tomorrowOutlook || ''} onChange={(e) => setForm({ ...form, focusOutlook: { ...form.focusOutlook, tomorrowOutlook: e.target.value } })} />
          <Textarea placeholder="Decisions Needed" value={form.focusOutlook?.decisionsNeeded || ''} onChange={(e) => setForm({ ...form, focusOutlook: { ...form.focusOutlook, decisionsNeeded: e.target.value } })} />
        </section>

        <div className="sticky bottom-3 flex justify-end">
          <Button disabled={saving} type="submit" className="px-6 py-3 shadow-lg">
            {saving ? 'Saving...' : 'Create Report'}
          </Button>
        </div>
      </form>
    </div>
  )
}

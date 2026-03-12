import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { downloadDocx, downloadPdf, getReport, printableHtml } from '../lib/api'
import type { ReportResponse } from '../types/report'
import { Button } from '../components/ui/button'

export function ReportViewPage() {
  const { id } = useParams()
  const [report, setReport] = useState<ReportResponse | null>(null)
  const [downloading, setDownloading] = useState<'docx' | 'pdf' | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    getReport(id).then(setReport)
  }, [id])

  if (!report) return <div className="container py-8">Loading...</div>

  async function onDownloadDocx() {
    setError('')
    setDownloading('docx')
    try {
      await downloadDocx(report!.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to export DOCX')
    } finally {
      setDownloading(null)
    }
  }

  async function onDownloadPdf() {
    setError('')
    setDownloading('pdf')
    try {
      await downloadPdf(report!.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to export PDF')
    } finally {
      setDownloading(null)
    }
  }

  return (
    <div className="container py-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Daily POD Status Report</h1>
        <div className="space-x-2">
          <Link to="/history"><Button className="bg-gray-700">History</Button></Link>
          <Link to="/"><Button className="bg-slate-700">Create</Button></Link>
          <Link to={`/reports/${report.id}/edit`}><Button>Edit</Button></Link>
        </div>
      </div>

      <div className="card p-4 space-y-2">
        <div><b>POD:</b> {report.podName}</div>
        <div><b>Date:</b> {report.date}</div>
        <div><b>Sprint:</b> {report.sprintNumber} Day {report.dayOfSprint}</div>
        <div><b>Warning:</b> {report.warningText || '-'}</div>
        {error ? <div className="text-red-700 text-sm">{error}</div> : null}
      </div>

      <div className="flex flex-wrap gap-2">
        <Button onClick={onDownloadDocx} disabled={downloading !== null}>{downloading === 'docx' ? 'Preparing DOCX...' : 'Export DOCX'}</Button>
        <Button onClick={onDownloadPdf} disabled={downloading !== null}>{downloading === 'pdf' ? 'Preparing PDF...' : 'Export PDF'}</Button>
        <a href={printableHtml(report.id)} target="_blank" rel="noreferrer"><Button className="bg-gray-700">Printable HTML</Button></a>
      </div>
      <div className="text-sm text-gray-600">
        Exact template layout is preserved in DOCX export. PDF export is best-effort rendering.
      </div>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listReports } from '../lib/api'
import type { ReportResponse } from '../types/report'
import { Button } from '../components/ui/button'

export function HistoryPage() {
  const [reports, setReports] = useState<ReportResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const data = await listReports()
      setReports(data.sort((a, b) => b.id - a.id))
    } catch {
      setError('Failed to load history.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  return (
    <div className="container py-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">History</h1>
        <Link to="/"><Button>Create</Button></Link>
      </div>

      {error ? <div className="text-red-700 text-sm card p-3 border-red-200 bg-red-50">{error}</div> : null}
      {loading ? <div className="card p-4 text-sm">Loading...</div> : null}

      {!loading ? (
        <div className="card overflow-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-100/80">
              <tr>
                <th className="text-left p-3">ID</th>
                <th className="text-left p-3">Date</th>
                <th className="text-left p-3">POD</th>
                <th className="text-left p-3">Sprint</th>
                <th className="text-left p-3">Version</th>
                <th className="text-left p-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id} className="border-t hover:bg-slate-50/70">
                  <td className="p-3">{r.id}</td>
                  <td className="p-3">{r.date}</td>
                  <td className="p-3">{r.podName}</td>
                  <td className="p-3">{r.sprintNumber}</td>
                  <td className="p-3">{r.versionNumber}</td>
                  <td className="p-3 space-x-3">
                    <Link to={`/reports/${r.id}`} className="text-accent">View</Link>
                    <Link to={`/reports/${r.id}/edit`} className="text-accent">Edit</Link>
                  </td>
                </tr>
              ))}
              {reports.length === 0 ? (
                <tr>
                  <td className="p-4 text-gray-600" colSpan={6}>No reports yet.</td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}

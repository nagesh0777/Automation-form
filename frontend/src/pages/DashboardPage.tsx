import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listReports } from '../lib/api'
import type { ReportResponse } from '../types/report'
import { Button } from '../components/ui/button'
import { Input } from '../components/ui/input'

export function DashboardPage() {
  const [reports, setReports] = useState<ReportResponse[]>([])
  const [sprint, setSprint] = useState('')
  const [date, setDate] = useState('')

  async function load() {
    const data = await listReports({
      sprint: sprint ? Number(sprint) : undefined,
      date: date || undefined,
    })
    setReports(data)
  }

  useEffect(() => { load() }, [])

  return (
    <div className="container py-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Reports Dashboard</h1>
        <div className="space-x-2">
          <Link to="/reports/new"><Button>Create Report</Button></Link>
        </div>
      </div>

      <div className="card p-4 grid md:grid-cols-4 gap-3">
        <Input placeholder="Sprint #" value={sprint} onChange={(e) => setSprint(e.target.value)} />
        <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <Button onClick={load}>Apply Filters</Button>
      </div>

      <div className="card overflow-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-100">
            <tr>
              <th className="text-left p-3">Date</th>
              <th className="text-left p-3">POD</th>
              <th className="text-left p-3">Sprint</th>
              <th className="text-left p-3">Version</th>
              <th className="text-left p-3">Warning</th>
              <th className="text-left p-3">Action</th>
            </tr>
          </thead>
          <tbody>
            {reports.map((r) => (
              <tr key={r.id} className="border-t">
                <td className="p-3">{r.date}</td>
                <td className="p-3">{r.podName}</td>
                <td className="p-3">{r.sprintNumber}</td>
                <td className="p-3">{r.versionNumber}</td>
                <td className="p-3 text-amber-700">{r.warningText || '-'}</td>
                <td className="p-3 space-x-2">
                  <Link to={`/reports/${r.id}`} className="text-accent">View</Link>
                  <Link to={`/reports/${r.id}/edit`} className="text-accent">Edit</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

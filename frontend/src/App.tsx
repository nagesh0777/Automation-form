import { Route, Routes } from 'react-router-dom'
import { ReportFormPage } from './pages/ReportFormPage'
import { ReportViewPage } from './pages/ReportViewPage'
import { HistoryPage } from './pages/HistoryPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<ReportFormPage />} />
      <Route path="/history" element={<HistoryPage />} />
      <Route path="/reports/new" element={<ReportFormPage />} />
      <Route path="/reports/:id/edit" element={<ReportFormPage />} />
      <Route path="/reports/:id" element={<ReportViewPage />} />
    </Routes>
  )
}

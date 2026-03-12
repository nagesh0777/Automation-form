import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../lib/api'
import { Button } from '../components/ui/button'
import { Input } from '../components/ui/input'

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    try {
      const auth = await login(email, password)
      localStorage.setItem('token', auth.token)
      localStorage.setItem('role', auth.role)
      localStorage.setItem('name', auth.name)
      navigate('/')
    } catch {
      setError('Invalid credentials')
    }
  }

  return (
    <div className="min-h-screen grid place-items-center p-4">
      <form className="card w-full max-w-md p-6 space-y-3" onSubmit={handleSubmit}>
        <h1 className="text-2xl font-semibold">Daily POD Status</h1>
        <p className="text-sm text-gray-600">Sign in to continue</p>
        {error ? <div className="text-red-600 text-sm">{error}</div> : null}
        <Input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <Input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <Button type="submit" className="w-full">Login</Button>
      </form>
    </div>
  )
}

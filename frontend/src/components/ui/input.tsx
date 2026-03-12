import { cn } from './utils'
import type { InputHTMLAttributes } from 'react'

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'w-full px-3 py-2 rounded-xl border border-slate-300/90 bg-white/95 shadow-inner',
        className
      )}
      {...props}
    />
  )
}

import { cn } from './utils'
import type { ButtonHTMLAttributes } from 'react'

export function Button({ className, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      className={cn(
        'px-4 py-2 rounded-xl bg-accent text-white font-medium tracking-[0.01em] shadow-[0_8px_18px_rgba(30,64,175,0.26)] hover:brightness-110 active:translate-y-[1px] disabled:opacity-50 disabled:cursor-not-allowed transition',
        className
      )}
      {...props}
    />
  )
}

import type { InputHTMLAttributes } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: string
}

/** Labelled input with an inline error message (accessible: label + aria-invalid + aria-describedby). */
export default function FormField({ label, error, hint, id, ...input }: Props) {
  const fieldId = id ?? input.name
  const describedBy = error ? `${fieldId}-error` : hint ? `${fieldId}-hint` : undefined
  return (
    <div className="space-y-1">
      <label htmlFor={fieldId} className="block text-xs font-medium text-stone-300">
        {label}
      </label>
      <input
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`w-full rounded-md bg-surface px-3 py-2 text-sm outline-none ring-1 focus:ring-accent ${
          error ? 'ring-red-500' : 'ring-surface-3'
        }`}
        {...input}
      />
      {error ? (
        <p id={`${fieldId}-error`} className="text-xs text-red-400">{error}</p>
      ) : hint ? (
        <p id={`${fieldId}-hint`} className="text-xs text-stone-500">{hint}</p>
      ) : null}
    </div>
  )
}

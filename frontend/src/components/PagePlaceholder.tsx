import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

interface PagePlaceholderProps {
  title: string
  description?: string
  children?: ReactNode
}

export default function PagePlaceholder({
  title,
  description,
  children,
}: PagePlaceholderProps) {
  return (
    <main className="page">
      <h1>{title}</h1>
      <p>
        {description ??
          'This section belongs to a later roadmap stage and is not implemented yet.'}
      </p>
      {children}
      <p>
        <Link to="/dashboard">← Back to dashboard</Link>
      </p>
    </main>
  )
}

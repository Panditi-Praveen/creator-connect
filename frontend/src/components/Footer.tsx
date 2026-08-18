import { Link } from 'react-router-dom'

const FOOTER_LINKS = [
  { to: '/', label: 'Home' },
  { to: '/projects', label: 'Projects' },
  { to: '/ai', label: 'AI Discovery' },
  { to: '/profile', label: 'Profile' },
  { to: '/hiring', label: 'Hiring' },
  { to: '/reviews', label: 'Reviews' },
]

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer-inner">
        <div className="footer-brand">
          <span className="brand-mark" aria-hidden="true">
            CC
          </span>
          <div>
            <div className="footer-name">CreatorConnect</div>
            <div className="footer-tagline">Find work. Find talent. Build together.</div>
          </div>
        </div>
        <nav className="footer-links" aria-label="Footer">
          {FOOTER_LINKS.map((link) => (
            <Link key={link.to} to={link.to}>
              {link.label}
            </Link>
          ))}
        </nav>
        <div className="footer-copy">
          © {new Date().getFullYear()} CreatorConnect · v1.0.0
        </div>
      </div>
    </footer>
  )
}

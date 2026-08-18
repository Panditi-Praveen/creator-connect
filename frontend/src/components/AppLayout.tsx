import { useLocation } from 'react-router-dom'
import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'

export default function AppLayout() {
  const location = useLocation()

  return (
    <>
      <Navbar />
      {/* Keyed by pathname so every route change replays the entrance animation. */}
      <div className="page-anim" key={location.pathname}>
        <Outlet />
      </div>
    </>
  )
}

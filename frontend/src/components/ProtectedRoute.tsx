import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/authContext'
import Spinner from './Spinner'

/**
 * Wraps pages that need a logged-in user. This is only UX: the real protection is on the
 * backend (every /api call needs a valid JWT). Never rely on the frontend for security.
 */
export default function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <Spinner label="Se verifică sesiunea…" />
  }
  if (status === 'anonymous') {
    // remember where the user wanted to go, so login can send them back there
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

import { createBrowserRouter } from 'react-router'
import AppLayout from './components/AppLayout'
import ProtectedRoute from './components/ProtectedRoute'
import HomePage from './pages/HomePage'
import DeckbuilderPage from './pages/DeckbuilderPage'
import CollectionPage from './pages/CollectionPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import LegalPage from './pages/LegalPage'
import NotFoundPage from './pages/NotFoundPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'legal/:doc', element: <LegalPage /> },
      {
        // everything below requires a logged-in user
        element: <ProtectedRoute />,
        children: [
          { path: 'deckbuilder', element: <DeckbuilderPage /> },
          { path: 'collection', element: <CollectionPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])

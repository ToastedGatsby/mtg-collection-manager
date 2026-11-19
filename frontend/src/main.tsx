import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { router } from './router'
import AuthProvider from './auth/AuthProvider'
// Fonts are bundled with the app (no request to Google Fonts: no visitor IP sent to a third party).
import '@fontsource/uncial-antiqua/400.css'
import '@fontsource/alegreya/400.css'
import '@fontsource/alegreya/500.css'
import '@fontsource/alegreya/700.css'
import '@fontsource/alegreya/400-italic.css'
import '@fontsource/alegreya-sans/400.css'
import '@fontsource/alegreya-sans/500.css'
import '@fontsource/alegreya-sans/700.css'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 60_000, retry: 1 },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)

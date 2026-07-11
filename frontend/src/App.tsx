import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthContext'
import { LandingPage } from './pages/LandingPage'
import { PartyLobbyPage } from './pages/PartyLobbyPage'
import { JoinPage } from './pages/JoinPage'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/party/:code" element={<PartyLobbyPage />} />
          <Route path="/join/:code" element={<JoinPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App

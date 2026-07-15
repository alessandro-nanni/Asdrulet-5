import {BrowserRouter, Route, Routes} from 'react-router-dom'
import {LandingPage} from './pages/LandingPage'
import {PartyLobbyPage} from './pages/PartyLobbyPage'
import {JoinPage} from './pages/JoinPage'

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<LandingPage/>}/>
                <Route path="/party/:code" element={<PartyLobbyPage/>}/>
                <Route path="/join/:code" element={<JoinPage/>}/>
            </Routes>
        </BrowserRouter>
    )
}

export default App

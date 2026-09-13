import { useEffect, useState } from 'react';
import Navbar from './components/Navbar';
import ReservationModal from './components/ReservationModal';
import EventsPage from './pages/EventsPage';
import LoginPage from './pages/LoginPage';
import OrdersPage from './pages/OrdersPage';
import PaymentsPage from './pages/PaymentsPage';
import EntriesPage from './pages/EntriesPage';
import AdminPage from './pages/AdminPage';
import { AdminProvider } from './contexts/AdminContext';
import { clearSession, getSession, isAdminUser, saveSession } from './services/api';

function App() {
  const [session, setSession] = useState(getSession());
  const [locationHash, setLocationHash] = useState(window.location.hash.slice(1) || 'eventos');
  const [selectedEvent, setSelectedEvent] = useState(null);

  useEffect(() => {
    const handleHashChange = () => setLocationHash(window.location.hash.slice(1) || 'eventos');
    const handleAuthExpired = () => {
      setSession(getSession());
      window.location.hash = 'login';
    };
    const consumeOAuthCallback = () => {
      const [callbackRoute, callbackQuery = ''] = window.location.hash.slice(1).split('?');
      if (callbackRoute !== 'oauth2') return;
      const params = new URLSearchParams(callbackQuery);
      const token = params.get('token');
      if (!token) return;
      saveSession({ token, user: { idUsuario: params.get('idUsuario'), email: params.get('email'), name: params.get('name') } });
      setSession(getSession());
      window.location.hash = 'eventos';
    };
    consumeOAuthCallback();
    window.addEventListener('hashchange', handleHashChange);
    window.addEventListener('goticket-auth-expired', handleAuthExpired);
    return () => {
      window.removeEventListener('hashchange', handleHashChange);
      window.removeEventListener('goticket-auth-expired', handleAuthExpired);
    };
  }, []);

  const [route, query = ''] = locationHash.split('?');
  const category = new URLSearchParams(query).get('categoria') || 'todos';
  const admin = isAdminUser(session.user);

  function handleLogout() {
    clearSession();
    setSession(getSession());
    window.location.hash = 'login';
  }

  function handleReserve(event) {
    setSelectedEvent(event);
    if (!session.token) window.location.hash = 'login';
  }

  function handleLogin() {
    window.location.hash = 'login';
  }

  if (route === 'admin' && !admin) {
    window.location.hash = 'eventos';
    return null;
  }

  if (route === 'login' || (!session.token && (route === 'ordenes' || route === 'pagos' || route === 'entradas' || route === 'admin'))) {
    return <LoginPage onLogin={() => { setSession(getSession()); window.location.hash = 'eventos'; }} />;
  }

  const page = route === 'ordenes'
    ? <OrdersPage />
    : route === 'pagos'
      ? <PaymentsPage />
        : route === 'entradas'
          ? <EntriesPage />
        : route === 'admin'
          ? <AdminPage />
        : <EventsPage category={category} onReserve={handleReserve} />;

      const shell = <div className="app-shell"><Navbar currentRoute={route} currentCategory={category} user={session.user} onLogout={handleLogout} onLogin={handleLogin} /><main>{page}</main>{selectedEvent && session.token && <ReservationModal event={selectedEvent} user={session.user} onClose={() => setSelectedEvent(null)} />}</div>;
      return route === 'admin' ? <AdminProvider>{shell}</AdminProvider> : shell;
}

export default App;

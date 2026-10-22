import { useEffect, useState } from 'react';
import { isAdminUser } from '../services/api';

const links = [
  { href: '#eventos', label: 'Todos' },
  { href: '#eventos?categoria=musica', label: 'Música' },
  { href: '#eventos?categoria=deportes', label: 'Deportes' },
  { href: '#eventos?categoria=teatro', label: 'Teatro' },
  { href: '#eventos?categoria=familia', label: 'Familia' },
  { href: '#eventos?categoria=especiales', label: 'Especiales' },
  { href: '#ordenes', label: 'Mis órdenes' },
  { href: '#pagos', label: 'Pagos' },
  { href: '#entradas', label: 'Mis entradas' },
  { href: '#perfil', label: 'Mi perfil' }
];

const utilityLinks = links.slice(6);

export default function Navbar({ currentRoute, currentCategory, initialSearch = '', user, onLogout, onLogin }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [categoryMenuOpen, setCategoryMenuOpen] = useState(false);
  const [searchText, setSearchText] = useState(initialSearch);
  const canAccessAdmin = isAdminUser(user);

  useEffect(() => setSearchText(initialSearch), [initialSearch]);

  function navigate(href) {
    window.location.hash = href.slice(1);
    setMenuOpen(false);
  }

  function searchEvents(event) {
    event.preventDefault();
    const params = new URLSearchParams();
    if (currentCategory && currentCategory !== 'todos') params.set('categoria', currentCategory);
    if (searchText.trim()) params.set('busqueda', searchText.trim());
    const queryString = params.toString();
    window.location.hash = queryString ? `eventos?${queryString}` : 'eventos';
  }

  return <header className="topbar app-navbar">
      <button className="brand brand-button" onClick={() => navigate('#eventos')} aria-label="Ir a eventos">
        <img className="brand-logo" src="/GOTICKET LOGO.png" alt="GoTicket" />
      </button>
      <button className="category-trigger" onClick={() => { setCategoryMenuOpen((open) => !open); if (currentRoute !== 'eventos') window.location.hash = 'eventos'; }} aria-expanded={categoryMenuOpen} aria-controls="category-menu" aria-label="Abrir categorías">
        ☰ <strong>Categorías</strong>
      </button>
      {categoryMenuOpen && <div className="category-menu" id="category-menu" role="menu">
        {links.slice(0, 6).map((link) => <a key={link.href} href={link.href} role="menuitem" onClick={() => setCategoryMenuOpen(false)}>{link.label}</a>)}
      </div>}
      <form className="search-box" onSubmit={searchEvents}>
        <input aria-label="Buscar eventos" placeholder="Busca por artista, evento o lugar" value={searchText} onChange={(event) => setSearchText(event.target.value)} />
        <button type="submit" aria-label="Buscar">⌕</button>
      </form>
    <button className="menu-button" onClick={() => setMenuOpen((open) => !open)} aria-expanded={menuOpen} aria-label="Abrir navegación">
      {menuOpen ? 'Cerrar' : 'Menú'}
    </button>
    <nav className={`main-nav ${menuOpen ? 'is-open' : ''}`} aria-label="Navegación principal">
      {utilityLinks.map((link) => <a key={link.href} href={link.href} className={currentRoute === link.href.slice(1) ? 'active' : ''} onClick={() => setMenuOpen(false)}>{link.label}</a>)}
      {canAccessAdmin && <><a href="#admin" className={currentRoute === 'admin' ? 'active' : ''} onClick={() => setMenuOpen(false)}>Admin</a><a href="#acceso" className={currentRoute === 'acceso' ? 'active' : ''} onClick={() => setMenuOpen(false)}>Control de acceso</a></>}
      {user ? <><span className="nav-user">{user?.nombre || user?.email || 'Usuario'}</span><button className="logout-button" onClick={onLogout}>Salir</button></> : <button className="login-button" onClick={onLogin}>Iniciar sesión</button>}
    </nav>
  </header>;
}
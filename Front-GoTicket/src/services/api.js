const API_MANAGER_URL = import.meta.env.VITE_API_MANAGER_URL ?? '';
const DEFAULT_API_KEY = import.meta.env.VITE_API_KEY || 'duoc-demo-key-2026';

function deriveUserId(email) {
  if (!email || typeof email !== 'string') return '';
  return String(Math.abs(email.trim().toLowerCase().split('').reduce((total, char) => ((total * 31) + char.charCodeAt(0)) >>> 0, 0)));
}

function normalizeUser(user) {
  if (!user) return null;
  const normalized = { ...user };
  const candidateId = normalized.idUsuario ?? normalized.id ?? normalized.id_usuario ?? (normalized.email ? deriveUserId(normalized.email) : '');
  if (candidateId) {
    normalized.idUsuario = String(candidateId);
  }
  return normalized;
}

export function isAdminUser(user) {
  return user?.email?.trim().toLowerCase().endsWith('@duocuc.cl') || false;
}

export function getSession() {
  return {
    token: localStorage.getItem('goticket_token') || '',
    apiKey: localStorage.getItem('goticket_api_key') || DEFAULT_API_KEY,
    user: normalizeUser(JSON.parse(localStorage.getItem('goticket_user') || 'null'))
  };
}

export function saveSession(data) {
  const safeUser = normalizeUser(data.user);
  localStorage.setItem('goticket_token', data.token);
  localStorage.setItem('goticket_user', JSON.stringify(safeUser));
}

export function clearSession() {
  localStorage.removeItem('goticket_token');
  localStorage.removeItem('goticket_user');
}

async function request(path, options = {}) {
  const session = getSession();
  const headers = new Headers(options.headers || {});
  headers.set('Accept', 'application/json');
  headers.set('Content-Type', 'application/json');
  headers.set('X-API-KEY', session.apiKey);
  if (session.token) headers.set('Authorization', `Bearer ${session.token}`);

  const response = await fetch(`${API_MANAGER_URL}${path}`, { ...options, headers });
  const rawText = await response.text();
  let body = null;

  if (rawText) {
    try {
      body = JSON.parse(rawText);
    } catch {
      body = rawText;
    }
  }

  if (!response.ok) {
    if ((response.status === 401 || response.status === 403) && path !== '/api/auth/google') {
      clearSession();
      window.dispatchEvent(new Event('goticket-auth-expired'));
    }
    const message = body && typeof body === 'object' ? body.message || body.error || body.detail : body;
    throw new Error(response.status === 403 ? 'Tu sesión expiró. Vuelve a iniciar sesión.' : (message || `La solicitud falló (${response.status})`));
  }
  return body;
}

async function download(path) {
  const session = getSession();
  const headers = new Headers({ Accept: 'application/pdf', 'X-API-KEY': session.apiKey });
  if (session.token) headers.set('Authorization', `Bearer ${session.token}`);
  const response = await fetch(`${API_MANAGER_URL}${path}`, { headers });
  if (!response.ok) {
    const text = await response.text();
    let body;
    try { body = text ? JSON.parse(text) : null; } catch { body = text; }
    if ((response.status === 401 || response.status === 403)) {
      clearSession();
      window.dispatchEvent(new Event('goticket-auth-expired'));
    }
    const message = body && typeof body === 'object' ? body.message || body.error || body.detail : body;
    throw new Error(response.status === 403 ? 'Tu sesión expiró. Vuelve a iniciar sesión.' : (message || `La solicitud falló (${response.status})`));
  }
  return response.blob();
}

export const api = {
  login: (token) => request('/api/auth/google', { method: 'POST', body: JSON.stringify({ token }) }),
  profile: () => request('/api/auth/profile'),
  updateProfile: (profile) => request('/api/auth/profile', { method: 'PUT', body: JSON.stringify(profile) }),
  events: () => request('/api/proxy/eventos'),
  event: (id) => request(`/api/proxy/eventos/${id}`),
  venues: () => request('/api/proxy/recintos'),
  createVenue: (venue) => request('/api/proxy/recintos', { method: 'POST', body: JSON.stringify(venue) }),
  updateVenue: (id, venue) => request(`/api/proxy/recintos/${id}`, { method: 'PUT', body: JSON.stringify(venue) }),
  deleteVenue: (id) => request(`/api/proxy/recintos/${id}`, { method: 'DELETE' }),
  venueSectors: (id) => request(`/api/proxy/recintos/${id}/sectores`),
  configureVenueSectors: (id, sectors) => request(`/api/proxy/recintos/${id}/sectores`, {
    method: 'PUT', body: JSON.stringify(sectors)
  }),
  eventSectors: (id) => request(`/api/proxy/eventos/${id}/sectores`),
  configureEventSectors: (id, sectors) => request(`/api/proxy/eventos/${id}/sectores`, {
    method: 'POST', body: JSON.stringify(sectors)
  }),
  searchEvents: (filters = {}) => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') params.set(key, value);
    });
    return request(`/api/proxy/eventos/buscar?${params.toString()}`);
  },
  inventory: () => request('/api/proxy/inventarios'),
  inventoryByEvent: (eventId) => request(`/api/proxy/inventarios/evento/${eventId}`),
  inventorySectors: (eventId) => request(`/api/proxy/inventarios/evento/${eventId}/sectores`),
  createEvent: (event) => request('/api/proxy/eventos', { method: 'POST', body: JSON.stringify(event) }),
  updateEvent: (id, event) => request(`/api/proxy/eventos/${id}`, { method: 'PUT', body: JSON.stringify(event) }),
  deleteEvent: (id) => request(`/api/proxy/eventos/${id}`, { method: 'DELETE' }),
  createInventory: (inventory) => request('/api/proxy/inventarios', { method: 'POST', body: JSON.stringify(inventory) }),
  updateInventoryByEvent: (eventId, inventory) => request(`/api/proxy/inventarios/evento/${eventId}`, { method: 'PUT', body: JSON.stringify(inventory) }),
  deleteInventoryByEvent: (eventId) => request(`/api/proxy/inventarios/evento/${eventId}`, { method: 'DELETE' }),
  reserveEntries: (reservation) => request('/api/proxy/inventarios/reservas', { method: 'POST', body: JSON.stringify(reservation) }),
  confirmReservation: (reservation) => request('/api/proxy/inventarios/reservas/confirmar', { method: 'POST', body: JSON.stringify(reservation) }),
  userEntries: (userId) => request(`/api/proxy/inventarios/usuario/${userId}/entradas`),
  ticketPdf: (userId, entryId) => download(`/api/proxy/inventarios/usuario/${userId}/entradas/${entryId}/pdf`),
  resaleEntries: () => request('/api/proxy/inventarios/reventa'),
  publishResale: (idEntrada, resale) => request(`/api/proxy/inventarios/entradas/${idEntrada}/reventa`, { method: 'POST', body: JSON.stringify(resale) }),
  reserveResale: (idEntrada, reservation) => request(`/api/proxy/inventarios/entradas/${idEntrada}/reventa/reservar`, { method: 'POST', body: JSON.stringify(reservation) }),
  validateAccess: (codigoAcceso) => request('/api/proxy/inventarios/acceso/validar', { method: 'POST', body: JSON.stringify({ codigoAcceso }) }),
  orders: (userId) => request(userId ? `/api/proxy/ordenes/usuario/${userId}` : '/api/proxy/ordenes'),
  payments: (userId) => request(userId ? `/api/proxy/pagos/usuario/${userId}` : '/api/proxy/pagos'),
  createOrder: (order) => request('/api/proxy/ordenes', { method: 'POST', body: JSON.stringify(order) }),
  checkout: (checkout) => request('/api/proxy/ordenes/checkout', { method: 'POST', body: JSON.stringify(checkout) }),
  createPayment: (payment) => request('/api/proxy/pagos', { method: 'POST', body: JSON.stringify(payment) }),
  simulatePayment: (id, approve, userId, paymentDetails = {}) => request(`/api/proxy/pagos/${id}/simular`, {
    method: 'POST',
    body: JSON.stringify({ aprobar: approve, idUsuario: userId, ...paymentDetails })
  }),
  updatePayment: (id, payment) => request(`/api/proxy/pagos/${id}`, { method: 'PUT', body: JSON.stringify(payment) })
};

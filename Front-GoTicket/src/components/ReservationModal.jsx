import { useEffect, useState } from 'react';
import { api } from '../services/api';

const arenaSectors = [
  { name: 'Tribuna', multiplier: 0.65, capacity: 2500, color: '#13d5e6' },
  { name: 'Platea Alta', multiplier: 1, capacity: 2200, color: '#702bd8' },
  { name: 'Platea Baja', multiplier: 1.35, capacity: 1800, color: '#00bf4f' },
  { name: 'Cancha General', multiplier: 1.6, capacity: 3200, color: '#ff9418' },
  { name: 'Platea Baja VIP', multiplier: 2, capacity: 900, color: '#f51b8c' }
];

const matchSectors = [
  { name: 'Rapa Nui', side: 'LOCAL', multiplier: 1, capacity: 1800, color: '#f6c344' },
  { name: 'Oceano', side: 'VISITANTE', multiplier: 0.72, capacity: 2400, color: '#f58220' },
  { name: 'Cordillera', side: 'LOCAL', multiplier: 0.52, capacity: 3000, color: '#e92d55' },
  { name: 'Caupolicán', side: 'VISITANTE', multiplier: 0.38, capacity: 2600, color: '#9272b4' },
  { name: 'Tucapel', side: 'LOCAL', multiplier: 0.38, capacity: 2600, color: '#a6379e' },
  { name: 'Galvarino', side: 'VISITANTE', multiplier: 0.38, capacity: 2200, color: '#4262a4' },
  { name: 'Arica', side: 'LOCAL', multiplier: 0.38, capacity: 2200, color: '#9bd8d5' },
  { name: 'Magallanes', side: 'VISITANTE', multiplier: 0.38, capacity: 2200, color: '#54bba9' }
];

function getMatchTeams(eventName) {
  const name = String(eventName || '').trim();
  const match = name.match(/^(.+?)\s+(?:vs\.?|v\.?|contra)\s+(.+)$/i)
    || name.match(/^(.+?)\s+-\s+(.+)$/i);
  if (!match) return { local: name, visitante: 'Rival por confirmar' };
  return { local: match[1].trim(), visitante: match[2].trim() };
}

export default function ReservationModal({ event, user, onClose, onCreated }) {
  const [quantity, setQuantity] = useState(1);
  const hasArenaSectors = event.lugar?.toLowerCase().includes('santander arena');
  const isMatch = /f[uú]tbol|partido|colo-colo|deportes concepci[oó]n/i.test(`${event.categoria || ''} ${event.nombre || ''}`);
  const sectors = isMatch ? matchSectors : arenaSectors;
  const matchTeams = isMatch ? getMatchTeams(event.nombre) : null;
  const [selectedSector, setSelectedSector] = useState((hasArenaSectors || isMatch) ? sectors[1] : null);
  const [availability, setAvailability] = useState(null);
  const [sectorAvailability, setSectorAvailability] = useState([]);
  const [state, setState] = useState({ loading: false, loadingAvailability: true, error: '', success: false });
  const userId = user?.idUsuario || user?.id || user?.id_usuario;
  const unitPrice = Number(event.precio || 0) * (selectedSector?.multiplier || 1);
  const total = unitPrice * quantity;
  const formatEventDate = (value) => value ? new Date(value).toLocaleString('es-CL', {
    weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit'
  }) : 'Fecha por confirmar';

  useEffect(() => {
    Promise.all([api.inventoryByEvent(event.idEvento), api.inventorySectors(event.idEvento)])
      .then(([inventory, availableSectors]) => {
        setAvailability(Number(inventory?.cantidadDisponible ?? 0));
        setSectorAvailability(availableSectors || []);
      })
      .catch((error) => setState((current) => ({ ...current, error: `No se pudo consultar la disponibilidad: ${error.message}` })))
      .finally(() => setState((current) => ({ ...current, loadingAvailability: false })));
  }, [event.idEvento]);

  const availableForSector = (sector) => {
    if (availability === null) return 0;
    if (!sector) return availability;
    return Number(sectorAvailability.find((item) => item.sector === sector.name)?.cantidadDisponible ?? 0);
  };
  const availabilityLabel = (sector) => {
    const available = availableForSector(sector);
    if (state.loadingAvailability) return 'Consultando disponibilidad...';
    if (available === 0) return 'Agotado';
    if (available <= Math.max(20, sector.capacity * 0.1)) return `Últimas ${available.toLocaleString('es-CL')} entradas`;
    return `${available.toLocaleString('es-CL')} entradas disponibles`;
  };

  async function handleSubmit(formEvent) {
    formEvent.preventDefault();
    if (!userId) {
      setState({ loading: false, error: 'Tu sesión no contiene un ID de usuario válido.', success: false });
      return;
    }

    if (availability === 0 || (selectedSector && availableForSector(selectedSector) < quantity)) {
      setState((current) => ({ ...current, error: 'No hay suficientes entradas disponibles para este sector.', success: false }));
      return;
    }
    setState((current) => ({ ...current, loading: true, error: '', success: false }));
    try {
      await api.reserveEntries({ idEvento: event.idEvento, idUsuario: userId, sector: selectedSector?.name || 'General', cantidad: quantity });
      const order = await api.createOrder({
        idEvento: event.idEvento,
        idUsuario: userId,
        cantidad: quantity,
        precioUnitario: unitPrice,
        estado: 'PENDIENTE',
        ...(selectedSector ? { sector: selectedSector.name } : {})
      });
      await api.confirmReservation({ idEvento: event.idEvento, idUsuario: userId, idOrden: order.idOrden });
      setState((current) => ({ ...current, loading: false, error: '', success: true }));
      onCreated?.(order);
    } catch (error) {
      setState((current) => ({ ...current, loading: false, error: error.message, success: false }));
    }
  }

  return <div className="modal-backdrop" role="presentation" onMouseDown={(mouseEvent) => mouseEvent.target === mouseEvent.currentTarget && onClose()}>
    <section className="reservation-modal" role="dialog" aria-modal="true" aria-labelledby="reservation-title">
      <button className="modal-close" onClick={onClose} aria-label="Cerrar reserva">×</button>
      {!state.success ? <>
        <p className="eyebrow">RESERVA DE ENTRADAS</p>
        <h2 id="reservation-title">{event.nombre}</h2>
        <div className="reservation-event-meta">
          <span>{formatEventDate(event.fechaInicio)}</span>
          <span>{event.lugar || 'Evento GoTicket'}</span>
        </div>
        {event.imagenUrl && <img className="reservation-event-image" src={event.imagenUrl} alt="" onError={(error) => { error.currentTarget.style.display = 'none'; }} />}
        <form className="reservation-form" onSubmit={handleSubmit}>
          {isMatch && <div className="function-picker">
            <span>PARTIDO / SELECCIONA FUNCIÓN</span>
            <div className="match-teams" aria-label={`Partido: local ${matchTeams.local}, visitante ${matchTeams.visitante}`}>
              <div className="match-team match-team-home"><strong>{matchTeams.local}</strong><small>LOCAL</small></div>
              <b className="match-vs">VS</b>
              <div className="match-team match-team-away"><strong>{matchTeams.visitante}</strong><small>VISITANTE</small></div>
            </div>
            <strong>{formatEventDate(event.fechaInicio).toUpperCase()}</strong><em>● Disponible</em>
          </div>}
          {(hasArenaSectors || isMatch) && <div className={`stadium-map ${isMatch ? 'match-map' : 'arena-map'}`} aria-label="Mapa de sectores">
            <span className="stadium-field">{isMatch ? 'CANCHA' : 'ESCENARIO'}</span>
            {sectors.slice(0, 4).map((sector, index) => <button
              key={sector.name}
              type="button"
              className={`map-sector map-sector-${index + 1} ${selectedSector?.name === sector.name ? 'is-selected' : ''}`}
              style={{ '--sector-color': sector.color }}
              aria-label={`Seleccionar ${sector.name}`}
              onClick={() => setSelectedSector(sector)}
            />)}
          </div>}
          {(hasArenaSectors || isMatch) && <fieldset className="sector-picker">
            <legend>Selecciona tu sector</legend>
            {sectors.map((sector) => {
              const sectorPrice = Number(event.precio || 0) * sector.multiplier;
              const available = availableForSector(sector);
              const soldOut = !state.loadingAvailability && available === 0;
              return <label className={`sector-option ${selectedSector?.name === sector.name ? 'is-selected' : ''} ${soldOut ? 'is-sold-out' : ''}`} key={sector.name} style={{ '--sector-color': sector.color }}>
                <input type="radio" name="sector" value={sector.name} checked={selectedSector?.name === sector.name} disabled={soldOut} onChange={() => { setSelectedSector(sector); setQuantity(1); }} />
                <span className="sector-name"><i aria-hidden="true" /><strong>{sector.name}</strong><small>{state.loadingAvailability ? 'Consultando disponibilidad...' : `${availableForSector(sector).toLocaleString('es-CL')} entradas disponibles`}</small></span>
                <span className="sector-price"><b>${sectorPrice.toLocaleString('es-CL')}</b><small>{availabilityLabel(sector)}</small>{isMatch && <em className={`sector-side sector-side-${sector.side.toLowerCase()}`}>{sector.side}</em>}</span>
              </label>;
            })}
          </fieldset>}
          {state.error && <p className="error-message">{state.error}</p>}
          <div className="reservation-summary">
            <div className="quantity-control">
              <span>Cantidad de entradas</span>
              <div><button type="button" onClick={() => setQuantity((current) => Math.max(1, current - 1))} disabled={quantity <= 1}>−</button><strong>{quantity}</strong><button type="button" onClick={() => setQuantity((current) => Math.min(10, current + 1))} disabled={quantity >= 10 || (selectedSector && availableForSector(selectedSector) <= quantity)}>+</button></div>
              <small>Máximo 10 entradas por usuario</small>
            </div>
            <div className="reservation-total"><span>{selectedSector?.name || 'Entrada'} · Total</span><strong>${total.toLocaleString('es-CL')}</strong></div>
            <button className="primary-button" type="submit" disabled={state.loading || state.loadingAvailability || availability === 0 || ((hasArenaSectors || isMatch) && !selectedSector)}>{state.loading ? 'Creando reserva...' : state.loadingAvailability ? 'Consultando disponibilidad...' : 'Continuar con la reserva →'}</button>
          </div>
        </form>
      </> : <div className="reservation-success">
        <p className="eyebrow">RESERVA CONFIRMADA</p>
        <h2>Tu entrada quedó reservada.</h2>
        <p className="muted">Puedes revisar el detalle en la sección Mis órdenes.</p>
        <button className="primary-button" onClick={onClose}>Continuar</button>
      </div>}
    </section>
  </div>;
}

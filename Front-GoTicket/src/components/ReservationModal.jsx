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
  { name: 'Rapa Nui', multiplier: 1, capacity: 1800, color: '#f6c344' },
  { name: 'Oceano', multiplier: 0.72, capacity: 2400, color: '#f58220' },
  { name: 'Cordillera', multiplier: 0.52, capacity: 3000, color: '#e92d55' },
  { name: 'Caupolicán', multiplier: 0.38, capacity: 2600, color: '#9272b4' },
  { name: 'Tucapel', multiplier: 0.38, capacity: 2600, color: '#a6379e' },
  { name: 'Galvarino', multiplier: 0.38, capacity: 2200, color: '#4262a4' },
  { name: 'Arica', multiplier: 0.38, capacity: 2200, color: '#9bd8d5' },
  { name: 'Magallanes', multiplier: 0.38, capacity: 2200, color: '#54bba9' }
];

export default function ReservationModal({ event, user, onClose, onCreated }) {
  const [quantity, setQuantity] = useState(1);
  const hasArenaSectors = event.lugar?.toLowerCase().includes('santander arena');
  const isMatch = /f[uú]tbol|partido|colo-colo|deportes concepci[oó]n/i.test(`${event.categoria || ''} ${event.nombre || ''}`);
  const sectors = isMatch ? matchSectors : arenaSectors;
  const [selectedSector, setSelectedSector] = useState((hasArenaSectors || isMatch) ? sectors[1] : null);
  const [availability, setAvailability] = useState(null);
  const [state, setState] = useState({ loading: false, loadingAvailability: true, error: '', success: false });
  const userId = user?.idUsuario || user?.id || user?.id_usuario;
  const unitPrice = Number(event.precio || 0) * (selectedSector?.multiplier || 1);
  const total = unitPrice * quantity;

  useEffect(() => {
    api.inventoryByEvent(event.idEvento)
      .then((inventory) => setAvailability(Number(inventory?.cantidadDisponible ?? 0)))
      .catch((error) => setState((current) => ({ ...current, error: `No se pudo consultar la disponibilidad: ${error.message}` })))
      .finally(() => setState((current) => ({ ...current, loadingAvailability: false })));
  }, [event.idEvento]);

  const totalSectorCapacity = sectors.reduce((sum, sector) => sum + sector.capacity, 0);
  const availableForSector = (sector) => availability === null ? 0 : Math.min(sector.capacity, Math.floor(availability * sector.capacity / totalSectorCapacity));

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
        <p className="muted">{event.lugar || 'Evento GoTicket'}</p>
        <form className="reservation-form" onSubmit={handleSubmit}>
          {isMatch && <div className="function-picker"><span>Selecciona función</span><strong>DOM. 13 SEPTIEMBRE · 17:30</strong><em>● Disponible</em></div>}
          {(hasArenaSectors || isMatch) && <div className={`stadium-map ${isMatch ? 'match-map' : 'arena-map'}`} aria-hidden="true"><span className="stadium-field">{isMatch ? 'CANCHA' : 'ESCENARIO'}</span><i /><i /><i /><i /></div>}
          {(hasArenaSectors || isMatch) && <fieldset className="sector-picker">
            <legend>Selecciona tu sector</legend>
            {sectors.map((sector) => {
              const sectorPrice = Number(event.precio || 0) * sector.multiplier;
              return <label className={`sector-option ${selectedSector?.name === sector.name ? 'is-selected' : ''}`} key={sector.name} style={{ '--sector-color': sector.color }}>
                <input type="radio" name="sector" value={sector.name} checked={selectedSector?.name === sector.name} onChange={() => setSelectedSector(sector)} />
                <span className="sector-name"><i aria-hidden="true" /><strong>{sector.name}</strong><small>{state.loadingAvailability ? 'Consultando disponibilidad...' : `${availableForSector(sector).toLocaleString('es-CL')} entradas disponibles`}</small></span>
                <b>${sectorPrice.toLocaleString('es-CL')}</b>
              </label>;
            })}
          </fieldset>}
          <label htmlFor="quantity">Cantidad de entradas
            <input id="quantity" type="number" min="1" max="10" value={quantity} onChange={(inputEvent) => setQuantity(Number(inputEvent.target.value))} required />
          </label>
          <div className="reservation-total"><span>{selectedSector?.name || 'Entrada'} · Total</span><strong>${total.toLocaleString('es-CL')}</strong></div>
          {state.error && <p className="error-message">{state.error}</p>}
          <button className="primary-button" type="submit" disabled={state.loading || state.loadingAvailability || availability === 0}>{state.loading ? 'Creando reserva...' : state.loadingAvailability ? 'Consultando disponibilidad...' : 'Confirmar reserva'}</button>
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

import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { canPurchaseEvent, isEventUpcoming } from '../utils/eventAvailability';

export default function ReservationModal({ event, user, onClose, onCreated }) {
  const [quantity, setQuantity] = useState(1);
  const [selectedSector, setSelectedSector] = useState('');
  const [availability, setAvailability] = useState(null);
  const [sectors, setSectors] = useState([]);
  const [state, setState] = useState({ loading: false, loadingAvailability: true, error: '', success: false });
  const userId = user?.idUsuario || user?.id || user?.id_usuario;
  const activeSector = sectors.find((sector) => sector.sector === selectedSector);
  const unitPrice = Number(activeSector?.precio ?? 0);
  const total = unitPrice * quantity;
  const purchaseAvailable = canPurchaseEvent(event);
  const eventStartTime = event.fechaInicio ? new Date(event.fechaInicio).getTime() : NaN;
  const saleUnavailableMessage = !event.activo
    ? 'La venta de este evento está cerrada.'
    : Number.isFinite(eventStartTime) && !isEventUpcoming(event.fechaInicio)
      ? 'Este evento ya comenzó o finalizó; no se pueden comprar entradas.'
      : !Number.isFinite(eventStartTime)
        ? 'La fecha del evento no está confirmada; no se pueden comprar entradas todavía.'
        : '';

  const formatEventDate = (value) => value ? new Date(value).toLocaleString('es-CL', {
    weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit'
  }) : 'Fecha por confirmar';

  useEffect(() => {
    let active = true;
    Promise.all([api.inventoryByEvent(event.idEvento), api.inventorySectors(event.idEvento)])
      .then(([inventory, availableSectors]) => {
        if (!active) return;
        setAvailability(Number(inventory?.cantidadDisponible ?? 0));
        setSectors(availableSectors || []);
        setSelectedSector((availableSectors || []).find((sector) => sector.cantidadDisponible > 0)?.sector || '');
        if (!availableSectors?.length) {
          setState((current) => ({ ...current, error: 'Este evento todavía no tiene sectores configurados.' }));
        }
      })
      .catch((error) => {
        if (active) setState((current) => ({ ...current, error: `No se pudo consultar la disponibilidad: ${error.message}` }));
      })
      .finally(() => {
        if (active) setState((current) => ({ ...current, loadingAvailability: false }));
      });
    return () => { active = false; };
  }, [event.idEvento]);

  const availableForSector = (sector) => Number(sector?.cantidadDisponible ?? 0);

  async function handleSubmit(formEvent) {
    formEvent.preventDefault();
    if (!userId) {
      setState((current) => ({ ...current, error: 'Tu sesión no contiene un ID de usuario válido.' }));
      return;
    }
    if (!activeSector || availableForSector(activeSector) < quantity) {
      setState((current) => ({ ...current, error: 'No hay suficientes entradas disponibles para este sector.' }));
      return;
    }

    setState((current) => ({ ...current, loading: true, error: '', success: false }));
    try {
      const order = await api.checkout({
        idEvento: event.idEvento,
        idUsuario: userId,
        cantidad: quantity,
        sector: selectedSector
      });
      setState((current) => ({ ...current, loading: false, success: true }));
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
          {sectors.length > 0 && <div className="sector-layout">
            <strong>Distribución de sectores configurada</strong>
            <div className="sector-layout-map" role="group" aria-label="Mapa interactivo de sectores">
              <span className="sector-layout-stage">ESCENARIO</span>
              {sectors.map((sector) => <button
                key={sector.sector}
                type="button"
                className={`sector-layout-marker ${selectedSector === sector.sector ? 'is-selected' : ''}`}
                style={{
                  left: `${sector.posicionX ?? 50}%`,
                  top: `${sector.posicionY ?? 50}%`,
                  '--sector-color': sector.color || '#702bd8'
                }}
                disabled={state.loadingAvailability || availableForSector(sector) === 0}
                aria-label={`${sector.sector}: ${availableForSector(sector)} disponibles`}
                onClick={() => { setSelectedSector(sector.sector); setQuantity(1); }}
              >{sector.sector}</button>)}
            </div>
          </div>}
          <fieldset className="sector-picker">
            <legend>Selecciona un sector</legend>
            {sectors.map((sector) => {
              const available = availableForSector(sector);
              const soldOut = available === 0;
              return <label className={`sector-option ${selectedSector === sector.sector ? 'is-selected' : ''} ${soldOut ? 'is-sold-out' : ''}`} key={sector.sector} style={{ '--sector-color': sector.color || '#702bd8' }}>
                <input type="radio" name="sector" value={sector.sector} checked={selectedSector === sector.sector} disabled={soldOut || state.loadingAvailability} onChange={() => { setSelectedSector(sector.sector); setQuantity(1); }} />
                <span className="sector-name"><strong>{sector.sector}</strong><small>Modalidad {sector.modalidad === 'NUMERADO' ? 'numerada' : 'general'} · {Number(sector.cantidadTotal ?? sector.capacidad ?? 0).toLocaleString('es-CL')} de capacidad</small></span>
                <span className="sector-price"><b>${Number(sector.precio).toLocaleString('es-CL')}</b><small>{state.loadingAvailability ? 'Consultando...' : soldOut ? 'Agotado' : `${available.toLocaleString('es-CL')} disponibles`}</small></span>
              </label>;
            })}
            {state.loadingAvailability && <p className="muted">Consultando disponibilidad...</p>}
            {!state.loadingAvailability && sectors.length === 0 && <p className="muted">No hay sectores disponibles para reservar.</p>}
          </fieldset>
          {state.error && <p className="error-message">{state.error}</p>}
          {saleUnavailableMessage && <p className="error-message">{saleUnavailableMessage}</p>}
          <div className="reservation-summary">
            <div className="quantity-control">
              <span>Cantidad de entradas</span>
              <div>
                <button type="button" onClick={() => setQuantity((current) => Math.max(1, current - 1))} disabled={quantity <= 1}>−</button>
                <strong>{quantity}</strong>
                <button type="button" onClick={() => setQuantity((current) => Math.min(10, availableForSector(activeSector), current + 1))} disabled={!activeSector || quantity >= Math.min(10, availableForSector(activeSector))}>+</button>
              </div>
              <small>Máximo 10 entradas por usuario</small>
            </div>
            <div className="reservation-total"><span>{selectedSector || 'Entrada'} · Total</span><strong>${total.toLocaleString('es-CL')}</strong></div>
            <button className="primary-button" type="submit" disabled={!purchaseAvailable || state.loading || state.loadingAvailability || availability === 0 || !activeSector || availableForSector(activeSector) < quantity}>
              {state.loading ? 'Creando reserva...' : state.loadingAvailability ? 'Consultando disponibilidad...' : 'Continuar con la reserva →'}
            </button>
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

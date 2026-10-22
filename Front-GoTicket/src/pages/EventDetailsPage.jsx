import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { canPurchaseEvent, isEventUpcoming } from '../utils/eventAvailability';

function formatDate(value) {
  return value
    ? new Date(value).toLocaleString('es-CL', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
    })
    : 'Fecha por confirmar';
}

export default function EventDetailsPage({ id, onReserve }) {
  const [event, setEvent] = useState(null);
  const [state, setState] = useState({ loading: true, error: '', shareMessage: '' });

  useEffect(() => {
    let active = true;
    api.event(id)
      .then((result) => { if (active) setEvent(result); })
      .catch((error) => { if (active) setState((current) => ({ ...current, error: error.message })); })
      .finally(() => { if (active) setState((current) => ({ ...current, loading: false })); });
    return () => { active = false; };
  }, [id]);

  async function shareEvent() {
    const shareData = { title: event.nombre, text: event.descripcion || event.nombre, url: window.location.href };
    try {
      if (navigator.share) {
        await navigator.share(shareData);
      } else {
        await navigator.clipboard.writeText(shareData.url);
      }
      setState((current) => ({ ...current, shareMessage: 'Enlace del evento copiado o compartido.' }));
    } catch (error) {
      if (error.name !== 'AbortError') {
        setState((current) => ({ ...current, error: 'No se pudo compartir el evento desde este navegador.' }));
      }
    }
  }

  if (state.loading) return <section className="page-section"><p className="muted">Cargando evento...</p></section>;
  if (state.error || !event) return <section className="page-section"><p className="error-message">{state.error || 'Evento no encontrado.'}</p><a className="secondary-button" href="#eventos">Volver a eventos</a></section>;

  const purchaseAvailable = canPurchaseEvent(event);
  const salesStatus = purchaseAvailable
    ? 'VENTA ACTIVA'
    : !event.activo
      ? 'VENTA CERRADA'
      : event.fechaInicio && !isEventUpcoming(event.fechaInicio)
        ? 'EVENTO FINALIZADO'
        : 'FECHA POR CONFIRMAR';

  return <article className="page-section event-details">
    <a className="event-back-link" href="#eventos">← Volver a eventos</a>
    {event.imagenUrl && <img className="event-details-image" src={event.imagenUrl} alt={`Imagen de ${event.nombre}`} />}
    <p className="eyebrow">{event.categoria} / {salesStatus}</p>
    <h1>{event.nombre}</h1>
    <div className="event-details-meta">
      <p><strong>Fecha</strong><span>{formatDate(event.fechaInicio)}</span></p>
      <p><strong>Recinto</strong><span>{event.lugar}</span></p>
      <p><strong>Precio desde</strong><span>${Number(event.precio || 0).toLocaleString('es-CL')}</span></p>
    </div>
    <div className="event-details-description">
      <h2>Acerca del evento</h2>
      <p>{event.descripcion || 'Más información del evento estará disponible próximamente.'}</p>
    </div>
    <aside className="refund-policy">
      <h2>Política de devolución</h2>
      <p>Las solicitudes de devolución deben gestionarse con el organizador. GoTicket no realiza cobros reales en este modo de demostración.</p>
    </aside>
    {state.error && <p className="error-message">{state.error}</p>}
    {state.shareMessage && <p className="success-message">{state.shareMessage}</p>}
    <div className="event-details-actions">
      <button className="secondary-button" onClick={shareEvent}>Compartir evento</button>
      <button className="primary-button" onClick={() => onReserve(event)} disabled={!purchaseAvailable}>
        {purchaseAvailable ? 'Comprar entradas' : salesStatus === 'EVENTO FINALIZADO' ? 'Evento finalizado' : 'Venta no disponible'}
      </button>
    </div>
  </article>;
}

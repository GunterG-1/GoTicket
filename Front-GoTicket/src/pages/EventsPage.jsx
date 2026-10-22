import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { canPurchaseEvent, isEventUpcoming } from '../utils/eventAvailability';

const categoryLabels = {
  todos: 'Todos los eventos',
  musica: 'Música',
  deportes: 'Deportes',
  teatro: 'Teatro',
  familia: 'Familia',
  especiales: 'Especiales'
};

const pageSize = 12;

function eventDate(value) {
  return value
    ? new Date(value).toLocaleString('es-CL', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
    })
    : 'Fecha por confirmar';
}

function eventSalesLabel(event) {
  if (canPurchaseEvent(event)) return 'Disponible';
  if (!event.activo) return 'Venta cerrada';
  if (event.fechaInicio && !isEventUpcoming(event.fechaInicio)) return 'Finalizado';
  return 'Fecha por confirmar';
}

export default function EventsPage({ category = 'todos', query = '', onReserve, onDetails }) {
  const [events, setEvents] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [draftFilters, setDraftFilters] = useState({ city: '', dateFrom: '', dateTo: '', priceMin: '', priceMax: '' });
  const [filters, setFilters] = useState(draftFilters);
  const [state, setState] = useState({ loading: true, error: '' });
  const [activeSlide, setActiveSlide] = useState(0);
  const [carouselPaused, setCarouselPaused] = useState(false);

  useEffect(() => {
    setPage(0);
  }, [category, query]);

  useEffect(() => {
    let active = true;
    setState({ loading: true, error: '' });
    api.searchEvents({
      texto: query,
      categoria: category === 'todos' ? '' : category,
      ciudad: filters.city,
      desde: filters.dateFrom ? `${filters.dateFrom}T00:00:00` : '',
      hasta: filters.dateTo ? `${filters.dateTo}T23:59:59` : '',
      precioMinimo: filters.priceMin,
      precioMaximo: filters.priceMax,
      page,
      size: pageSize
    })
      .then((result) => {
        if (!active) return;
        setEvents(result?.content || []);
        setTotalPages(result?.totalPages || 0);
        setActiveSlide(0);
      })
      .catch((error) => {
        if (active) setState({ loading: false, error: error.message });
      })
      .finally(() => {
        if (active) setState((current) => ({ ...current, loading: false }));
      });
    return () => { active = false; };
  }, [category, query, filters, page]);

  useEffect(() => {
    if (events.length < 2 || carouselPaused) return undefined;
    const timer = window.setInterval(() => setActiveSlide((slide) => (slide + 1) % events.length), 5000);
    return () => window.clearInterval(timer);
  }, [events.length, carouselPaused]);

  const featuredEvent = events[activeSlide];
  const previousSlide = () => setActiveSlide((slide) => (slide - 1 + events.length) % events.length);
  const nextSlide = () => setActiveSlide((slide) => (slide + 1) % events.length);

  function applyFilters(event) {
    event.preventDefault();
    setPage(0);
    setFilters({ ...draftFilters });
  }

  function clearFilters() {
    const emptyFilters = { city: '', dateFrom: '', dateTo: '', priceMin: '', priceMax: '' };
    setDraftFilters(emptyFilters);
    setFilters(emptyFilters);
    setPage(0);
  }

  return <section className="events-page">
    <div className="hero-carousel" aria-roledescription="carrusel" aria-label="Eventos destacados">
      <div className="hero-event" key={featuredEvent?.idEvento || 'empty'}>
        <div className="hero-copy">
          <p className="eyebrow">GOTICKET / EXPERIENCIAS</p>
          <p className="hero-category">● Evento destacado</p>
          <h1>{featuredEvent?.nombre || 'Encuentra tu próximo evento'}</h1>
          <p>{featuredEvent?.descripcion || 'Conciertos, deportes, teatro y experiencias para compartir.'}</p>
          {featuredEvent && <button className="hero-button" onClick={() => onDetails?.(featuredEvent)}>Ver evento <span aria-hidden="true">→</span></button>}
        </div>
        {featuredEvent?.imagenUrl
          ? <img className="hero-event-image" src={featuredEvent.imagenUrl} alt="" onError={(event) => { event.currentTarget.style.display = 'none'; }} />
          : <div className="hero-art" aria-hidden="true"><span>{featuredEvent ? 'GO' : 'GT'}</span><i /></div>}
        {events.length > 1 && <>
          <button className="carousel-arrow carousel-prev" onClick={previousSlide} aria-label="Evento anterior">‹</button>
          <button className="carousel-arrow carousel-next" onClick={nextSlide} aria-label="Siguiente evento">›</button>
          <div className="carousel-dots" aria-label="Seleccionar evento">
            {events.map((event, index) => <button key={event.idEvento || index}
              className={index === activeSlide ? 'is-active' : ''}
              onClick={() => setActiveSlide(index)}
              aria-label={`Ver evento ${index + 1}`}
              aria-current={index === activeSlide} />)}
          </div>
          <button className="carousel-pause" onClick={() => setCarouselPaused((paused) => !paused)} aria-pressed={carouselPaused}>
            {carouselPaused ? 'Reanudar' : 'Pausar'}
          </button>
        </>}
      </div>
    </div>
    <div className="page-section">
      <div className="section-heading">
        <div>
          <p className="eyebrow">AGENDA / {categoryLabels[category] || categoryLabels.todos}</p>
          <h2>{query ? `Resultados para “${query}”` : categoryLabels[category] || categoryLabels.todos}</h2>
        </div>
        <span className="status-dot">{state.error ? '● API no disponible' : state.loading ? '● Consultando API' : '● API conectada'}</span>
      </div>
      <form className="event-filters" onSubmit={applyFilters}>
        <label>Ciudad o recinto<input value={draftFilters.city} onChange={(event) => setDraftFilters((current) => ({ ...current, city: event.target.value }))} placeholder="Santiago" /></label>
        <label>Desde<input type="date" value={draftFilters.dateFrom} onChange={(event) => setDraftFilters((current) => ({ ...current, dateFrom: event.target.value }))} /></label>
        <label>Hasta<input type="date" value={draftFilters.dateTo} onChange={(event) => setDraftFilters((current) => ({ ...current, dateTo: event.target.value }))} /></label>
        <label>Precio mínimo<input type="number" min="0" value={draftFilters.priceMin} onChange={(event) => setDraftFilters((current) => ({ ...current, priceMin: event.target.value }))} /></label>
        <label>Precio máximo<input type="number" min="0" value={draftFilters.priceMax} onChange={(event) => setDraftFilters((current) => ({ ...current, priceMax: event.target.value }))} /></label>
        <div className="event-filter-actions"><button className="secondary-button" type="submit">Filtrar</button><button className="text-button" type="button" onClick={clearFilters}>Limpiar</button></div>
      </form>
      {state.loading && <p className="muted">Cargando eventos...</p>}
      {state.error && <p className="error-message">{state.error}</p>}
      {!state.loading && !state.error && <div className="event-grid">{events.map((event) => <article className="event-card" key={event.idEvento}>
        {event.imagenUrl && <img className="event-card-image" src={event.imagenUrl} alt="" loading="lazy" onError={(error) => { error.currentTarget.style.display = 'none'; }} />}
        <span className="event-tag">{event.categoria || 'Evento'} / {eventSalesLabel(event)}</span>
        <h3>{event.nombre}</h3>
        <p>{event.descripcion || 'Una experiencia GoTicket para recordar.'}</p>
        <p className="event-date">{eventDate(event.fechaInicio)}</p>
        <div className="event-meta"><span>{event.lugar}</span><strong>${Number(event.precio || 0).toLocaleString('es-CL')}</strong></div>
        <div className="event-card-actions">
          <button className="secondary-button" onClick={() => onDetails?.(event)}>Ver detalles</button>
          <button className="primary-button" onClick={() => onReserve?.(event)} disabled={!canPurchaseEvent(event)}>Reservar</button>
        </div>
      </article>)}</div>}
      {!state.loading && !state.error && events.length === 0 && <p className="empty-state">No encontramos eventos con esos filtros.</p>}
      {!state.loading && !state.error && totalPages > 1 && <nav className="pagination" aria-label="Paginación de eventos">
        <button className="secondary-button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Anterior</button>
        <span>Página {page + 1} de {totalPages}</span>
        <button className="secondary-button" disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)}>Siguiente</button>
      </nav>}
    </div>
  </section>;
}

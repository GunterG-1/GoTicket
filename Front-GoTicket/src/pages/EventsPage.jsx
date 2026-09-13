import { useEffect, useState } from 'react';
import { api } from '../services/api';

const categoryLabels = {
  todos: 'Todos los eventos',
  musica: 'Música',
  deportes: 'Deportes',
  teatro: 'Teatro',
  familia: 'Familia',
  especiales: 'Especiales'
};

const categoryMatchers = {
  musica: /pop|urbano|music|fan meeting|electr[oó]nica|bolero|metal|rock|hard rock|balada|bachata/i,
  deportes: /f[uú]tbol|deporte|exhibici[oó]n/i,
  teatro: /teatro|musical|show teatral/i,
  familia: /familia|youtuber|infantil|cirque|niños/i,
  especiales: /fonda|festival|fiesta/i
};

function belongsToCategory(event, category) {
  if (category === 'todos') return true;
  return categoryMatchers[category]?.test(`${event.categoria || ''} ${event.nombre || ''}`) || false;
}

export default function EventsPage({ category = 'todos', onReserve }) {
  const [events, setEvents] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [activeSlide, setActiveSlide] = useState(0);

  const filteredEvents = events.filter((event) => belongsToCategory(event, category));

  useEffect(() => {
    api.events().then(setEvents).catch((error) => setState({ loading: false, error: error.message })).finally(() => setState((current) => ({ ...current, loading: false })));
  }, []);

  useEffect(() => {
    setActiveSlide(0);
  }, [category]);

  useEffect(() => {
    if (filteredEvents.length < 2) return undefined;
    const timer = window.setInterval(() => setActiveSlide((slide) => (slide + 1) % filteredEvents.length), 5000);
    return () => window.clearInterval(timer);
  }, [filteredEvents.length, category]);

  useEffect(() => {
    if (activeSlide >= filteredEvents.length) setActiveSlide(0);
  }, [activeSlide, filteredEvents.length]);

  const featuredEvent = filteredEvents[activeSlide];
  const previousSlide = () => setActiveSlide((slide) => (slide - 1 + filteredEvents.length) % filteredEvents.length);
  const nextSlide = () => setActiveSlide((slide) => (slide + 1) % filteredEvents.length);

  return <section className="events-page">
    <div className="hero-carousel" aria-roledescription="carrusel" aria-label="Eventos destacados">
      <div className="hero-event" key={featuredEvent?.idEvento || 'empty'}>
        <div className="hero-copy">
          <p className="eyebrow">GOTICKET / EXPERIENCIAS</p>
          <p className="hero-category">● Evento destacado</p>
          <h1>{featuredEvent?.nombre || 'Encuentra tu próximo evento'}</h1>
          <p>{featuredEvent?.descripcion || 'Conciertos, deportes, teatro y experiencias para compartir.'}</p>
          {featuredEvent && <button className="hero-button" onClick={() => onReserve?.(featuredEvent)}>Ver evento <span aria-hidden="true">→</span></button>}
        </div>
        {featuredEvent?.imagenUrl ? <img className="hero-event-image" src={featuredEvent.imagenUrl} alt="" onError={(error) => { error.currentTarget.style.display = 'none'; }} /> : <div className="hero-art" aria-hidden="true"><span>{featuredEvent ? 'GO' : 'GT'}</span><i /></div>}
        {filteredEvents.length > 1 && <>
          <button className="carousel-arrow carousel-prev" onClick={previousSlide} aria-label="Evento anterior">‹</button>
          <button className="carousel-arrow carousel-next" onClick={nextSlide} aria-label="Siguiente evento">›</button>
          <div className="carousel-dots" aria-label="Seleccionar evento">
            {filteredEvents.map((event, index) => <button key={event.idEvento || index} className={index === activeSlide ? 'is-active' : ''} onClick={() => setActiveSlide(index)} aria-label={`Ver evento ${index + 1}`} aria-current={index === activeSlide} />)}
          </div>
        </>}
      </div>
    </div>
    <div className="page-section"><div className="section-heading"><div><p className="eyebrow">AGENDA EN VIVO / {categoryLabels[category] || categoryLabels.todos}</p><h2>{categoryLabels[category] || categoryLabels.todos}</h2></div><span className="status-dot">● API conectada</span></div>
    {state.loading && <p className="muted">Cargando eventos...</p>}
    {state.error && <p className="error-message">{state.error}</p>}
    {!state.loading && !state.error && <div className="event-grid">{filteredEvents.map((event) => <article className="event-card" key={event.idEvento}>
      {event.imagenUrl && <img className="event-card-image" src={event.imagenUrl} alt="" loading="lazy" onError={(error) => { error.currentTarget.style.display = 'none'; }} />}
      <span className="event-tag">{event.categoria || 'Evento'} / {event.activo ? 'Disponible' : 'No disponible'}</span><h3>{event.nombre}</h3><p>{event.descripcion || 'Una experiencia GoTicket para recordar.'}</p>
      <div className="event-meta"><span>{event.lugar}</span><strong>${Number(event.precio || 0).toLocaleString('es-CL')}</strong></div>
      <button className="secondary-button" onClick={() => onReserve?.(event)} disabled={!event.activo}>Reservar entrada</button>
    </article>)}</div>}
    {!state.loading && !state.error && filteredEvents.length === 0 && <p className="empty-state">No hay eventos en la categoría {categoryLabels[category] || category}.</p>}
    </div>
  </section>;
}

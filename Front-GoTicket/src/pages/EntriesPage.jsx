import { useEffect, useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { api, getSession } from '../services/api';

export default function EntriesPage() {
  const userId = getSession().user?.idUsuario;
  const [entries, setEntries] = useState([]);
  const [resaleEntries, setResaleEntries] = useState([]);
  const [events, setEvents] = useState([]);
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [prices, setPrices] = useState({});
  const [state, setState] = useState({ loading: true, buyingEntryId: null, downloadingEntryId: null, error: '', success: '' });

  function loadEntries() {
    if (!userId) return;
    setState((current) => ({ ...current, loading: true, error: '' }));
    Promise.all([api.userEntries(userId), api.resaleEntries(), api.events()])
      .then(([owned, resale, loadedEvents]) => { setEntries(owned || []); setResaleEntries(resale || []); setEvents(loadedEvents || []); })
      .catch((error) => setState((current) => ({ ...current, error: error.message })))
      .finally(() => setState((current) => ({ ...current, loading: false })));
  }

  useEffect(() => { loadEntries(); }, [userId]);
  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') setSelectedTicket(null);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  async function publish(entry) {
    const price = Number(prices[entry.idEntrada]);
    const originalPrice = Number(entry.precioOriginal ?? entry.precio);
    const maximumResalePrice = Math.floor(originalPrice * 0.95 * 100) / 100;
    if (!price || price <= 0 || price > maximumResalePrice) {
      setState((current) => ({ ...current, error: `El precio debe ser mayor que cero y no superar $${maximumResalePrice.toLocaleString('es-CL')}.` }));
      return;
    }
    try {
      await api.publishResale(entry.idEntrada, { idUsuario: userId, precio: price });
      setState((current) => ({ ...current, error: '', success: 'Entrada publicada para reventa.' }));
      loadEntries();
    } catch (error) { setState((current) => ({ ...current, error: error.message })); }
  }

  async function buy(entry) {
    setState((current) => ({ ...current, buyingEntryId: entry.idEntrada, error: '', success: '' }));
    try {
      const order = await api.checkout({
        idEvento: entry.idEvento,
        idUsuario: userId,
        cantidad: 1,
        sector: entry.sector,
        idEntradaReventa: entry.idEntrada
      });
      window.location.hash = `pagos?orden=${order.idOrden}`;
    } catch (error) {
      setState((current) => ({ ...current, buyingEntryId: null, error: error.message }));
    }
  }

  async function downloadTicket(entry) {
    setState((current) => ({ ...current, downloadingEntryId: entry.idEntrada, error: '' }));
    try {
      const pdf = await api.ticketPdf(userId, entry.idEntrada);
      const url = URL.createObjectURL(pdf);
      const link = document.createElement('a');
      link.href = url;
      link.download = `goticket-entrada-${entry.idEntrada}.pdf`;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      setState((current) => ({ ...current, error: error.message }));
    } finally {
      setState((current) => ({ ...current, downloadingEntryId: null }));
    }
  }

  const selectedEvent = events.find((event) => String(event.idEvento) === String(selectedTicket?.idEvento));
  return <section className="page-section entries-page"><p className="eyebrow">ENTRADAS</p><h2>Mis entradas y reventa.</h2>{state.error && <p className="error-message">{state.error}</p>}{state.success && <p className="success-message">{state.success}</p>}{state.loading ? <p className="muted">Cargando entradas...</p> : <><div className="table-wrap"><table><thead><tr><th>Entrada</th><th>Evento</th><th>Sector</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{entries.map((entry) => <tr key={entry.idEntrada}><td>#{entry.idEntrada} / Folio {entry.numeroAsiento}</td><td>{events.find((event) => String(event.idEvento) === String(entry.idEvento))?.nombre || `Evento #${entry.idEvento}`}</td><td>{entry.sector}</td><td><span className="pill">{entry.estado}</span></td><td>{['VENDIDO', 'USADA'].includes(entry.estado) && <span className="resale-action"><button className="secondary-button" onClick={() => downloadTicket(entry)} disabled={state.downloadingEntryId === entry.idEntrada}>{state.downloadingEntryId === entry.idEntrada ? 'Generando PDF...' : 'Descargar PDF'}</button>{entry.estado === 'VENDIDO' && <><button className="secondary-button" onClick={() => setSelectedTicket(entry)}>Ver entrada / QR</button><input type="number" min="1" max={Math.floor(Number(entry.precioOriginal ?? entry.precio) * 0.95 * 100) / 100} step="0.01" placeholder={`Máx. $${(Math.floor(Number(entry.precioOriginal ?? entry.precio) * 0.95 * 100) / 100).toLocaleString('es-CL')}`} value={prices[entry.idEntrada] || ''} onChange={(event) => setPrices((current) => ({ ...current, [entry.idEntrada]: event.target.value }))} /><button className="secondary-button" onClick={() => publish(entry)}>Publicar reventa</button></>}</span>}{entry.estado === 'USADA' && <span className="muted">Acceso utilizado</span>}</td></tr>)}</tbody></table>{entries.length === 0 && <p className="empty-state">No tienes entradas todavía.</p>}</div><div className="resale-market"><p className="eyebrow">MARKETPLACE · Comisión simulada 5%</p><h3>Entradas publicadas para reventa</h3><div className="event-grid">{resaleEntries.map((entry) => { const isOwner = String(entry.idUsuario) === String(userId); return <article className="event-card" key={entry.idEntrada}><span className="event-tag">REVENTA / {entry.sector}</span><h3>Entrada #{entry.idEntrada}</h3><p>Evento #{entry.idEvento} · Folio {entry.numeroAsiento}</p><p>Publicada por usuario #{entry.idUsuario}</p><div className="event-meta"><span>Disponible</span><strong>${Number(entry.precio || 0).toLocaleString('es-CL')}</strong></div>{isOwner ? <p className="muted">Esta entrada te pertenece.</p> : <button className="primary-button" disabled={state.buyingEntryId !== null} onClick={() => buy(entry)}>{state.buyingEntryId === entry.idEntrada ? 'Creando orden...' : 'Comprar entrada'}</button>}</article>; })}</div>{resaleEntries.length === 0 && <p className="empty-state">No hay entradas en reventa.</p>}</div></>}{selectedTicket && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setSelectedTicket(null)}><section className="reservation-modal" role="dialog" aria-modal="true" aria-labelledby="ticket-title"><button className="modal-close" onClick={() => setSelectedTicket(null)} aria-label="Cerrar entrada">×</button><p className="eyebrow">ENTRADA DIGITAL · #{selectedTicket.idEntrada}</p><h2 id="ticket-title">{selectedEvent?.nombre || `Evento #${selectedTicket.idEvento}`}</h2><p>{selectedTicket.sector} · Folio {selectedTicket.numeroAsiento}</p><p>{selectedEvent?.fechaInicio ? new Date(selectedEvent.fechaInicio).toLocaleString('es-CL') : 'Fecha por confirmar'}</p>{selectedTicket.codigoAcceso ? <div className="ticket-qr"><QRCodeSVG value={selectedTicket.codigoAcceso} size={240} level="H" includeMargin title="Código QR de acceso" /><p className="muted">Este código es único y queda invalidado al transferir o usar la entrada. Muéstralo al personal en el acceso.</p></div> : <p className="error-message">Esta entrada no tiene un código activo. Contacta al administrador.</p>}</section></div>}</section>;
}

import { useEffect, useState } from 'react';
import { api, getSession } from '../services/api';

export default function EntriesPage() {
  const userId = getSession().user?.idUsuario;
  const [entries, setEntries] = useState([]);
  const [resaleEntries, setResaleEntries] = useState([]);
  const [prices, setPrices] = useState({});
  const [state, setState] = useState({ loading: true, error: '', success: '' });

  function loadEntries() {
    if (!userId) return;
    setState((current) => ({ ...current, loading: true, error: '' }));
    Promise.all([api.userEntries(userId), api.resaleEntries()])
      .then(([owned, resale]) => { setEntries(owned || []); setResaleEntries(resale || []); })
      .catch((error) => setState((current) => ({ ...current, error: error.message })))
      .finally(() => setState((current) => ({ ...current, loading: false })));
  }

  useEffect(() => { loadEntries(); }, [userId]);

  async function publish(entry) {
    const price = Number(prices[entry.idEntrada]);
    if (!price || price <= 0) { setState((current) => ({ ...current, error: 'Ingresa un precio válido para publicar la entrada.' })); return; }
    try {
      await api.publishResale(entry.idEntrada, { idUsuario: userId, precio: price });
      setState((current) => ({ ...current, error: '', success: 'Entrada publicada para reventa.' }));
      loadEntries();
    } catch (error) { setState((current) => ({ ...current, error: error.message })); }
  }

  return <section className="page-section entries-page"><p className="eyebrow">ENTRADAS</p><h2>Mis entradas y reventa.</h2>{state.error && <p className="error-message">{state.error}</p>}{state.success && <p className="success-message">{state.success}</p>}{state.loading ? <p className="muted">Cargando entradas...</p> : <><div className="table-wrap"><table><thead><tr><th>Entrada</th><th>Evento</th><th>Sector</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{entries.map((entry) => <tr key={entry.idEntrada}><td>#{entry.idEntrada} / {entry.numeroAsiento}</td><td>#{entry.idEvento}</td><td>{entry.sector}</td><td><span className="pill">{entry.estado}</span></td><td>{entry.estado === 'VENDIDO' && <span className="resale-action"><input type="number" min="1" step="0.01" placeholder="Precio" value={prices[entry.idEntrada] || ''} onChange={(event) => setPrices((current) => ({ ...current, [entry.idEntrada]: event.target.value }))} /><button className="secondary-button" onClick={() => publish(entry)}>Publicar</button></span>}</td></tr>)}</tbody></table>{entries.length === 0 && <p className="empty-state">No tienes entradas todavía.</p>}</div><div className="resale-market"><p className="eyebrow">MARKETPLACE</p><h3>Entradas publicadas para reventa</h3><div className="event-grid">{resaleEntries.map((entry) => <article className="event-card" key={entry.idEntrada}><span className="event-tag">REVENTA / {entry.sector}</span><h3>Entrada #{entry.idEntrada}</h3><p>Evento #{entry.idEvento} · Asiento {entry.numeroAsiento}</p><div className="event-meta"><span>Disponible</span><strong>${Number(entry.precio || 0).toLocaleString('es-CL')}</strong></div></article>)}</div>{resaleEntries.length === 0 && <p className="empty-state">No hay entradas en reventa.</p>}</div></>}</section>;
}

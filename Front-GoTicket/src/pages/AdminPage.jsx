import { useAdmin } from '../contexts/AdminContext';
import './AdminPage.css';

export default function AdminPage() {
  const { events, inventory, form, state, updateForm, refresh, createEventWithInventory } = useAdmin();
  return <section className="admin-page page-section">
    <div className="admin-hero"><div><p className="admin-kicker">GOTICKET / ADMINISTRACIÓN</p><h1>Agregar evento</h1><p>Publica el evento en Catálogo y crea su disponibilidad en Inventario desde un solo lugar.</p></div><div className="admin-counter"><strong>{events.length}</strong><span>eventos registrados</span></div></div>
    {state.error && <p className="admin-message admin-error">{state.error}</p>}{state.success && <p className="admin-message admin-success">{state.success}</p>}
    <form className="admin-form" onSubmit={createEventWithInventory}><div className="admin-form-heading"><h2>Detalles del evento</h2><span>La capacidad inicial se replica en Inventario</span></div>
      <label className="admin-wide">Nombre del evento<input name="nombre" value={form.nombre} onChange={updateForm} required maxLength="150" /></label>
      <label>Categoría<select name="categoria" value={form.categoria} onChange={updateForm}><option value="musica">Música</option><option value="deportes">Deportes</option><option value="teatro">Teatro</option><option value="familia">Familia</option><option value="especiales">Especiales</option></select></label>
      <label>Lugar<input name="lugar" value={form.lugar} onChange={updateForm} required maxLength="200" /></label>
      <label>Inicio<input type="datetime-local" name="fechaInicio" value={form.fechaInicio} onChange={updateForm} required /></label><label>Término<input type="datetime-local" name="fechaFin" value={form.fechaFin} onChange={updateForm} /></label>
      <label>Capacidad<input type="number" name="capacidad" value={form.capacidad} onChange={updateForm} min="1" required /></label><label>Precio<input type="number" name="precio" value={form.precio} onChange={updateForm} min="0" step="0.01" /></label>
      <label className="admin-wide">Descripción<textarea name="descripcion" value={form.descripcion} onChange={updateForm} rows="4" /></label>
      <label>Imagen URL<input type="url" name="imagenUrl" value={form.imagenUrl} onChange={updateForm} /></label><label>URL externa<input type="url" name="urlEvento" value={form.urlEvento} onChange={updateForm} /></label>
      <div className="admin-actions admin-wide"><span>Se crearán ambas entidades relacionadas.</span><button type="submit" disabled={state.saving}>{state.saving ? 'Creando...' : 'Crear evento e inventario'}</button></div>
    </form>
    <div className="admin-inventory"><div className="admin-section-heading"><div><p className="admin-kicker">MICROSERVICIO INVENTARIO</p><h2>Disponibilidad por evento</h2></div><button className="admin-refresh" type="button" onClick={refresh}>Actualizar</button></div>{state.loading ? <p className="admin-empty">Cargando inventario...</p> : <div className="admin-table-wrap"><table><thead><tr><th>Evento</th><th>Total</th><th>Disponibles</th><th>Estado</th></tr></thead><tbody>{inventory.map((item) => { const currentEvent = events.find((event) => event.idEvento === item.idEvento); return <tr key={item.idInventario}><td><strong>{currentEvent?.nombre || `Evento #${item.idEvento}`}</strong><small>{currentEvent?.lugar || 'Sin lugar'}</small></td><td>{item.cantidadTotal}</td><td>{item.cantidadDisponible}</td><td><span className={item.activo ? 'admin-badge active' : 'admin-badge'}>{item.activo ? 'Activo' : 'Inactivo'}</span></td></tr>; })}</tbody></table>{inventory.length === 0 && <p className="admin-empty">No hay inventarios registrados.</p>}</div>}</div>
  </section>;
}
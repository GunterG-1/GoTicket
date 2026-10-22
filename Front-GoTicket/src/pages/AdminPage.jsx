import { useAdmin } from '../contexts/AdminContext';
import './AdminPage.css';

export default function AdminPage() {
  const { events, inventory, venues, form, sectors, venueForm, venueSectors, state, editingEventId, editingVenueId, updateForm, updateSector, addSector, removeSector, updateVenueForm, updateVenueSector, addVenueSector, removeVenueSector, editVenue, cancelVenueEditing, saveVenue, removeVenue, refresh, startEditing, cancelEditing, createEventWithInventory, removeEvent } = useAdmin();
  const sectorCapacity = sectors.reduce((sum, sector) => sum + (Number(sector.capacidad) || 0), 0);
  const capacityMatches = sectorCapacity === Number(form.capacidad);
  const venueSectorCapacity = venueSectors.reduce((sum, sector) => sum + (Number(sector.capacidad) || 0), 0);
  const venueCapacityMatches = venueSectorCapacity === Number(venueForm.capacidad);
  return <section className="admin-page page-section">
    <div className="admin-hero"><div><p className="admin-kicker">GOTICKET / ADMINISTRACIÓN</p><h1>{editingEventId ? 'Editar evento' : 'Agregar evento'}</h1><p>{editingEventId ? 'Actualiza los datos del evento y su disponibilidad.' : 'Publica el evento en Catálogo y crea su disponibilidad en Inventario desde un solo lugar.'}</p></div><div className="admin-counter"><strong>{events.length}</strong><span>eventos registrados</span></div></div>
    {state.error && <p className="admin-message admin-error">{state.error}</p>}{state.success && <p className="admin-message admin-success">{state.success}</p>}
    <form className="admin-form" onSubmit={createEventWithInventory}><div className="admin-form-heading"><h2>Detalles del evento</h2><span>La capacidad inicial se replica en Inventario</span></div>
      <label className="admin-wide">Nombre del evento<input name="nombre" value={form.nombre} onChange={updateForm} required maxLength="150" /></label>
      <label>Categoría<select name="categoria" value={form.categoria} onChange={updateForm}><option value="musica">Música</option><option value="deportes">Deportes</option><option value="teatro">Teatro</option><option value="familia">Familia</option><option value="especiales">Especiales</option></select></label>
      <label>Recinto<select name="idRecinto" value={form.idRecinto} onChange={updateForm} required><option value="">Selecciona un recinto</option>{venues.map((venue) => <option value={venue.idRecinto} key={venue.idRecinto}>{venue.nombre} · {venue.ciudad} · capacidad {venue.capacidad}</option>)}</select></label>
      <label>Inicio<input type="datetime-local" name="fechaInicio" value={form.fechaInicio} onChange={updateForm} required /></label><label>Término<input type="datetime-local" name="fechaFin" value={form.fechaFin} onChange={updateForm} /></label>
      <label>Capacidad del mapa<input type="number" name="capacidad" value={form.capacidad} readOnly required /></label><label>Precio desde<input type="number" name="precio" value={form.precio} onChange={updateForm} min="0" step="0.01" required /></label>
      <label className="admin-wide">Descripción<textarea name="descripcion" value={form.descripcion} onChange={updateForm} rows="4" /></label>
      <label>Imagen URL<input type="url" name="imagenUrl" value={form.imagenUrl} onChange={updateForm} /></label><label>URL externa<input type="url" name="urlEvento" value={form.urlEvento} onChange={updateForm} /></label>
      <fieldset className="admin-sector-editor admin-wide">
        <legend>Mapa del recinto para este evento</legend>
        <p>La distribución y las capacidades vienen del mapa del recinto. Los precios se configuran para este evento.</p>
        {sectors.map((sector, index) => <div className="admin-sector-row" key={`sector-${index}`}>
          <label>Sector<input value={sector.nombre} readOnly required maxLength="80" /></label>
          <label>Capacidad<input type="number" value={sector.capacidad} readOnly required /></label>
          <label>Precio<input type="number" value={sector.precio} onChange={(event) => updateSector(index, 'precio', event.target.value)} min="0" step="0.01" required /></label>
          <label>Modalidad<input value={sector.modalidad === 'NUMERADO' ? 'Numerado' : 'General'} readOnly /></label>
          <label>Color<input type="color" value={sector.color} readOnly /></label>
          <label>Posición X<input type="number" value={sector.posicionX} readOnly /></label>
          <label>Posición Y<input type="number" value={sector.posicionY} readOnly /></label>
        </div>)}
        {!sectors.length && <p className="admin-empty">Selecciona un recinto con mapa configurado para cargar sus sectores.</p>}
        <div className="admin-sector-footer"><strong className={capacityMatches ? 'capacity-valid' : 'capacity-invalid'}>Capacidad del mapa: {sectorCapacity.toLocaleString('es-CL')} / {Number(form.capacidad || 0).toLocaleString('es-CL')}</strong></div>
      </fieldset>
      <div className="admin-actions admin-wide"><span>{editingEventId ? 'La capacidad ajustará las entradas disponibles sin afectar las comprometidas.' : 'Se crearán ambas entidades relacionadas.'}</span><div className="admin-form-buttons">{editingEventId && <button className="admin-cancel" type="button" onClick={cancelEditing} disabled={state.saving}>Cancelar</button>}<button type="submit" disabled={state.saving || !capacityMatches}>{state.saving ? 'Guardando...' : editingEventId ? 'Guardar cambios' : 'Crear evento e inventario'}</button></div></div>
    </form>
    <section className="admin-venues">
      <div className="admin-section-heading"><div><p className="admin-kicker">RECINTOS</p><h2>{editingVenueId ? 'Editar recinto' : 'Administrar recintos'}</h2></div></div>
      <form className="admin-venue-form" onSubmit={saveVenue}>
        <label>Nombre<input name="nombre" value={venueForm.nombre} onChange={updateVenueForm} required maxLength="150" /></label>
        <label>Ciudad<input name="ciudad" value={venueForm.ciudad} onChange={updateVenueForm} required maxLength="100" /></label>
        <label>Dirección<input name="direccion" value={venueForm.direccion} onChange={updateVenueForm} required maxLength="250" /></label>
        <label>Capacidad<input type="number" name="capacidad" value={venueForm.capacidad} onChange={updateVenueForm} min="1" required /></label>
        <div className="admin-form-buttons"><button className="admin-action-edit" type="submit" disabled={state.saving}>{editingVenueId ? 'Guardar recinto' : 'Crear recinto'}</button>{editingVenueId && <button className="admin-cancel" type="button" onClick={cancelVenueEditing} disabled={state.saving}>Cancelar</button>}</div>
        <fieldset className="admin-sector-editor admin-wide admin-venue-map">
          <legend>Mapa reutilizable del recinto</legend>
          <p>Define aquí la distribución física y el aforo de cada sector. Al crear un evento con este recinto se copiará el mapa; sus precios se definirán para ese evento.</p>
          {venueSectors.map((sector, index) => <div className="admin-sector-row venue-sector-row" key={`venue-sector-${index}`}>
            <label>Sector<input value={sector.nombre} onChange={(event) => updateVenueSector(index, 'nombre', event.target.value)} required maxLength="80" /></label>
            <label>Capacidad<input type="number" value={sector.capacidad} onChange={(event) => updateVenueSector(index, 'capacidad', event.target.value)} min="1" required /></label>
            <label>Modalidad<select value={sector.modalidad} onChange={(event) => updateVenueSector(index, 'modalidad', event.target.value)}><option value="GENERAL">General</option><option value="NUMERADO">Numerado</option></select></label>
            <label>Color<input type="color" value={sector.color} onChange={(event) => updateVenueSector(index, 'color', event.target.value)} /></label>
            <label>Posición X<input type="number" value={sector.posicionX} onChange={(event) => updateVenueSector(index, 'posicionX', Number(event.target.value))} min="0" max="100" required /></label>
            <label>Posición Y<input type="number" value={sector.posicionY} onChange={(event) => updateVenueSector(index, 'posicionY', Number(event.target.value))} min="0" max="100" required /></label>
            <button className="admin-action-delete" type="button" onClick={() => removeVenueSector(index)} disabled={venueSectors.length <= 1}>Quitar</button>
          </div>)}
          <div className="admin-sector-footer"><button className="admin-cancel" type="button" onClick={addVenueSector}>Agregar sector</button><strong className={venueCapacityMatches ? 'capacity-valid' : 'capacity-invalid'}>Capacidad del mapa: {venueSectorCapacity.toLocaleString('es-CL')} / {Number(venueForm.capacidad || 0).toLocaleString('es-CL')}</strong></div>
        </fieldset>
      </form>
      <div className="admin-venue-list">{venues.map((venue) => <article key={venue.idRecinto}><div><strong>{venue.nombre}</strong><small>{venue.ciudad} · {venue.direccion} · capacidad {venue.capacidad.toLocaleString('es-CL')}</small></div><div className="admin-row-actions"><button className="admin-action-edit" type="button" onClick={() => editVenue(venue)} disabled={state.saving}>Editar</button><button className="admin-action-delete" type="button" onClick={() => removeVenue(venue)} disabled={state.saving}>Eliminar</button></div></article>)}{venues.length === 0 && <p className="admin-empty">Todavía no hay recintos. Crea uno para poder publicar eventos.</p>}</div>
    </section>
    <div className="admin-inventory"><div className="admin-section-heading"><div><p className="admin-kicker">MICROSERVICIO INVENTARIO</p><h2>Disponibilidad por evento</h2></div><button className="admin-refresh" type="button" onClick={refresh}>Actualizar</button></div>{state.loading ? <p className="admin-empty">Cargando inventario...</p> : <div className="admin-table-wrap"><table><thead><tr><th>Evento</th><th>Total</th><th>Disponibles</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{inventory.map((item) => { const currentEvent = events.find((event) => event.idEvento === item.idEvento); return <tr key={item.idInventario}><td><strong>{currentEvent?.nombre || `Evento #${item.idEvento}`}</strong><small>{currentEvent?.lugar || 'Sin lugar'}</small></td><td>{item.cantidadTotal}</td><td>{item.cantidadDisponible}</td><td><span className={item.activo ? 'admin-badge active' : 'admin-badge'}>{item.activo ? 'Activo' : 'Inactivo'}</span></td><td className="admin-row-actions">{currentEvent && <><button className="admin-action-edit" type="button" onClick={() => startEditing(currentEvent)} disabled={state.saving}>Editar</button><button className="admin-action-delete" type="button" onClick={() => removeEvent(currentEvent)} disabled={state.saving}>Eliminar</button></>}</td></tr>; })}</tbody></table>{inventory.length === 0 && <p className="admin-empty">No hay inventarios registrados.</p>}</div>}</div>
  </section>;
}
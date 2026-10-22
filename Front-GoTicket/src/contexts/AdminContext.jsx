import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../services/api';

const AdminContext = createContext(null);

const defaultSector = { nombre: 'General', capacidad: '', precio: '', modalidad: 'GENERAL', color: '#702bd8', posicionX: 50, posicionY: 35 };

function sectorPosition(index, total) {
  const angle = (2 * Math.PI * index / Math.max(total, 1)) - Math.PI / 2;
  return {
    posicionX: Math.round(50 + 36 * Math.cos(angle)),
    posicionY: Math.round(50 + 36 * Math.sin(angle))
  };
}

export const emptyEventForm = {
  nombre: '', descripcion: '', categoria: 'musica', imagenUrl: '', urlEvento: '',
  fechaInicio: '', fechaFin: '', idRecinto: '', capacidad: '', precio: ''
};

const emptyVenueForm = { nombre: '', ciudad: '', direccion: '', capacidad: '' };

export function AdminProvider({ children }) {
  const [events, setEvents] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [venues, setVenues] = useState([]);
  const [form, setForm] = useState(emptyEventForm);
  const [sectors, setSectors] = useState([]);
  const [venueForm, setVenueForm] = useState(emptyVenueForm);
  const [venueSectors, setVenueSectors] = useState([{ ...defaultSector }]);
  const [editingVenueId, setEditingVenueId] = useState(null);
  const [editingEventId, setEditingEventId] = useState(null);
  const [state, setState] = useState({ loading: true, saving: false, error: '', success: '' });
  const venueSelection = useRef('');

  async function refresh() {
    setState((current) => ({ ...current, loading: true, error: '' }));
    try {
      const [loadedEvents, loadedInventory, loadedVenues] = await Promise.all([
        api.events(), api.inventory(), api.venues()
      ]);
      setEvents(loadedEvents || []);
      setInventory(loadedInventory || []);
      setVenues(loadedVenues || []);
    } catch (error) {
      setState((current) => ({ ...current, error: error.message }));
    } finally {
      setState((current) => ({ ...current, loading: false }));
    }
  }

  useEffect(() => { refresh(); }, []);

  function updateForm(event) {
    const { name, value } = event.target;
    const selectedVenue = name === 'idRecinto' ? venues.find((venue) => String(venue.idRecinto) === value) : null;
    setForm((current) => ({ ...current, [name]: value, ...(selectedVenue ? { capacidad: selectedVenue.capacidad } : {}) }));
    if (name === 'idRecinto') {
      venueSelection.current = value;
      setSectors([]);
      setState((current) => ({ ...current, error: '' }));
      if (!selectedVenue) {
        setForm((current) => ({ ...current, capacidad: '' }));
        return;
      }
      api.venueSectors(selectedVenue.idRecinto)
        .then((layout) => {
          if (venueSelection.current !== value) return;
          setSectors((layout || []).map((sector) => ({
            nombre: sector.nombre,
            capacidad: sector.capacidad,
            precio: form.precio || '',
            modalidad: sector.modalidad,
            color: sector.color || '#702bd8',
            posicionX: sector.posicionX,
            posicionY: sector.posicionY
          })));
          if (!layout?.length) {
            setState((current) => ({ ...current, error: 'El recinto todavía no tiene un mapa configurado.' }));
          }
        })
        .catch((error) => {
          if (venueSelection.current === value) {
            setState((current) => ({ ...current, error: `No se pudo cargar el mapa del recinto: ${error.message}` }));
          }
        });
    }
    if (sectors.length === 1 && sectors[0].nombre === 'General') {
      if (name === 'capacidad') updateSector(0, 'capacidad', value);
      if (name === 'precio') updateSector(0, 'precio', value);
    }
  }

  function startEditing(event) {
    const toInputDate = (value) => value ? value.slice(0, 16) : '';
    setEditingEventId(event.idEvento);
    setForm({
      nombre: event.nombre || '',
      descripcion: event.descripcion || '',
      categoria: event.categoria || 'musica',
      imagenUrl: event.imagenUrl || '',
      urlEvento: event.urlEvento || '',
      fechaInicio: toInputDate(event.fechaInicio),
      fechaFin: toInputDate(event.fechaFin),
      idRecinto: event.idRecinto ? String(event.idRecinto) : '',
      capacidad: event.capacidad || '',
      precio: event.precio ?? ''
    });
    setSectors([{ ...defaultSector, capacidad: event.capacidad || '', precio: event.precio ?? '' }]);
    setState((current) => ({ ...current, error: '', success: '' }));
    api.eventSectors(event.idEvento)
      .then((loaded) => {
        if (loaded?.length) {
          setSectors(loaded.map((sector, index) => ({
            nombre: sector.nombre,
            capacidad: sector.capacidad,
            precio: sector.precio,
            modalidad: sector.modalidad,
            color: sector.color || '#702bd8',
            posicionX: sector.posicionX ?? sectorPosition(index, loaded.length).posicionX,
            posicionY: sector.posicionY ?? sectorPosition(index, loaded.length).posicionY
          })));
        }
      })
      .catch((error) => setState((current) => ({ ...current, error: `No se pudieron cargar los sectores del evento: ${error.message}` })));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function cancelEditing() {
    setEditingEventId(null);
    setForm(emptyEventForm);
    setSectors([]);
    setState((current) => ({ ...current, error: '', success: '' }));
  }

  async function createEventWithInventory(event) {
    event.preventDefault();
    setState((current) => ({ ...current, saving: true, error: '', success: '' }));
    let createdEvent = null;
    let previousEvent = null;
    try {
      const payload = {
        ...form,
        capacidad: Number(form.capacidad),
        precio: form.precio === '' ? null : Number(form.precio),
        fechaFin: form.fechaFin || null,
        idRecinto: form.idRecinto ? Number(form.idRecinto) : null,
        imagenUrl: form.imagenUrl || null,
        urlEvento: form.urlEvento || null,
        activo: true
      };
      if (editingEventId) {
        previousEvent = events.find((currentEvent) => currentEvent.idEvento === editingEventId);
        const previousInventory = inventory.find((item) => item.idEvento === editingEventId);
        await api.updateEvent(editingEventId, payload);
        try {
          await api.updateInventoryByEvent(editingEventId, {
            cantidadTotal: Number(form.capacidad),
            activo: true
          });
          await api.configureEventSectors(editingEventId, normalizeSectors());
        } catch (updateError) {
          const rollbackErrors = [];
          if (previousInventory) {
            try {
              await api.updateInventoryByEvent(editingEventId, {
                cantidadTotal: previousInventory.cantidadTotal,
                activo: previousInventory.activo
              });
            } catch (rollbackError) {
              rollbackErrors.push(`inventario: ${rollbackError.message}`);
            }
          }
          if (previousEvent) {
            try {
              await api.updateEvent(editingEventId, previousEvent);
            } catch (rollbackError) {
              rollbackErrors.push(`evento: ${rollbackError.message}`);
            }
          }
          if (rollbackErrors.length) {
            throw new Error(`${updateError.message}. No se pudo restaurar ${rollbackErrors.join(' y ')}.`);
          }
          throw updateError;
        }
      } else {
        createdEvent = await api.createEvent(payload);
        await api.configureEventSectors(createdEvent.idEvento, normalizeSectors());
        await api.createInventory({
          idEvento: createdEvent.idEvento,
          cantidadTotal: Number(form.capacidad),
          cantidadDisponible: Number(form.capacidad),
          activo: true
        });
      }

      setForm(emptyEventForm);
      setSectors([]);
      setEditingEventId(null);
      setState((current) => ({ ...current, saving: false, success: editingEventId ? 'Evento actualizado correctamente.' : 'Evento e inventario creados correctamente.' }));
      await refresh();
    } catch (error) {
      if (createdEvent?.idEvento) {
        try { await api.deleteEvent(createdEvent.idEvento); } catch { /* preserve original error */ }
      }
      setState((current) => ({ ...current, saving: false, error: createdEvent ? `El evento se creó, pero no se pudo completar su configuración de inventario y sectores: ${error.message}` : `No se pudo completar la operación: ${error.message}` }));
    }
  }

  function normalizeSectors() {
    return sectors.map((sector) => ({
      ...sector,
      capacidad: Number(sector.capacidad),
      precio: Number(sector.precio)
    }));
  }

  function updateSector(index, field, value) {
    setSectors((current) => current.map((sector, position) =>
      position === index ? { ...sector, [field]: value } : sector));
  }

  function updateVenueForm(event) {
    const { name, value } = event.target;
    setVenueForm((current) => ({ ...current, [name]: value }));
    if (name === 'capacidad' && venueSectors.length === 1 && venueSectors[0].nombre === 'General') {
      updateVenueSector(0, 'capacidad', value);
    }
  }

  function editVenue(venue) {
    setEditingVenueId(venue.idRecinto);
    setVenueSectors([]);
    setVenueForm({
      nombre: venue.nombre || '',
      ciudad: venue.ciudad || '',
      direccion: venue.direccion || '',
      capacidad: venue.capacidad || ''
    });
    api.venueSectors(venue.idRecinto)
      .then((layout) => setVenueSectors((layout || []).map((sector) => ({
        nombre: sector.nombre,
        capacidad: sector.capacidad,
        modalidad: sector.modalidad,
        color: sector.color || '#702bd8',
        posicionX: sector.posicionX,
        posicionY: sector.posicionY
      }))))
      .catch((error) => setState((current) => ({ ...current, error: `No se pudo cargar el mapa del recinto: ${error.message}` })));
  }

  function cancelVenueEditing() {
    setEditingVenueId(null);
    setVenueForm(emptyVenueForm);
    setVenueSectors([{ ...defaultSector }]);
  }

  async function saveVenue(event) {
    event.preventDefault();
    const configuredCapacity = venueSectors.reduce((sum, sector) => sum + (Number(sector.capacidad) || 0), 0);
    if (configuredCapacity !== Number(venueForm.capacidad)) {
      setState((current) => ({
        ...current,
        error: `La capacidad del mapa (${configuredCapacity}) debe coincidir con el aforo del recinto (${Number(venueForm.capacidad)}).`
      }));
      return;
    }
    setState((current) => ({ ...current, saving: true, error: '', success: '' }));
    let savedVenue = null;
    try {
      const payload = { ...venueForm, capacidad: Number(venueForm.capacidad) };
      if (editingVenueId) {
        savedVenue = await api.updateVenue(editingVenueId, payload);
      } else {
        savedVenue = await api.createVenue(payload);
      }
      await api.configureVenueSectors(savedVenue.idRecinto, venueSectors.map(({ precio, ...sector }) => ({
        ...sector,
        capacidad: Number(sector.capacidad),
        posicionX: Number(sector.posicionX),
        posicionY: Number(sector.posicionY)
      })));
      setVenueForm(emptyVenueForm);
      setEditingVenueId(null);
      setVenueSectors([{ ...defaultSector }]);
      setState((current) => ({ ...current, saving: false, success: editingVenueId ? 'Recinto actualizado correctamente.' : 'Recinto creado correctamente.' }));
      await refresh();
    } catch (error) {
      setState((current) => ({
        ...current,
        saving: false,
        error: savedVenue
          ? `El recinto se guardó, pero no se pudo guardar su mapa: ${error.message}`
          : `No se pudo guardar el recinto: ${error.message}`
      }));
    }
  }

  function updateVenueSector(index, field, value) {
    setVenueSectors((current) => current.map((sector, position) =>
      position === index ? { ...sector, [field]: value } : sector));
  }

  function addVenueSector() {
    setVenueSectors((current) => [...current, {
      ...defaultSector,
      nombre: '',
      capacidad: '',
      precio: undefined,
      ...sectorPosition(current.length, current.length + 1)
    }]);
  }

  function removeVenueSector(index) {
    setVenueSectors((current) => current.filter((_, position) => position !== index));
  }

  async function removeVenue(venue) {
    if (!window.confirm(`¿Eliminar el recinto "${venue.nombre}"?`)) return;
    setState((current) => ({ ...current, saving: true, error: '', success: '' }));
    try {
      await api.deleteVenue(venue.idRecinto);
      if (editingVenueId === venue.idRecinto) cancelVenueEditing();
      setState((current) => ({ ...current, saving: false, success: 'Recinto eliminado correctamente.' }));
      await refresh();
    } catch (error) {
      setState((current) => ({ ...current, saving: false, error: `No se pudo eliminar "${venue.nombre}": ${error.message}` }));
    }
  }

  async function deleteEvent(event) {
    setState((current) => ({ ...current, saving: true, error: '', success: '' }));
    try {
      await api.deleteInventoryByEvent(event.idEvento);
      await api.deleteEvent(event.idEvento);
      setState((current) => ({ ...current, saving: false, success: 'Evento eliminado correctamente.' }));
      await refresh();
    } catch (error) {
      setState((current) => ({ ...current, saving: false, error: `No se pudo eliminar "${event.nombre}": ${error.message}` }));
    }
  }

  async function removeEvent(event) {
    if (!window.confirm(`¿Eliminar el evento "${event.nombre}"? Esta acción no se puede deshacer.`)) return;
    await deleteEvent(event);
  }

  const value = useMemo(() => ({
    events, inventory, venues, form, sectors, venueForm, venueSectors, state, editingEventId, editingVenueId,
    updateForm, updateSector, updateVenueForm, updateVenueSector,
    addVenueSector, removeVenueSector, editVenue,
    cancelVenueEditing, saveVenue, removeVenue, refresh, startEditing, cancelEditing,
    createEventWithInventory, removeEvent
  }), [events, inventory, venues, form, sectors, venueForm, venueSectors, state, editingEventId, editingVenueId]);
  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>;
}

export function useAdmin() {
  const context = useContext(AdminContext);
  if (!context) throw new Error('useAdmin debe utilizarse dentro de AdminProvider');
  return context;
}
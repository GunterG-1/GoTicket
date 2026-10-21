import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../services/api';

const AdminContext = createContext(null);

export const emptyEventForm = {
  nombre: '', descripcion: '', categoria: 'musica', imagenUrl: '', urlEvento: '',
  fechaInicio: '', fechaFin: '', lugar: '', capacidad: '', precio: ''
};

export function AdminProvider({ children }) {
  const [events, setEvents] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [form, setForm] = useState(emptyEventForm);
  const [editingEventId, setEditingEventId] = useState(null);
  const [state, setState] = useState({ loading: true, saving: false, error: '', success: '' });

  async function refresh() {
    setState((current) => ({ ...current, loading: true, error: '' }));
    try {
      const [loadedEvents, loadedInventory] = await Promise.all([api.events(), api.inventory()]);
      setEvents(loadedEvents || []);
      setInventory(loadedInventory || []);
    } catch (error) {
      setState((current) => ({ ...current, error: error.message }));
    } finally {
      setState((current) => ({ ...current, loading: false }));
    }
  }

  useEffect(() => { refresh(); }, []);

  function updateForm(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
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
      lugar: event.lugar || '',
      capacidad: event.capacidad || '',
      precio: event.precio ?? ''
    });
    setState((current) => ({ ...current, error: '', success: '' }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function cancelEditing() {
    setEditingEventId(null);
    setForm(emptyEventForm);
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
        imagenUrl: form.imagenUrl || null,
        urlEvento: form.urlEvento || null,
        activo: true
      };
      if (editingEventId) {
        previousEvent = events.find((currentEvent) => currentEvent.idEvento === editingEventId);
        await api.updateEvent(editingEventId, payload);
        try {
          await api.updateInventoryByEvent(editingEventId, {
            cantidadTotal: Number(form.capacidad),
            activo: true
          });
        } catch (inventoryError) {
          if (previousEvent) {
            await api.updateEvent(editingEventId, previousEvent);
          }
          throw inventoryError;
        }
      } else {
        createdEvent = await api.createEvent(payload);
        await api.createInventory({
          idEvento: createdEvent.idEvento,
          cantidadTotal: Number(form.capacidad),
          cantidadDisponible: Number(form.capacidad),
          activo: true
        });
      }
      setForm(emptyEventForm);
      setEditingEventId(null);
      setState((current) => ({ ...current, saving: false, success: editingEventId ? 'Evento actualizado correctamente.' : 'Evento e inventario creados correctamente.' }));
      await refresh();
    } catch (error) {
      if (createdEvent?.idEvento) {
        try { await api.deleteEvent(createdEvent.idEvento); } catch { /* preserve original error */ }
      }
      setState((current) => ({ ...current, saving: false, error: createdEvent ? `El evento se creó, pero no se pudo crear su inventario: ${error.message}` : `No se pudo completar la operación: ${error.message}` }));
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

  const value = useMemo(() => ({ events, inventory, form, state, editingEventId, updateForm, refresh, startEditing, cancelEditing, createEventWithInventory, removeEvent }), [events, inventory, form, state, editingEventId]);
  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>;
}

export function useAdmin() {
  const context = useContext(AdminContext);
  if (!context) throw new Error('useAdmin debe utilizarse dentro de AdminProvider');
  return context;
}
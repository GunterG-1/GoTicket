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

  async function createEventWithInventory(event) {
    event.preventDefault();
    setState((current) => ({ ...current, saving: true, error: '', success: '' }));
    let createdEvent = null;
    try {
      createdEvent = await api.createEvent({
        ...form,
        capacidad: Number(form.capacidad),
        precio: form.precio === '' ? null : Number(form.precio),
        fechaFin: form.fechaFin || null,
        imagenUrl: form.imagenUrl || null,
        urlEvento: form.urlEvento || null,
        activo: true
      });
      await api.createInventory({
        idEvento: createdEvent.idEvento,
        cantidadTotal: Number(form.capacidad),
        cantidadDisponible: Number(form.capacidad),
        activo: true
      });
      setForm(emptyEventForm);
      setState((current) => ({ ...current, saving: false, success: 'Evento e inventario creados correctamente.' }));
      await refresh();
    } catch (error) {
      if (createdEvent?.idEvento) {
        try { await api.deleteEvent(createdEvent.idEvento); } catch { /* preserve original error */ }
      }
      setState((current) => ({ ...current, saving: false, error: createdEvent ? `El evento se creó, pero no se pudo crear su inventario: ${error.message}` : `No se pudo completar el alta: ${error.message}` }));
    }
  }

  const value = useMemo(() => ({ events, inventory, form, state, updateForm, refresh, createEventWithInventory }), [events, inventory, form, state]);
  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>;
}

export function useAdmin() {
  const context = useContext(AdminContext);
  if (!context) throw new Error('useAdmin debe utilizarse dentro de AdminProvider');
  return context;
}
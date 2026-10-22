import { useEffect, useState } from 'react';
import { api } from '../services/api';
import './ProfilePage.css';

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ rut: '', telefono: '' });
  const [state, setState] = useState({ loading: true, saving: false, error: '', success: '' });

  useEffect(() => {
    let active = true;
    api.profile()
      .then((user) => {
        if (!active) return;
        setProfile(user);
        setForm({ rut: user.rut || '', telefono: user.telefono || '' });
      })
      .catch((error) => {
        if (active) setState((current) => ({ ...current, error: error.message }));
      })
      .finally(() => {
        if (active) setState((current) => ({ ...current, loading: false }));
      });
    return () => { active = false; };
  }, []);

  async function saveProfile(event) {
    event.preventDefault();
    setState((current) => ({ ...current, saving: true, error: '', success: '' }));
    try {
      const updated = await api.updateProfile(form);
      setProfile(updated);
      setForm({ rut: updated.rut || '', telefono: updated.telefono || '' });
      setState((current) => ({ ...current, saving: false, success: 'Tu perfil se actualizó correctamente.' }));
    } catch (error) {
      setState((current) => ({ ...current, saving: false, error: error.message }));
    }
  }

  return <section className="page-section profile-page">
    <p className="eyebrow">TU CUENTA GOTICKET</p>
    <h1>Mi perfil</h1>
    {state.loading ? <p className="empty-state">Cargando tu perfil...</p> : <>
      {state.error && <p className="error-message">{state.error}</p>}
      {state.success && <p className="profile-success">{state.success}</p>}
      {profile && <form className="profile-form" onSubmit={saveProfile}>
        <label>Nombre<input value={profile.nombre || ''} readOnly /></label>
        <label>Correo electrónico<input type="email" value={profile.email || ''} readOnly /></label>
        <label>RUT<input value={form.rut} onChange={(event) => setForm((current) => ({ ...current, rut: event.target.value }))} placeholder="12.345.678-5" maxLength="12" /></label>
        <label>Teléfono de contacto<input type="tel" value={form.telefono} onChange={(event) => setForm((current) => ({ ...current, telefono: event.target.value }))} placeholder="+56 9 1234 5678" maxLength="25" /></label>
        <p className="profile-help">El RUT se valida y normaliza antes de guardarse. Nombre y correo provienen de tu cuenta verificada de Google.</p>
        <button className="primary-button" type="submit" disabled={state.saving}>{state.saving ? 'Guardando...' : 'Guardar cambios'}</button>
      </form>}
    </>}
  </section>;
}

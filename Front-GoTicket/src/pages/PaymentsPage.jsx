import { useEffect, useState } from 'react';
import { api, getSession } from '../services/api';

const initialForm = { titular: '', numeroTarjeta: '', vencimiento: '', cvv: '' };

export default function PaymentsPage() {
  const [payments, setPayments] = useState([]);
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [form, setForm] = useState(initialForm);
  const [state, setState] = useState({ loading: true, processing: false, error: '', success: '' });
  const userId = getSession().user?.idUsuario;

  function loadPayments() {
    setState((current) => ({ ...current, loading: true }));
    api.payments(userId).then(setPayments).catch((requestError) => setState((current) => ({ ...current, error: requestError.message }))).finally(() => setState((current) => ({ ...current, loading: false })));
  }

  useEffect(() => { loadPayments(); }, []);

  function updateForm(event) {
    const { name, value } = event.target;
    const normalizedValue = name === 'numeroTarjeta' ? value.replace(/\D/g, '').slice(0, 19).replace(/(.{4})/g, '$1 ').trim() : name === 'cvv' ? value.replace(/\D/g, '').slice(0, 4) : name === 'vencimiento' ? value.replace(/\D/g, '').slice(0, 6).replace(/^(\d{2})(\d)/, '$1/$2') : value;
    setForm((current) => ({ ...current, [name]: normalizedValue }));
  }

  async function processPayment(event) {
    event.preventDefault();
    setState({ loading: false, processing: true, error: '', success: '' });
    try {
      const payment = await api.processPayment(selectedPayment.idPago, form);
      setPayments((current) => current.map((item) => item.idPago === payment.idPago ? payment : item));
      setSelectedPayment(null);
      setForm(initialForm);
      setState({ loading: false, processing: false, error: '', success: payment.estado === 'APROBADO' ? 'Pago aprobado. Tu orden está siendo confirmada.' : 'El pago fue rechazado y la reserva será liberada.' });
    } catch (requestError) {
      setState({ loading: false, processing: false, error: requestError.message, success: '' });
    }
  }

  return <section className="page-section"><p className="eyebrow">PAGOS</p><h2>Pagos y comprobantes.</h2><p className="muted">Procesa tu compra de forma segura. Nunca almacenamos el número completo de tu tarjeta.</p>{state.error && <p className="error-message">{state.error}</p>}{state.success && <p className="success-message">{state.success}</p>}<div className="table-wrap"><table><thead><tr><th>Pago</th><th>Orden</th><th>Método</th><th>Monto</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.idPago}><td>#{payment.idPago}</td><td>#{payment.idOrden}</td><td>{payment.metodoPago === 'TARJETA' ? `${payment.marcaTarjeta} **** ${payment.ultimosCuatro}` : 'Pendiente'}</td><td>${Number(payment.monto).toLocaleString('es-CL')}</td><td><span className="pill">{payment.estado}</span></td><td>{payment.estado === 'PENDIENTE' && <button className="secondary-button" onClick={() => { setSelectedPayment(payment); setState((current) => ({ ...current, error: '', success: '' })); }}>Pagar ahora</button>}</td></tr>)}</tbody></table>{state.loading && <p className="empty-state">Cargando pagos...</p>}{!state.loading && !state.error && payments.length === 0 && <p className="empty-state">No hay pagos registrados.</p>}</div>{selectedPayment && <div className="modal-backdrop" role="presentation"><section className="reservation-modal" role="dialog" aria-modal="true" aria-labelledby="payment-title"><button className="modal-close" onClick={() => setSelectedPayment(null)} aria-label="Cerrar pago">×</button><p className="eyebrow">PAGO SEGURO · ORDEN #{selectedPayment.idOrden}</p><h2 id="payment-title">Completa tu pago</h2><p className="muted">Total: ${Number(selectedPayment.monto).toLocaleString('es-CL')}</p><form className="reservation-form" onSubmit={processPayment}><label>Nombre del titular<input name="titular" value={form.titular} onChange={updateForm} autoComplete="cc-name" required /></label><label>Número de tarjeta<input name="numeroTarjeta" inputMode="numeric" value={form.numeroTarjeta} onChange={updateForm} autoComplete="cc-number" placeholder="0000 0000 0000 0000" required /></label><div className="payment-fields"><label>Vencimiento<input name="vencimiento" value={form.vencimiento} onChange={updateForm} placeholder="MM/AAAA" autoComplete="cc-exp" required /></label><label>CVV<input name="cvv" type="password" inputMode="numeric" value={form.cvv} onChange={updateForm} autoComplete="cc-csc" required /></label></div><button className="primary-button" type="submit" disabled={state.processing}>{state.processing ? 'Validando pago...' : 'Pagar ahora'}</button></form></section></div>}</section>;
}

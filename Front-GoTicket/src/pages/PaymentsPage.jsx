import { useEffect, useState } from 'react';
import { api, getSession } from '../services/api';

const paymentMethods = [
  { id: 'TARJETA', title: 'Tarjeta', description: 'Crédito o débito', icon: '▰' },
  { id: 'BANCONTACT', title: 'Bancontact', description: 'Pago bancario simulado', icon: 'B' },
  { id: 'SEPA', title: 'Transferencia SEPA', description: 'Transferencia simulada', icon: '⇄' }
];

function formatAmount(amount) {
  return `$${Number(amount || 0).toLocaleString('es-CL')}`;
}

function detectCardBrand(cardNumber) {
  const digits = cardNumber.replace(/\D/g, '');
  if (/^4/.test(digits)) return 'VISA';
  if (/^(5[1-5]|2[2-7])/.test(digits)) return 'MASTERCARD';
  if (/^3[47]/.test(digits)) return 'AMEX';
  return null;
}

function formatCardNumber(value) {
  return value.replace(/\D/g, '').slice(0, 19).replace(/(.{4})/g, '$1 ').trim();
}

function formatExpiry(value) {
  const digits = value.replace(/\D/g, '').slice(0, 4);
  return digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
}

export default function PaymentsPage() {
  const userId = getSession().user?.idUsuario;
  const requestedOrderId = new URLSearchParams(window.location.hash.split('?')[1] || '').get('orden');
  const [payments, setPayments] = useState([]);
  const [orders, setOrders] = useState([]);
  const [activeOrder, setActiveOrder] = useState(null);
  const [activeEvent, setActiveEvent] = useState(null);
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [paymentStep, setPaymentStep] = useState('details');
  const [paymentMethod, setPaymentMethod] = useState('TARJETA');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvc, setCardCvc] = useState('');
  const [secondsRemaining, setSecondsRemaining] = useState(0);
  const [state, setState] = useState({ loading: true, processing: false, error: '', success: '' });

  useEffect(() => {
    let active = true;
    async function loadOrders() {
      try {
        const loadedOrders = await api.orders(userId);
        if (!active) return;
        const safeOrders = loadedOrders || [];
        setOrders(safeOrders);
        if (requestedOrderId) {
          const order = safeOrders.find((item) => String(item.idOrden) === requestedOrderId);
          if (!order) {
            setState((current) => ({ ...current, error: `No se encontró la orden #${requestedOrderId}.` }));
            return;
          }
          setActiveOrder(order);
          const event = await api.event(order.idEvento);
          if (active) setActiveEvent(event);
        }
      } catch (requestError) {
        if (active) setState((current) => ({ ...current, error: requestError.message }));
      }
    }
    loadOrders();
    return () => { active = false; };
  }, [userId, requestedOrderId]);

  useEffect(() => {
    let active = true;
    let requestInFlight = false;
    let pollingTimer = null;
    async function loadPayments() {
      if (requestInFlight) return;
      requestInFlight = true;
      try {
        const loadedPayments = await api.payments(userId);
        if (!active) return;
        const safePayments = loadedPayments || [];
        setPayments(safePayments);
        if (requestedOrderId) {
          const payment = safePayments.find((item) => String(item.idOrden) === requestedOrderId);
          if (payment) {
            setSelectedPayment(payment);
            if (pollingTimer) window.clearInterval(pollingTimer);
            if (payment.estado !== 'PENDIENTE') {
              setState((current) => ({
                ...current,
                success: `El pago de la orden #${requestedOrderId} está ${payment.estado}.`
              }));
            }
          }
        }
        setState((current) => ({ ...current, loading: false }));
      } catch (requestError) {
        if (active) setState((current) => ({ ...current, loading: false, error: requestError.message }));
      } finally {
        requestInFlight = false;
      }
    }
    loadPayments();
    if (requestedOrderId) pollingTimer = window.setInterval(loadPayments, 2000);
    return () => {
      active = false;
      if (pollingTimer) window.clearInterval(pollingTimer);
    };
  }, [userId, requestedOrderId]);

  useEffect(() => {
    if (!selectedPayment?.expiraReserva && !selectedPayment?.fechaCreacion) {
      setSecondsRemaining(0);
      return undefined;
    }
    const expiresAt = selectedPayment.expiraReserva
      ? new Date(selectedPayment.expiraReserva).getTime()
      : new Date(selectedPayment.fechaCreacion).getTime() + 5 * 60 * 1000;
    const updateRemaining = () => setSecondsRemaining(Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000)));
    updateRemaining();
    const timer = window.setInterval(updateRemaining, 1000);
    return () => window.clearInterval(timer);
  }, [selectedPayment]);

  async function showPaymentDetails(payment) {
    const order = orders.find((item) => String(item.idOrden) === String(payment.idOrden));
    if (!order) {
      setState((current) => ({ ...current, error: `No se encontró el detalle de la orden #${payment.idOrden}.` }));
      return;
    }
    setState((current) => ({ ...current, error: '', success: '' }));
    setActiveOrder(order);
    setSelectedPayment(payment);
    setPaymentStep('details');
    setDetailOpen(true);
    try {
      setActiveEvent(await api.event(order.idEvento));
    } catch (requestError) {
      setState((current) => ({ ...current, error: requestError.message }));
    }
  }

  function openPaymentMethods() {
    setDetailOpen(false);
    setPaymentMethod('TARJETA');
    setCardNumber('');
    setCardExpiry('');
    setCardCvc('');
    setState((current) => ({ ...current, error: '', success: '' }));
    setPaymentStep('method');
  }

  async function simulatePayment() {
    let brand = null;
    let lastFour = null;
    if (paymentMethod === 'TARJETA') {
      const digits = cardNumber.replace(/\D/g, '');
      const [monthValue, yearValue] = cardExpiry.split('/');
      const month = Number(monthValue);
      const year = Number(`20${yearValue}`);
      const expiryDate = new Date(year, month, 0, 23, 59, 59);
      if (digits.length < 13 || digits.length > 19 || !/^\d{2}\/\d{2}$/.test(cardExpiry)
          || month < 1 || month > 12 || expiryDate < new Date()
          || !/^\d{3,4}$/.test(cardCvc)) {
        setState((current) => ({
          ...current,
          error: 'Revisa el número de tarjeta, la fecha de vencimiento y el código de seguridad.'
        }));
        return;
      }
      brand = detectCardBrand(cardNumber);
      lastFour = digits.slice(-4);
    }

    setState((current) => ({ ...current, processing: true, error: '', success: '' }));
    try {
      const payment = await api.simulatePayment(selectedPayment.idPago, true, userId, {
        metodoPago: paymentMethod,
        marcaTarjeta: brand,
        ultimosCuatro: lastFour
      });
      setPayments((current) => current.map((item) => item.idPago === payment.idPago ? payment : item));
      setSelectedPayment(payment);
      setPaymentStep('details');
      setCardNumber('');
      setCardExpiry('');
      setCardCvc('');
      setState({
        loading: false,
        processing: false,
        error: '',
        success: payment.estado === 'APROBADO'
          ? 'Pago de demostración aprobado. Tu entrada se está emitiendo.'
          : 'La reserva expiró y el pago fue rechazado.'
      });
    } catch (requestError) {
      setState((current) => ({ ...current, processing: false, error: requestError.message }));
    }
  }

  const orderTotal = activeOrder?.total ?? selectedPayment?.monto;
  const orderQuantity = activeOrder?.cantidad ?? selectedPayment?.cantidad;
  const canPay = selectedPayment?.estado === 'PENDIENTE' && secondsRemaining > 0;

  return (
    <section className="page-section">
      <p className="eyebrow">PAGOS</p>
      <h2>Detalle de compra y pago.</h2>
      {state.error && <p className="error-message" role="alert">{state.error}</p>}
      {state.success && <p className="success-message" role="status">{state.success}</p>}

      {activeOrder && (
        <article className="checkout-summary">
          <div>
            <p className="eyebrow">RESUMEN DE COMPRA · ORDEN #{activeOrder.idOrden}</p>
            <h3>{activeEvent?.nombre || `Evento #${activeOrder.idEvento}`}</h3>
            <p className="muted">{activeEvent?.lugar || activeEvent?.recinto || 'Recinto por confirmar'}</p>
            {activeEvent?.fechaInicio && <p>{new Date(activeEvent.fechaInicio).toLocaleString('es-CL')}</p>}
          </div>
          <dl className="checkout-details">
            <div><dt>Evento</dt><dd>#{activeOrder.idEvento}</dd></div>
            <div><dt>Sector</dt><dd>{activeOrder.sector || 'General'}</dd></div>
            <div><dt>Entradas</dt><dd>{orderQuantity || 0}</dd></div>
            <div><dt>Precio por entrada</dt><dd>{formatAmount(activeOrder.precioUnitario ?? (Number(orderTotal) / (orderQuantity || 1)))}</dd></div>
            <div className="checkout-total"><dt>Total</dt><dd>{formatAmount(orderTotal)}</dd></div>
          </dl>
          {selectedPayment?.estado === 'PENDIENTE'
            ? <div className="checkout-action">
                <p>Reserva disponible por {Math.floor(secondsRemaining / 60)}:{String(secondsRemaining % 60).padStart(2, '0')}</p>
                <button className="primary-button" type="button" onClick={openPaymentMethods} disabled={!canPay}>
                  {canPay ? 'Pagar' : 'Reserva expirada'}
                </button>
              </div>
            : state.loading
              ? <p className="empty-state">Estamos preparando el pago de esta orden…</p>
              : selectedPayment
                ? <p className="pill">Estado del pago: {selectedPayment.estado}</p>
                : <p className="empty-state">Estamos registrando el pago de la orden. Esta pantalla se actualizará automáticamente.</p>}
        </article>
      )}

      <div className="table-wrap">
        <table>
          <thead><tr><th>Pago</th><th>Orden</th><th>Método</th><th>Monto</th><th>Estado</th><th>Acción</th></tr></thead>
          <tbody>
            {payments.map((payment) => (
              <tr key={payment.idPago}>
                <td>#{payment.idPago}</td>
                <td>#{payment.idOrden}</td>
                <td>{payment.metodoPago === 'PENDIENTE' ? 'Por elegir' : payment.metodoPago}</td>
                <td>{formatAmount(payment.monto)}</td>
                <td><span className="pill">{payment.estado}</span></td>
                <td>{payment.estado === 'PENDIENTE' && (
                  <button className="secondary-button" type="button" onClick={() => showPaymentDetails(payment)}>
                    Ver detalle
                  </button>
                )}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {state.loading && <p className="empty-state">Cargando tus órdenes y pagos…</p>}
        {!state.loading && !state.error && payments.length === 0 && !activeOrder
          && <p className="empty-state">No hay pagos registrados.</p>}
      </div>

      {selectedPayment && paymentStep === 'method' && (
        <div className="modal-backdrop" role="presentation">
          <section className="reservation-modal payment-modal" role="dialog" aria-modal="true" aria-labelledby="payment-title">
            <button className="modal-close" type="button" onClick={() => setPaymentStep('details')} aria-label="Cerrar pago">×</button>
            <p className="eyebrow">PAGO SIMULADO · ORDEN #{selectedPayment.idOrden}</p>
            <h2 id="payment-title">Elige cómo pagar</h2>
            <p className="muted">Total a pagar: <strong>{formatAmount(selectedPayment.monto)}</strong></p>
            <div className="payment-method-options" role="group" aria-label="Método de pago">
              {paymentMethods.map((method) => (
                <button
                  className={`payment-method-option${paymentMethod === method.id ? ' is-selected' : ''}`}
                  key={method.id}
                  type="button"
                  aria-pressed={paymentMethod === method.id}
                  onClick={() => { setPaymentMethod(method.id); setState((current) => ({ ...current, error: '' })); }}
                >
                  <span className="payment-method-icon" aria-hidden="true">{method.icon}</span>
                  <span><strong>{method.title}</strong><small>{method.description}</small></span>
                </button>
              ))}
            </div>
            {paymentMethod === 'TARJETA' && (
              <div className="payment-card-fields">
                <label>Número de tarjeta
                  <input autoComplete="cc-number" inputMode="numeric" placeholder="1234 5678 9012 3456" value={cardNumber} onChange={(event) => setCardNumber(formatCardNumber(event.target.value))} />
                </label>
                <div className="payment-fields">
                  <label>Vencimiento
                    <input autoComplete="cc-exp" inputMode="numeric" placeholder="MM/AA" value={cardExpiry} onChange={(event) => setCardExpiry(formatExpiry(event.target.value))} />
                  </label>
                  <label>Código de seguridad
                    <input autoComplete="cc-csc" inputMode="numeric" placeholder="CVC" value={cardCvc} onChange={(event) => setCardCvc(event.target.value.replace(/\D/g, '').slice(0, 4))} />
                  </label>
                </div>
                <p className="muted payment-demo-note">Simulación local: no se realiza ningún cobro y los datos de tarjeta no se envían ni se guardan.</p>
              </div>
            )}
            {paymentMethod !== 'TARJETA' && (
              <p className="payment-demo-note">Este método se confirma solo como demostración; no se inicia una transferencia real.</p>
            )}
            {state.error && <p className="error-message" role="alert">{state.error}</p>}
            <div className="payment-modal-actions">
              <button className="secondary-button" type="button" onClick={() => setPaymentStep('details')} disabled={state.processing}>Volver</button>
              <button className="primary-button" type="button" onClick={simulatePayment} disabled={state.processing || !canPay}>
                {state.processing ? 'Procesando…' : `Pagar ${formatAmount(selectedPayment.monto)}`}
              </button>
            </div>
          </section>
        </div>
      )}
      {selectedPayment && detailOpen && paymentStep === 'details' && (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) setDetailOpen(false);
        }}>
          <section className="reservation-modal payment-modal" role="dialog" aria-modal="true" aria-labelledby="order-detail-title">
            <button className="modal-close" type="button" onClick={() => setDetailOpen(false)} aria-label="Cerrar detalle">×</button>
            <p className="eyebrow">DETALLE DE COMPRA · ORDEN #{activeOrder?.idOrden}</p>
            <h2 id="order-detail-title">{activeEvent?.nombre || `Evento #${activeOrder?.idEvento}`}</h2>
            <p className="muted">{activeEvent?.lugar || activeEvent?.recinto || 'Recinto por confirmar'}</p>
            {activeEvent?.fechaInicio && <p>{new Date(activeEvent.fechaInicio).toLocaleString('es-CL')}</p>}
            <dl className="checkout-details">
              <div><dt>Evento</dt><dd>#{activeOrder?.idEvento}</dd></div>
              <div><dt>Sector</dt><dd>{activeOrder?.sector || 'General'}</dd></div>
              <div><dt>Entradas</dt><dd>{activeOrder?.cantidad ?? selectedPayment.cantidad ?? 0}</dd></div>
              <div><dt>Precio por entrada</dt><dd>{formatAmount(activeOrder?.precioUnitario ?? (Number(selectedPayment.monto) / (activeOrder?.cantidad || selectedPayment.cantidad || 1)))}</dd></div>
              <div className="checkout-total"><dt>Total</dt><dd>{formatAmount(activeOrder?.total ?? selectedPayment.monto)}</dd></div>
              <div><dt>Estado del pago</dt><dd>{selectedPayment.estado}</dd></div>
            </dl>
            {state.error && <p className="error-message" role="alert">{state.error}</p>}
            <div className="payment-modal-actions">
              <button className="secondary-button" type="button" onClick={() => setDetailOpen(false)}>Cerrar</button>
              {selectedPayment.estado === 'PENDIENTE' && (
                <button
                  className="primary-button"
                  type="button"
                  onClick={openPaymentMethods}
                  disabled={!canPay}
                >
                  {canPay ? 'Continuar a pagar' : 'Reserva expirada'}
                </button>
              )}
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

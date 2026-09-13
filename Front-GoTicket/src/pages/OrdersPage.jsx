import { useEffect, useState } from 'react';
import { api, getSession } from '../services/api';

export default function OrdersPage() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  useEffect(() => { api.orders(getSession().user?.idUsuario).then(setOrders).catch((requestError) => setError(requestError.message)); }, []);
  return <section className="page-section"><p className="eyebrow">ORDENES</p><h2>Tu historial de reservas.</h2>{error && <p className="error-message">{error}</p>}<div className="table-wrap"><table><thead><tr><th>Orden</th><th>Evento</th><th>Sector</th><th>Cantidad</th><th>Total</th><th>Estado</th></tr></thead><tbody>{orders.map((order) => <tr key={order.idOrden}><td>#{order.idOrden}</td><td>{order.idEvento}</td><td>{order.sector || 'General'}</td><td>{order.cantidad}</td><td>${Number(order.total).toLocaleString('es-CL')}</td><td><span className="pill">{order.estado}</span></td></tr>)}</tbody></table>{!error && orders.length === 0 && <p className="empty-state">No tienes órdenes todavía.</p>}</div></section>;
}

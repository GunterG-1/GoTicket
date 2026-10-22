export function isEventUpcoming(fechaInicio, now = Date.now()) {
  if (!fechaInicio) return false;
  const startTime = new Date(fechaInicio).getTime();
  return Number.isFinite(startTime) && startTime > now;
}

export function canPurchaseEvent(event, now = Date.now()) {
  return Boolean(event?.activo) && isEventUpcoming(event.fechaInicio, now);
}

import { useEffect, useRef, useState } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { api } from '../services/api';

export default function AccessPage() {
  const scannerRef = useRef(null);
  const scanInProgress = useRef(false);
  const [state, setState] = useState({ checking: false, error: '', result: null });

  useEffect(() => {
    const scanner = new Html5QrcodeScanner('goticket-access-reader', {
      fps: 8,
      qrbox: { width: 240, height: 240 },
      rememberLastUsedCamera: true
    }, false);
    scannerRef.current = scanner;
    scanner.render(async (code) => {
      if (scanInProgress.current) return;
      scanInProgress.current = true;
      setState({ checking: true, error: '', result: null });
      try {
        const entry = await api.validateAccess(code);
        setState({ checking: false, error: '', result: entry });
      } catch (error) {
        setState({ checking: false, error: error.message, result: null });
      } finally {
        await scanner.clear().catch((error) => console.warn('No se pudo cerrar el lector QR:', error));
      }
    }, () => {});
    return () => {
      scanner.clear().catch((error) => console.warn('No se pudo cerrar el lector QR:', error));
      scannerRef.current = null;
    };
  }, []);

  function restartScanner() {
    scanInProgress.current = false;
    setState({ checking: false, error: '', result: null });
    scannerRef.current?.render(async (code) => {
      if (scanInProgress.current) return;
      scanInProgress.current = true;
      setState({ checking: true, error: '', result: null });
      try {
        const entry = await api.validateAccess(code);
        setState({ checking: false, error: '', result: entry });
      } catch (error) {
        setState({ checking: false, error: error.message, result: null });
      } finally {
        await scannerRef.current?.clear().catch((error) => console.warn('No se pudo cerrar el lector QR:', error));
      }
    }, () => {});
  }

  return <section className="page-section">
    <p className="eyebrow">CONTROL DE ACCESO</p>
    <h2>Escanear entrada</h2>
    <p className="muted">El lector valida el código una sola vez y marca la entrada como utilizada. Esta función está restringida al personal administrador.</p>
    <div id="goticket-access-reader" />
    {state.checking && <p role="status">Validando entrada...</p>}
    {state.error && <p className="error-message" role="alert">{state.error}</p>}
    {state.result && <div className="success-message" role="status">
      Acceso autorizado · Entrada #{state.result.idEntrada} · Sector {state.result.sector} · Asiento {state.result.numeroAsiento}
    </div>}
    {(state.result || state.error) && <button className="secondary-button" type="button" onClick={restartScanner}>Escanear otra entrada</button>}
  </section>;
}

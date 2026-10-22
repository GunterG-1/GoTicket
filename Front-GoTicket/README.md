# Frontend GoTicket

Aplicación web React para consultar eventos, reservar entradas, revisar órdenes y administrar el catálogo.

## Funcionalidades

- Inicio de sesión mediante Google OAuth.
- Consulta de eventos e inventario.
- Reserva y confirmación de entradas.
- Consulta de órdenes y pagos.
- Panel administrativo para gestionar eventos e inventario.

## Tecnologías

- React 18
- Vite
- Bootstrap
- Nginx para producción
- Docker

## Requisitos

- Node.js 20 o superior
- npm

## Desarrollo local

Instalar dependencias:

```bash
npm ci
```

Iniciar Vite:

```bash
npm run dev
```

La aplicación se abre normalmente en:

```text
http://localhost:5173
```

Para indicar la URL del API Manager:

```powershell
$env:VITE_API_MANAGER_URL="http://localhost:8080"
npm run dev
```

## Construcción

```bash
npm run build
```

## Docker

El Compose público construye el frontend y lo sirve con Nginx:

```bash
docker compose --env-file .env.public -f ../docker-compose.public.yml up -d --build frontend
```

El frontend usa rutas relativas en producción. Nginx envía `/api`, `/oauth2` y `/login` hacia API Manager. La configuración de Nginx se procesa al iniciar el contenedor para resolver las rutas de documentación usando `BACKEND_HOST`.

## Seguridad

No guardes credenciales OAuth, claves privadas ni archivos `.env` en el repositorio.

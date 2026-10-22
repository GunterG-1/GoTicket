# GoTicket

GoTicket es una plataforma de venta y reserva de entradas para eventos. El proyecto está dividido en un frontend React, un API Manager y cuatro microservicios Java Spring Boot.

Este documento explica la estructura del proyecto y cómo ejecutarlo en un entorno local con `localhost`.

## Arquitectura del proyecto

```text
Navegador
   │
   ▼
Frontend React/Vite
http://localhost:5173
   │
   ▼
API Manager
http://localhost:8080
   │
   ├── Catálogo de Eventos
   │   http://localhost:8081
   ├── Inventario
   │   http://localhost:8082
   ├── Orden
   │   http://localhost:8083
   └── Pagos
       http://localhost:8084

Microservicios ──► MySQL
Inventario ──────► Redis
Orden/Pagos/Inventario ──► SNS/SQS para eventos
```

## Componentes

| Carpeta | Componente | Puerto | Función |
|---|---|---:|---|
| `Front-GoTicket` | Frontend React | 5173 | Interfaz web para usuarios y administradores |
| `APIManager` | API Gateway | 8080 | Autenticación, OAuth2, JWT y enrutamiento |
| `Catalogo-Evento` | Microservicio de catálogo | 8081 | Creación y consulta de eventos |
| `Inventario` | Microservicio de inventario | 8082 | Disponibilidad, reservas y liberación de entradas |
| `Orden` | Microservicio de órdenes | 8083 | Creación y seguimiento de órdenes |
| `Pagos` | Microservicio de pagos | 8084 | Procesamiento y estado de pagos |

### Sectores y capacidad de eventos

Los administradores gestionan recintos con ciudad, dirección y capacidad, y configuran en cada uno un mapa reutilizable de sectores, aforo, modalidad, color y posición X/Y (porcentaje del plano). Los mapas iniciales proponen distribuciones según el recinto (estadios, arenas, teatros y espacios abiertos); los eventos existentes heredan esos nombres, colores y posiciones, ajustando el aforo proporcionalmente a su capacidad. Cada evento conserva su precio base como precio inicial de sus sectores, que el administrador puede ajustar por separado. El evento ya no requiere un campo de lugar independiente: el recinto determina su ubicación. La suma de capacidades de los sectores del mapa debe coincidir con el aforo del recinto, y el evento no puede superar la capacidad del recinto. Inventario consulta los sectores y sus precios por evento al publicar disponibilidad y crear entradas. La reserva muestra el plano interactivo con la disponibilidad y los precios reales, sin estimaciones.

La selección actual es por sector. Las posiciones del plano son configurables por el administrador, pero la modalidad `NUMERADO` todavía es solo metadata: no existe selección ni asignación individual de asientos.

### Perfil de cliente

En el primer inicio de sesión se registra el identificador estable de Google y se crea el perfil en MySQL del Catálogo. Para no dejar sin titular las órdenes, pagos y entradas existentes, el ID inicial conserva el valor histórico derivado del correo; los siguientes inicios de sesión vinculan el perfil por el `sub` de Google y mantienen ese ID aunque cambie el correo. El cliente puede guardar RUT chileno y teléfono desde **Mi perfil**. Los endpoints de perfil solo se consumen desde API Manager usando el token interno.

### Entradas PDF y correo

Cuando Inventario procesa un pago aprobado, guarda una notificación pendiente en la misma transacción que emite las entradas. El procesador genera un PDF por entrada con su QR y los envía como adjuntos al correo del perfil. Los envíos fallidos se conservan y se reintentan; el propietario también puede descargar el PDF desde **Mis entradas**. Configura `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` y `MAIL_FROM` en `.env.backend`. Los valores de ejemplo no son credenciales válidas y no se envían correos reales hasta configurar un proveedor SMTP.

## Tecnologías

- Java 17.
- Spring Boot 3.2.5.
- Spring Data JPA e Hibernate.
- MySQL para la persistencia de los microservicios.
- Redis para reservas temporales del inventario.
- Amazon SNS/SQS para la comunicación basada en eventos.
- React 18, Vite y Bootstrap para el frontend.
- Docker y Docker Compose.
- H2 en memoria para el API Manager.

## Requisitos locales

Instala las siguientes herramientas:

- Git.
- Java 17.
- Docker Desktop con Docker Compose.
- Node.js 20 o superior y npm.
- Maven 3.9 o superior, opcional si se usa `mvnw`.
- MySQL 8/MariaDB 10.5 o Docker para ejecutar MySQL.

Comprueba las versiones:

```powershell
java -version
docker --version
docker compose version
node --version
npm --version
```

## 1. Clonar el proyecto

```powershell
git clone https://github.com/GunterG-1/GoTicket.git
cd GoTicket
```

## 2. Preparar y levantar GoTicket localmente

Desde la raíz del repositorio, crea un `.env.backend` local con secretos aleatorios (el script no sobrescribe un archivo existente):

```powershell
.\scripts\prepare-local.ps1
```

El entorno Compose local incluye MySQL, Redis, LocalStack (SNS/SQS), Mailpit, los cuatro microservicios y API Manager. Los recursos AWS son simulados por LocalStack; no se usan endpoints ni credenciales de AWS reales. Los puertos publicados de MySQL son `3307` en el equipo y `3306` entre contenedores.

Construye y levanta los servicios:

```powershell
docker compose --env-file .env.backend -f docker-compose.local.yml up -d --build
```

La inicialización de LocalStack crea el topic SNS, siete colas SQS y sus filtros de eventos. La primera construcción descarga imágenes y dependencias y puede tardar varios minutos.

Revisa el estado:

```powershell
docker compose --env-file .env.backend -f docker-compose.local.yml ps
```

Consulta los logs:

```powershell
docker compose --env-file .env.backend -f docker-compose.local.yml logs -f localstack-init catalogo inventario orden pagos apimanager
```

Los servicios pueden tardar unos segundos en iniciar mientras se construyen las imágenes y se crean las tablas.

## 3. Levantar el frontend

En otra terminal:

```powershell
cd Front-GoTicket
npm ci
$env:VITE_API_MANAGER_URL="http://localhost:8080"
npm run dev
```

Abre la aplicación en:

```text
http://localhost:5173
```

Para iniciar sesión y comprar, configura `OAUTH2_CLIENT_ID` y `OAUTH2_CLIENT_SECRET` en `.env.backend` con credenciales de Google OAuth cuyo URI de retorno autorizado sea `http://localhost:8080/login/oauth2/code/google`. Luego recrea API Manager para aplicar las variables nuevas:

```powershell
docker compose --env-file .env.backend -f docker-compose.local.yml up -d --force-recreate apimanager
```

Sin credenciales OAuth válidas se puede revisar la interfaz y los endpoints públicos, pero las compras requieren un usuario autenticado. Las credenciales locales de reemplazo solo permiten que API Manager arranque; no permiten iniciar sesión con Google.

## URLs locales

| Recurso | URL |
|---|---|
| Frontend | `http://localhost:5173` |
| API Manager | `http://localhost:8080` |
| Health del API Manager | `http://localhost:8080/actuator/health` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| OpenAPI | `http://localhost:8080/v3/api-docs` |
| Consola H2 | `http://localhost:8080/h2-console` |
| Catálogo | `http://localhost:8081` |
| Inventario | `http://localhost:8082` |
| Orden | `http://localhost:8083` |
| Pagos | `http://localhost:8084` |
| Mailpit (bandeja SMTP local) | `http://localhost:8025` |
| LocalStack (SNS/SQS local) | `http://localhost:4566` |
| MySQL desde el equipo | `localhost:3307` |

## Comprobar descuento de inventario

Después de iniciar sesión, abre un evento y registra el valor `cantidadDisponible` en `GET http://localhost:8080/api/proxy/inventarios/evento/{idEvento}`. Completa un checkout de una entrada y vuelve a consultar el mismo endpoint: la disponibilidad agregada debe bajar en uno. La reserva temporal se crea primero; al confirmarse el checkout, Inventario descuenta el stock. Si el pago se rechaza o vence la reserva, el flujo de liberación devuelve la disponibilidad. La disponibilidad detallada por sector también se consulta en `GET http://localhost:8080/api/proxy/inventarios/evento/{idEvento}/sectores`.

Los correos generados localmente no se envían a destinatarios reales: revisa los mensajes y PDFs adjuntos en [Mailpit](http://localhost:8025).

## Detener el proyecto

Detener los microservicios:

```powershell
docker compose --env-file .env.backend -f docker-compose.local.yml down
```

Detenerlos y eliminar los datos persistidos de Redis:

```powershell
docker compose --env-file .env.backend -f docker-compose.local.yml down -v
```

`down -v` elimina las bases locales y Redis: úsalo solo si quieres borrar los datos de prueba.

## Ejecución de pruebas y compilación

Cada microservicio Java tiene su propio Maven Wrapper. Por ejemplo:

```powershell
cd Catalogo-Evento
.\mvnw.cmd test
cd ..
```

Repite el comando en `Inventario`, `Orden`, `Pagos` y `APIManager` según sea necesario.

Para validar el frontend:

```powershell
cd Front-GoTicket
npm run build
```

## Problemas frecuentes

### No se puede conectar a MySQL

Verifica que el contenedor esté activo:

```powershell
docker ps
```

En el Compose local, los servicios usan el hostname `mysql` y el puerto interno `3306`. El puerto publicado `3307` es solo para conectarse desde el equipo anfitrión.

### El API Manager no encuentra un microservicio

Confirma que los cinco contenedores estén activos y que las URLs utilicen los puertos `8081` a `8084`.

### El frontend no carga datos

Confirma que el API Manager esté ejecutándose en `http://localhost:8080` y que hayas establecido:

```powershell
$env:VITE_API_MANAGER_URL="http://localhost:8080"
```

### No funciona el login de Google

Verifica que el URI registrado sea exactamente:

```text
http://localhost:8080/login/oauth2/code/google
```

Además, comprueba que `OAUTH2_CLIENT_ID` y `OAUTH2_CLIENT_SECRET` estén definidos antes de iniciar el API Manager.

## Seguridad

No subas al repositorio:

- `.env`
- `.env.backend`
- `.env.public`
- Client secrets de Google.
- Contraseñas de MySQL.
- Claves privadas.
- Credenciales AWS.

Los archivos `.env.backend.example` y `.env.public.example` son solamente plantillas.

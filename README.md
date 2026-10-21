# GoTicket

GoTicket es una plataforma de venta y reserva de entradas para eventos. El proyecto está dividido en un frontend React, un API Manager y cuatro microservicios Java Spring Boot.

Este documento explica la estructura del proyecto y cómo ejecutarlo en un entorno local con `localhost`. No contiene instrucciones de despliegue en AWS ni de producción.

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

## 2. Preparar MySQL local

Puedes utilizar una instalación local de MySQL/MariaDB o iniciar un contenedor. La siguiente opción crea una instancia local para desarrollo:

```powershell
docker run --name goticket-mysql `
  -e MYSQL_ROOT_PASSWORD=root `
  -p 3306:3306 `
  -d mysql:8.0
```

Espera unos segundos hasta que MySQL acepte conexiones. Los microservicios crean sus bases de datos automáticamente cuando se conectan:

```text
goticket_catalogo
goticket_inventario
goticket_orden
goticket_pagos
```

Si ya existe un contenedor con ese nombre, inícialo:

```powershell
docker start goticket-mysql
```

## 3. Configurar el backend local

El Compose de backend necesita un archivo `.env.backend`. Créalo en la raíz del proyecto:

```powershell
Copy-Item .env.backend.example .env.backend
```

Para ejecutar los microservicios dentro de Docker y conectarlos al MySQL publicado en el equipo, edita `.env.backend` con estos valores:

```env
MYSQL_HOST=host.docker.internal
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=root
REDIS_HOST=redis
REDIS_PORT=6379
AWS_REGION=us-east-1
GOTICKET_SNS_TOPIC_ARN=
GOTICKET_ORDEN_PAYMENT_APPROVED_QUEUE_URL=
GOTICKET_ORDEN_PAYMENT_FAILED_QUEUE_URL=
GOTICKET_INVENTARIO_ORDER_CREATED_QUEUE_URL=
GOTICKET_INVENTARIO_PAYMENT_APPROVED_QUEUE_URL=
GOTICKET_INVENTARIO_PAYMENT_FAILED_QUEUE_URL=
GOTICKET_PAGOS_ORDER_CREATED_QUEUE_URL=
GOTICKET_PAGOS_PAYMENT_FAILED_QUEUE_URL=
GOTICKET_AWS_POLL_MS=1000
```

`host.docker.internal` es necesario porque los microservicios estarán dentro de Docker y MySQL se publicará en el equipo local.

## 4. Levantar los microservicios

Desde la raíz del repositorio:

```powershell
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d --build
```

Este comando inicia:

- Redis.
- Catálogo.
- Inventario.
- Orden.
- Pagos.

Revisa el estado:

```powershell
docker compose --env-file .env.backend -f docker-compose.backend.yml ps
```

Consulta los logs:

```powershell
docker compose --env-file .env.backend -f docker-compose.backend.yml logs -f catalogo inventario orden pagos
```

Los servicios pueden tardar unos segundos en iniciar mientras se construyen las imágenes y se crean las tablas.

## 5. Levantar el API Manager

### Opción A: ejecutar con Maven

Desde `APIManager`:

```powershell
cd APIManager
.\mvnw.cmd spring-boot:run
```

El API Manager utilizará por defecto:

```text
http://localhost:8081  Catálogo
http://localhost:8082  Inventario
http://localhost:8083  Orden
http://localhost:8084  Pagos
```

El gateway estará disponible en:

```text
http://localhost:8080
```

Regresa a la raíz cuando termines:

```powershell
cd ..
```

### Opción B: ejecutar API Manager con Docker

Si los microservicios están en Docker y publican los puertos en el equipo, puedes iniciar el API Manager así:

```powershell
docker build -t goticket-apimanager-local .\APIManager
docker run --rm --name goticket-apimanager-local -p 8080:8080 `
  -e URI_CATALOGO=http://host.docker.internal:8081 `
  -e URI_INVENTARIO=http://host.docker.internal:8082 `
  -e URI_ORDEN=http://host.docker.internal:8083 `
  -e URI_PAGOS=http://host.docker.internal:8084 `
  -e CORS_ALLOWED_ORIGINS=http://localhost:5173 `
  -e FRONTEND_OAUTH_SUCCESS_URL=http://localhost:5173/#oauth2 `
  goticket-apimanager-local
```

## 6. Levantar el frontend

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

## Google OAuth2 local

El inicio de sesión con Google es opcional para ejecutar el resto de la aplicación. Para probarlo, registra en Google Cloud Console:

```text
http://localhost:8080/login/oauth2/code/google
```

Antes de iniciar el API Manager, configura las credenciales en PowerShell:

```powershell
$env:OAUTH2_CLIENT_ID="tu-client-id"
$env:OAUTH2_CLIENT_SECRET="tu-client-secret"
.\mvnw.cmd spring-boot:run
```

El login comienza en:

```text
http://localhost:8080/oauth2/authorization/google
```

No guardes el `client secret` en Git ni en archivos versionados.

## SNS/SQS en desarrollo local

El proyecto está preparado para publicar y consumir eventos mediante SNS/SQS. Para que funcionen los flujos completos de orden, inventario y pagos en local se necesitan:

1. Credenciales AWS disponibles para la aplicación.
2. El topic SNS.
3. Las colas SQS y sus URLs.
4. Las variables correspondientes en `.env.backend`.

Si las variables de SNS/SQS se dejan vacías, los servicios pueden iniciar para probar endpoints y persistencia, pero los flujos asíncronos entre órdenes, pagos e inventario no estarán disponibles.

## Detener el proyecto

Detener los microservicios:

```powershell
docker compose --env-file .env.backend -f docker-compose.backend.yml down
```

Detenerlos y eliminar los datos persistidos de Redis:

```powershell
docker compose --env-file .env.backend -f docker-compose.backend.yml down -v
```

Detener MySQL:

```powershell
docker stop goticket-mysql
```

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

Desde un microservicio Docker, `MYSQL_HOST` debe ser `host.docker.internal`, no `localhost`.

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

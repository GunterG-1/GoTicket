# API Manager GoTicket (configuración local)

API Gateway de GoTicket para ejecutar en el equipo local. Recibe las solicitudes del frontend y las enruta hacia los microservicios que están disponibles en `localhost`.

## Responsabilidades

- Enrutar solicitudes mediante `/api/proxy/**`.
- Aplicar API Keys y autenticación.
- Integrar el inicio de sesión con Google OAuth2.
- Emitir tokens JWT para la sesión del frontend.
- Exponer documentación OpenAPI/Swagger.

## Requisitos

- Java 17 o superior.
- Maven 3.9 o superior, o Maven Wrapper.
- Los microservicios ejecutándose localmente.
- Credenciales OAuth2 de Google únicamente si se probará el inicio de sesión.

## Puertos locales

| Servicio | URL local |
|---|---|
| API Manager | `http://localhost:8080` |
| Catálogo | `http://localhost:8081` |
| Inventario | `http://localhost:8082` |
| Orden | `http://localhost:8083` |
| Pagos | `http://localhost:8084` |
| Frontend | `http://localhost:5173` |

## Configuración local

El archivo `src/main/resources/application.properties` ya contiene valores predeterminados para trabajar con `localhost`:

```properties
URI_CATALOGO=http://localhost:8081
URI_INVENTARIO=http://localhost:8082
URI_ORDEN=http://localhost:8083
URI_PAGOS=http://localhost:8084
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
FRONTEND_OAUTH_SUCCESS_URL=http://localhost:5173/#oauth2
```

También se pueden sobrescribir mediante variables de entorno:

```env
URI_CATALOGO=http://localhost:8081
URI_INVENTARIO=http://localhost:8082
URI_ORDEN=http://localhost:8083
URI_PAGOS=http://localhost:8084
CORS_ALLOWED_ORIGINS=http://localhost:5173
JWT_SECRET=clave-local-para-desarrollo
JWT_EXPIRATION_MS=3600000
OAUTH2_CLIENT_ID=
OAUTH2_CLIENT_SECRET=
FRONTEND_OAUTH_SUCCESS_URL=http://localhost:5173/#oauth2
```

No subas a GitHub los valores reales de `OAUTH2_CLIENT_SECRET` ni otros secretos.

## Ejecución local

Desde la carpeta `APIManager`:

```bash
./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Si Maven está instalado globalmente:

```bash
mvn spring-boot:run
```

El API Manager quedará disponible en:

```text
http://localhost:8080
```

## Ejecución con Docker

Desde la carpeta `APIManager`:

```bash
docker build -t goticket-apimanager-local .
docker run --rm --name goticket-apimanager-local -p 8080:8080 `
  -e URI_CATALOGO=http://host.docker.internal:8081 `
  -e URI_INVENTARIO=http://host.docker.internal:8082 `
  -e URI_ORDEN=http://host.docker.internal:8083 `
  -e URI_PAGOS=http://host.docker.internal:8084 `
  -e CORS_ALLOWED_ORIGINS=http://localhost:5173 `
  -e FRONTEND_OAUTH_SUCCESS_URL=http://localhost:5173/#oauth2 `
  goticket-apimanager-local
```

`host.docker.internal` permite que el contenedor acceda a los microservicios que se están ejecutando directamente en el equipo local.

## Google OAuth2 en localhost

En Google Cloud Console registra este URI de redirección:

```text
http://localhost:8080/login/oauth2/code/google
```

Configura las credenciales antes de iniciar la aplicación:

```powershell
$env:OAUTH2_CLIENT_ID="tu-client-id"
$env:OAUTH2_CLIENT_SECRET="tu-client-secret"
.\mvnw.cmd spring-boot:run
```

El flujo de login comienza en:

```text
http://localhost:8080/oauth2/authorization/google
```

Después de autenticarse, el usuario será redirigido a:

```text
http://localhost:5173/#oauth2
```

## Accesos útiles

```text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/swagger-ui.html
GET http://localhost:8080/v3/api-docs
GET http://localhost:8080/h2-console
```

La base de datos H2 se ejecuta en memoria y se pierde al detener la aplicación.

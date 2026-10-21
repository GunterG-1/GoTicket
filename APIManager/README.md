# API Manager GoTicket

API Gateway de GoTicket. Es el punto de entrada HTTP para el frontend y enruta las solicitudes hacia los microservicios privados.

## Responsabilidades

- Enrutar solicitudes mediante `/api/proxy/**`.
- Aplicar API Keys y autenticación.
- Integrar inicio de sesión con Google OAuth.
- Emitir tokens JWT para la sesión del frontend.
- Registrar tráfico y métricas.
- Exponer documentación OpenAPI/Swagger.

## Tecnologías

- Java 17
- Spring Boot 3.2.5
- Spring Security OAuth2 Client
- Spring Data JPA
- H2 en memoria
- OpenAPI/Swagger
- Docker

## Puerto

```text
8080
```

En producción el puerto se mantiene interno dentro de la red Docker pública. Nginx publica el puerto `80` y reenvía las rutas del frontend hacia API Manager.

## Variables de entorno

```env
BACKEND_HOST=10.0.133.244
URI_CATALOGO=http://10.0.133.244:8081
URI_INVENTARIO=http://10.0.133.244:8082
URI_ORDEN=http://10.0.133.244:8083
URI_PAGOS=http://10.0.133.244:8084
OAUTH2_CLIENT_ID=cliente-de-google
OAUTH2_CLIENT_SECRET=secreto-de-google
JWT_SECRET=secreto-largo-y-aleatorio
FRONTEND_OAUTH_SUCCESS_URL=https://app.example.com/#oauth2
CORS_ALLOWED_ORIGINS=https://app.example.com
```

Configura estas variables en `.env.public`, que no debe subirse a Git.

## Ejecución local

```bash
mvn spring-boot:run
```

También puedes construir la imagen:

```bash
docker build -t goticket-apimanager .
docker run --rm -p 8080:8080 --env-file .env.public goticket-apimanager
```

## Docker Compose

Desde la raíz del repositorio:

```bash
docker compose --env-file .env.public -f docker-compose.public.yml up -d --build apimanager
```

## Accesos

```text
GET /actuator/health
GET /swagger-ui.html
GET /v3/api-docs
```

El callback OAuth2 de producción debe tener este formato:

```text
https://app.example.com/login/oauth2/code/google
```

Debe registrarse exactamente igual en Google Cloud Console.

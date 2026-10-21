# Microservicio Inventario

Servicio responsable de controlar la disponibilidad, reservas, entradas y reventa de entradas.

## Responsabilidades

- Administrar inventario por evento.
- Reservar y confirmar entradas.
- Procesar eventos de órdenes y pagos.
- Coordinarse con Catálogo mediante HTTP.
- Usar Redis para estados temporales de reserva.
- Publicar y consumir eventos mediante SNS/SQS.

## Tecnologías

- Java 17
- Spring Boot 4
- Spring Data JPA
- MySQL/MariaDB
- Redis
- Amazon SNS/SQS
- Docker

## Puerto

```text
8082
```

## Dependencias de ejecución

- Catálogo: `http://catalogo:8081` en Docker Compose.
- Redis: servicio `redis`, puerto `6379`.
- MySQL/MariaDB: base `goticket_inventario`.
- Amazon SNS/SQS en `us-east-1`.

Variables principales:

```env
MYSQL_HOST=10.0.146.0
MYSQL_PORT=3306
MYSQL_USER=goticket_app
MYSQL_PASSWORD=la-contraseña-privada
REDIS_HOST=redis
REDIS_PORT=6379
AWS_REGION=us-east-1
GOTICKET_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:232790552366:goticket-events
```

Las URLs de SQS se encuentran en [.env.backend.example](../.env.backend.example).

## Ejecución local

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Docker

Desde la raíz:

```bash
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d --build inventario
```

## Health check

```text
GET /actuator/health
```


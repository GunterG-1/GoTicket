# Microservicio Inventario

Servicio responsable de controlar la disponibilidad, reservas, entradas y reventa de entradas.

## Responsabilidades

- Administrar inventario por evento.
- Reservar y confirmar entradas.
- Consultar disponibilidad total y distribución por sector.
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

## Disponibilidad y sectores

La capacidad definida para el evento se distribuye entre los sectores configurados para partidos y eventos del Santander Arena. El resto de los eventos usa el sector `General`. La distribución es proporcional a los cupos base de cada sector; los cupos calculados suman exactamente la capacidad total del evento.

```text
GET /api/inventarios/evento/{idEvento}/sectores
```

La reserva valida el sector en el backend. Al confirmar la orden se descuenta la disponibilidad total una sola vez; un pago fallido libera la reserva y devuelve la cantidad.

```text
POST /api/inventarios/entradas/{idEntrada}/reventa/reservar
```

Este endpoint asocia una orden pendiente con la publicación para evitar que dos compradores reclamen la misma entrada. La transferencia definitiva se realiza al procesar el pago aprobado.

## Reventa

Una entrada vendida puede publicarse para reventa. La compra crea una orden y un pago pendiente; la titularidad cambia al comprador solo cuando se procesa el evento de pago aprobado. Si el pago falla, la entrada vuelve a estar disponible en el marketplace.

# Microservicio Orden

Servicio responsable de crear y consultar órdenes de reserva.

## Responsabilidades

- Crear y consultar órdenes.
- Mantener el estado de cada orden.
- Publicar `orden.creada` mediante Amazon SNS.
- Consumir los estados de pago desde Amazon SQS.
- Procesar eventos pendientes mediante el outbox.

## Tecnologías

- Java 17
- Spring Boot 4
- Spring Data JPA
- MySQL/MariaDB
- Amazon SNS/SQS
- Docker

## Puerto

```text
8083
```

## Base de datos

Utiliza la base `goticket_orden`. Hibernate crea o actualiza las tablas al iniciar.

Variables principales:

```env
MYSQL_HOST=10.0.146.0
MYSQL_PORT=3306
MYSQL_USER=goticket_app
MYSQL_PASSWORD=la-contraseña-privada
AWS_REGION=us-east-1
GOTICKET_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:232790552366:goticket-events
```

También requiere las URLs SQS de aprobación y fallo de pago definidas en [.env.backend.example](../.env.backend.example).

## Ejecución local

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Docker

```bash
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d --build orden
```

## Health check

```text
GET /actuator/health
```


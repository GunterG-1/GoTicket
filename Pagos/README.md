# Microservicio Pagos

Servicio responsable de registrar y procesar los pagos asociados a las órdenes.

## Responsabilidades

- Crear y consultar pagos.
- Procesar pagos y actualizar su estado.
- Publicar `pago.aprobado` o `pago.fallido` mediante SNS.
- Consumir las órdenes creadas desde SQS.
- Gestionar la expiración de pagos pendientes.

## Tecnologías

- Java 17
- Spring Boot 4
- Spring Data JPA
- MySQL/MariaDB
- Amazon SNS/SQS
- Docker

## Puerto

```text
8084
```

## Base de datos

Utiliza la base `goticket_pagos`.

Variables principales:

```env
MYSQL_HOST=10.0.146.0
MYSQL_PORT=3306
MYSQL_USER=goticket_app
MYSQL_PASSWORD=la-contraseña-privada
AWS_REGION=us-east-1
GOTICKET_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:232790552366:goticket-events
```

Las URLs SQS de órdenes creadas y pagos fallidos se configuran en [.env.backend.example](../.env.backend.example).

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
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d --build pagos
```

## Health check

```text
GET /actuator/health
```


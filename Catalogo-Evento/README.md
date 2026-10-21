# Microservicio Catálogo de Eventos

Servicio Spring Boot responsable de administrar los eventos disponibles en GoTicket.

## Responsabilidades

- Crear, consultar, actualizar y eliminar eventos.
- Exponer la API HTTP del catálogo.
- Persistir los eventos en MySQL.

## Tecnologías

- Java 17
- Spring Boot 4
- Spring Data JPA
- MySQL/MariaDB
- Docker

## Puerto

```text
8081
```

## Base de datos

La aplicación utiliza la base `goticket_catalogo`. Las tablas se crean o actualizan automáticamente con Hibernate:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Variables de conexión:

```env
MYSQL_HOST=10.0.146.0
MYSQL_PORT=3306
MYSQL_USER=goticket_app
MYSQL_PASSWORD=la-contraseña-privada
```

## Ejecución local

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Docker

Desde la raíz del repositorio:

```bash
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d --build catalogo
```

El servicio queda disponible en:

```text
http://localhost:8081
```

## Health check

```text
GET /actuator/health
```


# Spring Boot API Manager & Gateway 🚀

Un ejemplo completo y profesional de **API Manager / API Gateway** desarrollado con **Spring Boot 3**, **Spring Data JPA**, **Base de Datos H2**, **OpenAPI / Swagger** y un **Dashboard Web Interactivo**.

---

## 🌟 Características Principales

1. **Gestión de Rutas Proxy (API Management)**:
   - Registro, actualización, activación/desactivación y eliminación dinámica de APIs objetivo.
   - Enrutamiento transparente desde `/api/proxy/{patron}/**` hacia los servidores o microservicios destino.

2. **Seguridad y Autenticación con API Keys**:
   - Generación y revención de llaves de acceso (`X-API-KEY`).
   - Control de acceso por ruta (Rutas públicas vs Rutas protegidas).

3. **Limitación de Frecuencia (Rate Limiting)**:
   - Algoritmo de control de cuotas por minuto por cliente/IP para prevenir sobrecarga de servidores.

4. **Auditoría y Registro de Tráfico (Logging & Analytics)**:
   - Medición de latencia en milisegundos, códigos de estado HTTP, IPs de origen y almacenamiento en BD.

5. **Servicios Backend Simulados (Internal Mocks)**:
   - Incluye endpoints mock internos (`/api/mock/users`, `/api/mock/products`, `/api/mock/status`) para probar el Gateway de inmediato sin depender de servicios de terceros.

6. **Dashboard Web Interactivo (UI)**:
   - Interfaz web SPA integrada en `http://localhost:8080/` con diseño moderno, métricas en vivo y **Sandbox de pruebas** (Tester intercativo tipo Postman).

---

## 📂 Estructura del Proyecto

```
APIManager/
├── pom.xml                                   # Configuración de dependencias Maven
├── README.md                                 # Documentación del proyecto
└── src/
    ├── main/
    │   ├── java/com/duoc/apimanager/
    │   │   ├── ApiManagerApplication.java    # Clase principal Spring Boot
    │   │   ├── config/
    │   │   │   └── DataInitializer.java     # Precarga de datos iniciales
    │   │   ├── controller/
    │   │   │   ├── ApiKeyController.java    # REST API para llaves (/api/v1/keys)
    │   │   │   ├── ApiLogController.java     # REST API para auditoría (/api/v1/logs)
    │   │   │   ├── ApiRouteController.java   # REST API para rutas (/api/v1/routes)
    │   │   │   ├── GatewayController.java    # Gateway Proxy (/api/proxy/**)
    │   │   │   └── MockBackendController.java# Backends de simulación (/api/mock/**)
    │   │   ├── model/
    │   │   │   ├── ApiKey.java
    │   │   │   ├── ApiLog.java
    │   │   │   └── ApiRoute.java
    │   │   ├── repository/
    │   │   │   ├── ApiKeyRepository.java
    │   │   │   ├── ApiLogRepository.java
    │   │   │   └── ApiRouteRepository.java
    │   │   └── service/
    │   │       ├── ProxyService.java         # Enrutamiento dinámico HTTP
    │   │       └── RateLimiterService.java   # Control de frecuencia por minuto
    │   └── resources/
    │       ├── application.properties        # Configuración de puerto y BD H2
    │       └── static/
    │           └── index.html                # Single Page Application Dashboard
```

---

## 🚀 Cómo Ejecutar el Proyecto

### Prerrequisitos
- Java 17 o superior instalado.
- Maven (o la instalación de Maven en `"C:\Program Files (x86)\apache-maven-3.9.16\bin"`).

### Comandos de Ejecución

Con Maven instalado:
```bash
mvn spring-boot:run
```

O compilando el archivo `.jar`:
```bash
mvn clean package
java -jar target/apimanager-1.0.0.jar
```

---

## 🌐 Accesos Web

Una vez iniciada la aplicación, accede desde el navegador a:

- 📊 **Dashboard Interactivo**: [http://localhost:8080/](http://localhost:8080/)
- 📑 **Documentación Swagger / OpenAPI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 🗄️ **Consola de Base de Datos H2**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - *JDBC URL*: `jdbc:h2:mem:apimanagerdb`
  - *Usuario*: `sa`
  - *Password*: (vacío)

---

## 🧪 Ejemplos de Pruebas de Enrutamiento (API Gateway)

### 1. Consultar usuarios a través del Gateway (API Pública)
```http
GET http://localhost:8080/api/proxy/usuarios
```
**Respuesta:** Retorna los usuarios entregados por el backend mock enrutado.

### 2. Consultar productos (API Protegida con API Key)
Sin Header `X-API-KEY`:
```http
GET http://localhost:8080/api/proxy/productos
```
**Respuesta (`401 Unauthorized`):**
```json
{
  "status": 401,
  "error": "Esta API requiere una clave de acceso (Header 'X-API-KEY')"
}
```

Con Header `X-API-KEY`:
```http
GET http://localhost:8080/api/proxy/productos
Header: X-API-KEY: duoc-demo-key-2026
```
**Respuesta (`200 OK`):** Retorna la lista de productos.

---

## 🛠️ Tecnologías Utilizadas

- **Spring Boot 3.2.5**
- **Spring Data JPA**
- **H2 In-Memory Database**
- **Springdoc OpenAPI (Swagger UI)**
- **HTML5 / Vanilla CSS (Glassmorphic Dark UI) / JS ES6**

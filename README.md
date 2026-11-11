# CyberGuard Enterprise

Plataforma de monitoreo de seguridad informática para empresas. Java 21 + Spring Boot 3 en el servidor; JavaScript, Bootstrap 5 y Chart.js en la interfaz; PostgreSQL como base de datos.

## Arranque rápido
Necesita Java 21. Maven no hace falta (el lanzador descarga una copia local) y PostgreSQL es opcional para probar (sin él se usa una base de datos incorporada).

- Windows: doble clic en `ejecutar.bat`
- Linux/macOS: `sh ejecutar.sh`
- Cualquier sistema: `java Ejecutar.java` (o `java Ejecutar.java jar` para generar y ejecutar el `.jar`)

Primer acceso: sesión `admin`, contraseña `CambiaEsta#2026` (la consola la muestra al arrancar). También puede pulsar "Crear cuenta" para registrar su propia empresa.

Comandos manuales: `mvn test`, `mvn spring-boot:run`, `mvn clean package` y `java -jar target/cyberguard-enterprise.jar`.

## Módulos
Resumen con gráficas · Dispositivos (inventario y agentes) · Alertas · Vulnerabilidades · Incidentes · Informes (resumen, CSV e impresión) · Mi perfil (datos, contraseña, incidentes asignados y actividad) · Sesiones y permisos (con restablecer clave) · Invitaciones por enlace y código QR · Auditoría · Empresa y licencia.

## Documentación
- `docs/MANUAL_INSTALACION.md`: instalación, configuración, producción y prueba de funcionamiento.
- `docs/MANUAL_USUARIO.md`: uso del panel por rol y del agente.
- `docs/API.md`: referencia de la API REST.

## Estructura
```
Ejecutar.java, ejecutar.bat, ejecutar.sh   lanzador
agente/CyberGuardAgent.java                agente para los equipos a monitorear
src/main/java/com/cyberguard/              backend (config, security, controller, service, repository, entity, dto)
src/main/resources/static/                 interfaz web (HTML, CSS, JS)
src/main/resources/db/migration*/          esquema de la base de datos (PostgreSQL y modo local)
```

## Seguridad
BCrypt (coste 12) · JWT con expiración de 60 min · roles ADMIN, ANALISTA y LECTOR aplicados en el servidor · validación de entradas · datos separados por empresa en todas las consultas · claves de agente e invitaciones con token aleatorio de 256 bits (solo se guarda su huella SHA-256; las invitaciones vencen, tienen límite de usos, se pueden revocar y nunca dan rol de administrador) · bloqueo de 15 minutos tras 5 inicios de sesión fallidos · cambio de contraseña que cierra las demás sesiones · límite de intentos de registro y de eventos de agentes · auditoría de operaciones · CSV protegido contra inyección de fórmulas.

## Pendiente para producción
Servir la aplicación con HTTPS (proxy inverso), cambiar las contraseñas y el secreto JWT iniciales, y servir Bootstrap y Chart.js desde el propio servidor si la red de la empresa bloquea los CDN.

## Modo invitado (entrar solo para ver)

La pantalla de inicio de sesión incluye el botón **Entrar solo para ver**. Entra con la cuenta `invitado` (rol LECTOR) de la primera empresa, sin escribir usuario ni contraseña. El servidor rechaza cualquier escritura de ese rol y la cuenta de invitado no puede cambiar su perfil ni su contraseña.

- Desactivarlo: variable de entorno `CYBERGUARD_GUEST_ENABLED=false` (el botón deja de mostrarse).
- Cambiar el nombre de la cuenta: `CYBERGUARD_GUEST_USER`.
- Los invitados ven los datos de la primera empresa: úsese en demostraciones o con datos que se puedan mostrar.
- Foto de la pantalla de inicio: guarde su imagen como `src/main/resources/static/img/login.jpg` (si no existe, se usa un fondo por defecto).

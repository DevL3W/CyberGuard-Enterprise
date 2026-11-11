# Manual de instalación

## 1. Requisitos
| Componente | Necesario | Notas |
|---|---|---|
| Java 21 | Sí | Compruebe con `java -version` |
| Maven 3.9+ | No | Si falta, `Ejecutar.java` descarga una copia en la carpeta `.maven` |
| PostgreSQL 14+ | Para producción | Para probar, el lanzador usa una base de datos local (carpeta `data`) |
| Internet | Primera ejecución | Descarga de dependencias; la interfaz carga Bootstrap y Chart.js de un CDN |

## 2. Ejecución con el lanzador
1. Descomprima el proyecto y abra una terminal en la carpeta `cyberguard-enterprise`.
2. Ejecute `ejecutar.bat` (Windows), `sh ejecutar.sh` (Linux/macOS) o `java Ejecutar.java`.
3. El lanzador comprueba Java, Maven, el puerto 8080 y PostgreSQL, y abre el navegador cuando el servidor está listo.
4. Si no encuentra PostgreSQL arranca en **modo local**. Use `java Ejecutar.java local` para forzarlo.

## 3. PostgreSQL (producción)
```sql
CREATE USER cyberguard WITH PASSWORD 'una-clave-segura';
CREATE DATABASE cyberguard OWNER cyberguard;
```
Las tablas se crean solas al arrancar (Flyway). Configure la conexión con variables de entorno.

## 4. Variables de entorno
| Variable | Descripción | Por defecto |
|---|---|---|
| CYBERGUARD_DB_URL | URL JDBC | jdbc:postgresql://localhost:5432/cyberguard |
| CYBERGUARD_DB_USER / _PASSWORD | Credenciales de la base de datos | cyberguard / cyberguard |
| CYBERGUARD_JWT_SECRET | Clave de firma de sesiones, mínimo 32 caracteres | valor de ejemplo (**cámbielo**) |
| CYBERGUARD_ADMIN_USER / _PASSWORD | Administrador inicial | admin / CambiaEsta#2026 |
| CYBERGUARD_COMPANY_NAME | Nombre de la primera empresa | Empresa Demo |
| CYBERGUARD_REGISTRATION_ENABLED | `false` desactiva el registro público | true |

Ajustes en `application.properties`: `cyberguard.security.max-login-failures` (5 intentos fallidos antes de bloquear 15 minutos).

Ajustes de monitoreo en `application.properties`: `cyberguard.monitoring.offline-after-seconds` (180), `check-interval-ms` (30000) y `max-events-per-minute` (120).

## 5. Generar el ejecutable
```
mvn clean package
java -jar target/cyberguard-enterprise.jar
```
Con la base de datos local: `java -jar target/cyberguard-enterprise.jar --spring.profiles.active=local`.

## 6. Puesta en producción
- Publique la aplicación detrás de un proxy inverso con **HTTPS** (Nginx, Apache, Caddy…). Sin HTTPS, contraseñas y claves de agente viajan sin cifrar.
- Cambie la contraseña del administrador, el secreto JWT y las credenciales de la base de datos.
- Si no quiere que cualquiera cree empresas, ponga `CYBERGUARD_REGISTRATION_ENABLED=false`.
- Haga copias de seguridad periódicas de PostgreSQL.

## 7. Prueba de funcionamiento (15 minutos)
1. Ejecute `mvn test`: deben pasar todas las pruebas.
2. Arranque la aplicación y entre con `admin`. Compruebe la pantalla de carga y el panel.
3. En **Sesiones**, cree una sesión `lector1` con rol "Solo consulta" y otra `analista1` con rol "Analista".
4. En **Dispositivos**, registre un equipo. En **Alertas**, registre una alerta y resuélvala. Compruebe el **Resumen**.
5. Cierre sesión y entre con `lector1`: debe ver todo, sin botones de crear, editar ni eliminar, y sin Sesiones, Auditoría ni Empresa.
6. Como administrador, en **Dispositivos** pulse **Agente** y copie el comando. En el equipo, desde la carpeta `agente`, ejecútelo. En unos segundos el dispositivo pasa a "En línea" con "Última conexión".
7. Detenga el agente (Ctrl+C) y espere unos 3 minutos: el dispositivo pasa a "Sin conexión" y aparece la alerta "Dispositivo sin conexión". Al reiniciar el agente, la alerta se cierra sola.
8. En **Informes**, descargue un CSV y revise **Auditoría**: debe aparecer cada paso anterior.
9. Cree una cuenta nueva desde "Crear cuenta" y compruebe que no ve los datos de la otra empresa.
10. En **Mi perfil**, cambie su nombre y su contraseña. Con la misma cuenta abierta en otra ventana, compruebe que esa otra ventana pide iniciar sesión de nuevo.
11. En el login, falle 5 veces la contraseña de una sesión: debe aparecer el aviso de bloqueo de 15 minutos.

## 8. Problemas frecuentes
| Síntoma | Solución |
|---|---|
| "release version 21 not supported" | Maven usa un Java anterior: revise `JAVA_HOME` o use `Ejecutar.java` |
| Puerto 8080 ocupado | Cierre el programa que lo usa, o agregue `--server.port=8081` |
| No conecta a PostgreSQL | Revise que el servicio esté activo y las variables `CYBERGUARD_DB_*` |
| La pantalla se ve sin estilos o sin gráficas | El navegador no pudo cargar el CDN: revise internet |
| El agente dice "clave no válida" | Genere una clave nueva en Dispositivos > Agente |

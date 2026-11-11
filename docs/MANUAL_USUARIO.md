# Manual de usuario

## Inicio de sesión y registro
- **Iniciar sesión:** escriba el nombre de su sesión y su contraseña. La sesión caduca a los 60 minutos.
- **Crear cuenta:** registra una empresa nueva y su primer administrador (si el registro público está activado). Cada empresa solo ve sus propios datos.

## Roles
| | Administrador | Analista | Solo consulta |
|---|---|---|---|
| Ver todos los módulos de seguridad | Sí | Sí | Sí |
| Registrar y editar dispositivos, alertas, vulnerabilidades e incidentes | Sí | Sí | No |
| Eliminar dispositivos | Sí | No | No |
| Generar claves de agente | Sí | Sí | No |
| Sesiones, Invitaciones, Auditoría, Empresa y licencia | Sí | No | No |

## Mi perfil
Se abre pulsando su nombre en la esquina inferior del menú o en **Mi cuenta > Mi perfil**. Todos los roles lo tienen.
- **Mis datos:** cambie su nombre completo, correo electrónico y cargo.
- **Cambiar contraseña:** escriba la actual y la nueva (mínimo 10 caracteres, distinta de la actual). Al cambiarla, las demás sesiones abiertas con su cuenta se cierran y usted continúa conectado.
- **Incidentes asignados a mí:** los incidentes abiertos de los que es responsable.
- **Mi actividad reciente:** sus últimos 15 movimientos.

Tras 5 intentos fallidos al iniciar sesión, el acceso se bloquea 15 minutos para esa conexión y esa sesión.

## Resumen
Tarjetas con dispositivos, alertas, vulnerabilidades e incidentes abiertos; gráficas de estado de dispositivos, alertas por severidad y alertas de los últimos 7 días; y las alertas más recientes.

## Dispositivos
Inventario con búsqueda y filtro por estado. **Registrar dispositivo** abre el formulario; **Editar** modifica uno existente; **Eliminar** (solo administrador) lo borra y conserva sus alertas sin dispositivo asociado. La etiqueta **Agente** indica que tiene un agente configurado.

### Instalar el agente en un equipo
1. En la fila del dispositivo pulse **Agente** y confirme. Copie la clave y el comando: **la clave solo se muestra una vez**.
2. Copie la carpeta `agente` al equipo (necesita Java 21) y ejecute el comando desde esa carpeta.
3. El agente envía cada 30 s el nombre del equipo, su IP y su sistema operativo, y avisa si queda poco espacio en disco (menos del 10 %). No lee archivos ni ejecuta comandos.
4. Si el agente deja de enviar señal durante 3 minutos, el dispositivo pasa a "Sin conexión" y se crea una alerta de severidad Alta. Al volver, la alerta se cierra sola.
5. Los dispositivos en "Mantenimiento" no cambian de estado por el agente. Si pierde la clave, genere otra: la anterior deja de funcionar.

## Alertas
Filtre por texto, severidad o estado. Estados: **Abierta → Reconocida → Resuelta**. Con **Reabrir** vuelve a Abierta. Los agentes y el sistema también crean alertas.

## Vulnerabilidades
Registre debilidades conocidas (con CVE opcional) y avance su estado: Abierta → En proceso → Corregida. CyberGuard no escanea la red: las vulnerabilidades se registran manualmente.

## Incidentes
Registre incidentes, asigne un responsable y cambie su estado desde la propia tabla: Abierto → En investigación → Contenido → Cerrado. Al cerrar se guarda la fecha.

## Informes
Resumen por tipo, estado y severidad, con el tiempo medio de resolución de alertas. Los botones descargan CSV (dispositivos, alertas, vulnerabilidades, incidentes) para abrir en Excel. **Imprimir resumen** genera una versión para papel o PDF.

## Sesiones (administrador)
Cree sesiones con rol Administrador, Analista o Solo consulta, cambie roles, active o desactive sesiones y use **Restablecer clave** para fijar una contraseña nueva a otra persona (sus sesiones abiertas se cierran; entréguesela por un canal seguro). La sesión principal `admin` no puede desactivarse ni cambiar de rol, y nadie puede desactivarse a sí mismo.

## Invitaciones y compartir por enlace o QR (administrador)
Abra **Administración > Invitaciones**.
- **Dirección del servidor:** escriba la dirección con la que los demás abren CyberGuard (por ejemplo `http://192.168.1.20:8080` o `https://seguridad.miempresa.com`). Si dice `localhost`, el enlace solo abrirá en el equipo del servidor. Se recuerda en su navegador.
- **Compartir el acceso:** el panel de la izquierda muestra el enlace de inicio de sesión y su código QR. Puede copiarlo, descargar el QR como imagen o, en teléfonos y equipos que lo permitan, usar **Compartir…**.
- **Invitar a alguien a la empresa:** pulse **Nueva invitación**, elija el rol (Solo consulta o Analista), cuántos días vale (1, 7 o 30) y cuántas personas pueden usarla. Obtendrá un enlace y un QR; quien los abra escribe su nombre, sesión y contraseña y entra directamente con ese rol.
- **Seguridad:** el enlace se muestra una sola vez (el servidor solo guarda una huella). Si lo pierde, revoque la invitación y cree otra. **Revocar** deja el enlace y el QR sin efecto de inmediato; las sesiones ya creadas se conservan. Una invitación nunca puede dar el rol de administrador: para eso use **Sesiones**. Las altas por invitación quedan en Auditoría.

## Auditoría (administrador)
Historial de los últimos 200 movimientos: inicios de sesión, altas, cambios, eliminaciones, claves de agente y desconexiones.

## Empresa y licencia (administrador)
Cambie el nombre de la empresa y consulte la clave de licencia, su estado y los totales de sesiones y dispositivos. Con la licencia suspendida, sus sesiones y agentes dejan de funcionar.

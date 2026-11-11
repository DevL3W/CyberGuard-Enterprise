# API REST

Todas las rutas devuelven JSON (salvo las exportaciones CSV). Los errores tienen la forma `{"error": "mensaje"}`.
Las sesiones de usuario envían `Authorization: Bearer <token>`; los agentes envían `X-Device-Id` y `X-Agent-Key`.
Roles: **A** = ADMIN, **N** = ANALISTA o superior, **T** = cualquier sesión de usuario (incluye LECTOR).

## Autenticación
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | /api/auth/login | público | `{username, password}` → `{token, expiresInMinutes, user}` |
| POST | /api/auth/register | público | `{companyName, fullName, username, password}` → crea empresa y administrador; devuelve igual que login (201) |
| POST | /api/auth/guest | público | Modo invitado (solo ver): devuelve igual que login con la cuenta de invitado, rol LECTOR. 403 si está desactivado |
| GET | /api/auth/config | público | `{registrationEnabled, guestEnabled}` |
| GET | /api/auth/me | T | Datos de la sesión actual |

`POST /api/auth/login` bloquea 15 minutos (429) tras 5 intentos fallidos desde la misma conexión con la misma sesión.

## Mi perfil
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | /api/profile | T | Datos propios, incidentes asignados abiertos y últimos 15 movimientos propios |
| PUT | /api/profile | T | `{fullName, email?, jobTitle?}` |
| POST | /api/profile/password | T | `{currentPassword, newPassword (≥10)}` → devuelve un token nuevo; los tokens anteriores dejan de valer |

## Sesiones, empresa y auditoría
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | /api/users | A | Sesiones de la empresa |
| POST | /api/users | A | `{username, password (≥10), fullName, role}` (201) |
| PATCH | /api/users/{id} | A | `{role?, enabled?}` |
| GET | /api/directory | T | Sesiones activas (id, nombre, rol) para asignar responsables |
| PATCH | /api/users/{id}/password | A | `{newPassword (≥10)}`: restablece la contraseña de otra sesión (204). No aplica a la propia ni a la principal |
| GET | /api/invitations | A | Últimas 100 invitaciones: `{id, role, maxUses, uses, expiresAt, createdBy, createdAt, status}` (`ACTIVA`, `REVOCADA`, `VENCIDA`, `AGOTADA`) |
| POST | /api/invitations | A | `{role: ANALISTA\|LECTOR, days: 1-30, maxUses: 1-100}` → `{invitation, token}` (201). El `token` solo se devuelve aquí. No admite `ADMIN` (400) |
| DELETE | /api/invitations/{id} | A | Revoca la invitación (204) |
| GET | /api/join/{token} | público | Vista previa de la invitación: `{companyName, role, expiresAt}`. 404 si no existe, venció, está agotada o revocada |
| POST | /api/join/{token} | público | `{fullName, username, password (≥10)}` → crea la sesión con el rol de la invitación; devuelve igual que login (201). 409 si el nombre ya existe; 429 tras 20 intentos por hora y conexión |
| GET | /api/company | A | Empresa, licencia y totales |
| PUT | /api/company | A | `{name}`: cambia el nombre de la empresa |
| GET | /api/audit | A | Últimos 200 movimientos |

## Dispositivos
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | /api/devices | T | Lista |
| POST | /api/devices | N | `{name, hostname?, ipAddress?, type, operatingSystem?, status}` (201) |
| PUT | /api/devices/{id} | N | Mismo cuerpo |
| DELETE | /api/devices/{id} | A | Elimina (204) |
| POST | /api/devices/{id}/agent-key | N | Genera clave de agente: `{deviceId, key}` (la clave solo se devuelve una vez) |

`type`: SERVIDOR, COMPUTADOR, RED, OTRO · `status`: ONLINE, OFFLINE, MANTENIMIENTO.

## Alertas, vulnerabilidades e incidentes
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | /api/alerts | T | Lista |
| POST | /api/alerts | N | `{title, description?, severity, deviceId?}` (201) |
| PATCH | /api/alerts/{id}/status | N | `{status}`: ABIERTA, RECONOCIDA, RESUELTA |
| GET | /api/vulnerabilities | T | Lista |
| POST | /api/vulnerabilities | N | `{title, description?, cve?, severity, deviceId?}` (201) |
| PATCH | /api/vulnerabilities/{id}/status | N | `{status}`: ABIERTA, EN_PROCESO, CORREGIDA |
| GET | /api/incidents | T | Lista |
| POST | /api/incidents | N | `{title, description?, severity, deviceId?, assigneeId?}` (201) |
| PATCH | /api/incidents/{id} | N | `{status?, assigneeId?}`: ABIERTO, EN_INVESTIGACION, CONTENIDO, CERRADO |

`severity`: CRITICA, ALTA, MEDIA, BAJA. `cve`: formato `CVE-2024-12345`.

## Resumen e informes
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | /api/dashboard | T | Cifras y datos de las gráficas |
| GET | /api/reports/overview | T | Resumen por tipo, estado y severidad |
| GET | /api/reports/export/{name} | T | CSV: `devices`, `alerts`, `vulnerabilities` o `incidents` |

## API de agentes
| Método | Ruta | Descripción |
|---|---|---|
| POST | /api/agent/heartbeat | `{hostname?, ipAddress?, operatingSystem?}`: marca el dispositivo en línea y actualiza sus datos |
| POST | /api/agent/events | `{title, description?, severity}`: crea una alerta del dispositivo (201). Máximo 120 por minuto |

Ejemplo:
```
curl -X POST http://localhost:8080/api/agent/heartbeat \
  -H "Content-Type: application/json" -H "X-Device-Id: 1" -H "X-Agent-Key: CLAVE" \
  -d '{"hostname":"srv01","ipAddress":"10.0.0.5","operatingSystem":"Ubuntu 24.04"}'
```

## Códigos de error
400 datos no válidos · 401 sin sesión, sesión caducada o clave de agente inválida · 403 sin permiso, o registro desactivado · 404 no existe (o pertenece a otra empresa) · 404 también para invitaciones inválidas o vencidas · 409 nombre de sesión repetido · 429 demasiados registros o eventos.

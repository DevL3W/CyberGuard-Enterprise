(() => {
  const ACTION = { CREAR: 'Crear', MODIFICAR: 'Modificar', ELIMINAR: 'Eliminar', INICIO_SESION: 'Inicio de sesión',
    REGISTRO: 'Registro', CLAVE_AGENTE: 'Clave de agente', DESCONEXION: 'Desconexión',
    CAMBIO_CLAVE: 'Cambio de contraseña' };
  let logs = [];

  function render() {
    const q = $('audFilter').value.trim().toLowerCase();
    const act = $('audAction').value;
    const shown = logs.filter((l) => (!act || l.action === act)
      && (!q || [l.username, l.detail, l.entityType].some((x) => x && x.toLowerCase().includes(q))));
    $('audRows').innerHTML = shown.length ? shown.map((l) => `<tr>
      <td class="text-nowrap">${fmtDate(l.createdAt)}</td>
      <td>${escapeHtml(l.username)}</td>
      <td><span class="badge badge-role">${escapeHtml(ACTION[l.action] || l.action)}</span></td>
      <td>${escapeHtml(l.entityType)}${l.entityId ? ' #' + l.entityId : ''}</td>
      <td>${escapeHtml(l.detail || '')}</td></tr>`).join('')
      : '<tr><td colspan="5" class="text-center text-secondary py-4">No hay movimientos que coincidan.</td></tr>';
  }

  App.register({
    id: 'auditoria',
    title: 'Auditoría',
    roles: ['ADMIN'],
    admin: true,
    init() {
      $('audFilter').addEventListener('input', render);
      $('audAction').addEventListener('change', render);
    },
    async show() {
      logs = await Api.get('/api/audit');
      render();
    },
  });
})();

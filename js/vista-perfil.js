(() => {
  const ACTION = { CREAR: 'Creó', MODIFICAR: 'Modificó', ELIMINAR: 'Eliminó', INICIO_SESION: 'Inició sesión',
    REGISTRO: 'Registró', CLAVE_AGENTE: 'Generó clave de agente', DESCONEXION: 'Desconexión',
    CAMBIO_CLAVE: 'Cambió una contraseña' };

  function render(p) {
    $('pfHeader').innerHTML = `<div class="d-flex align-items-center gap-3">
      <div class="avatar avatar-lg" aria-hidden="true">${escapeHtml(initials(p.fullName))}</div>
      <div class="min-w-0">
        <div class="h5 mb-0">${escapeHtml(p.fullName)}</div>
        <div class="text-secondary">${escapeHtml(ROLE_LABEL[p.role] || p.role)} · ${escapeHtml(p.companyName)}</div>
        <div class="small text-secondary">Sesión: ${escapeHtml(p.username)} · Miembro desde ${fmtDate(p.createdAt)}</div>
      </div></div>`;
    $('pfName').value = p.fullName;
    $('pfEmail').value = p.email || '';
    $('pfJob').value = p.jobTitle || '';

    $('pfIncidents').innerHTML = p.assignedIncidents.length
      ? `<ul class="feed">${p.assignedIncidents.map((i) => `<li class="d-flex align-items-center gap-2">
          ${sevBadge(i.severity)}<span class="flex-grow-1">${escapeHtml(i.title)}</span>${inBadge(i.status)}</li>`).join('')}</ul>`
      : '<p class="text-secondary mb-0">No tiene incidentes abiertos asignados.</p>';

    $('pfActivity').innerHTML = p.recentActivity.length
      ? `<ul class="feed">${p.recentActivity.map((a) => `<li>
          <div>${escapeHtml(ACTION[a.action] || a.action)} · ${escapeHtml(a.entityType)}${a.entityId ? ' #' + a.entityId : ''}</div>
          <div class="small text-secondary">${fmtDate(a.createdAt)}${a.detail ? ' · ' + escapeHtml(a.detail) : ''}</div></li>`).join('')}</ul>`
      : '<p class="text-secondary mb-0">Todavía no hay actividad.</p>';
  }

  async function load() { render(await Api.get('/api/profile')); }

  App.register({
    id: 'perfil',
    title: 'Mi perfil',
    account: true,
    init() {
      $('pfForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('pfError');
        box.classList.add('d-none');
        try {
          const p = await Api.put('/api/profile', {
            fullName: $('pfName').value.trim(),
            email: $('pfEmail').value.trim(),
            jobTitle: $('pfJob').value.trim(),
          });
          App.setName(p.fullName);
          render(p);
          App.flash('Datos guardados.');
        } catch (err) { box.textContent = err.message; box.classList.remove('d-none'); }
      });

      $('pwForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('pwError');
        box.classList.add('d-none');
        const next = $('pwNew').value;
        if (next.length < 10) { box.textContent = 'La contraseña nueva debe tener al menos 10 caracteres.'; box.classList.remove('d-none'); return; }
        if (next !== $('pwNew2').value) { box.textContent = 'Las contraseñas nuevas no coinciden.'; box.classList.remove('d-none'); return; }
        try {
          const res = await Api.post('/api/profile/password', { currentPassword: $('pwCurrent').value, newPassword: next });
          Api.setToken(res.token); // el token anterior ya no vale
          $('pwForm').reset();
          App.flash('Contraseña actualizada. Las demás sesiones abiertas se cerraron.');
          await load();
        } catch (err) { box.textContent = err.message; box.classList.remove('d-none'); }
      });
    },
    show: load,
  });
})();

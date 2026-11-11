(() => {
  async function render() {
    const c = await Api.get('/api/company');
    $('empName').value = c.name;
    const row = (k, v) => `<dt class="col-sm-4 mb-2">${k}</dt><dd class="col-sm-8 mb-2">${v}</dd>`;
    $('empresaBox').innerHTML =
      row('Empresa', escapeHtml(c.name)) +
      row('Clave de licencia', `<span class="license-key">${escapeHtml(c.licenseKey)}</span>
        <button class="btn btn-sm btn-outline-secondary ms-2" data-copy="${escapeHtml(c.licenseKey)}">Copiar</button>`) +
      row('Estado de la licencia', `<span class="badge ${c.active ? 'badge-on' : 'badge-off'}">${c.active ? 'Activa' : 'Suspendida'}</span>`) +
      row('Registrada el', fmtDate(c.createdAt)) +
      row('Sesiones', c.users) +
      row('Dispositivos', c.devices);
  }

  App.register({
    id: 'empresa',
    title: 'Empresa y licencia',
    roles: ['ADMIN'],
    admin: true,
    init() {
      $('empresaBox').addEventListener('click', async (e) => {
        if (!e.target.dataset.copy) return;
        try {
          await navigator.clipboard.writeText(e.target.dataset.copy);
          App.flash('Clave de licencia copiada.');
        } catch (_) { App.flash('No se pudo copiar. Selecciónela y cópiela a mano.', 'warning'); }
      });

      $('empForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = $('empName').value.trim();
        if (!name) { App.flash('Escriba el nombre de la empresa.', 'danger'); return; }
        try {
          const c = await Api.put('/api/company', { name });
          $('companyName').textContent = c.name;
          App.flash('Nombre de la empresa actualizado.');
          await render();
        } catch (err) { App.flash(err.message, 'danger'); }
      });
    },
    show: render,
  });
})();

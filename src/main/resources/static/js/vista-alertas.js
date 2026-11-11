(() => {
  let alerts = [];
  let devices = [];
  const modal = () => bootstrap.Modal.getOrCreateInstance($('alertModal'));

  function actions(a) {
    if (!App.canEdit()) return '';
    const btn = (st, text) => `<button class="btn btn-sm btn-outline-secondary" data-status="${st}" data-id="${a.id}">${text}</button>`;
    if (a.status === 'ABIERTA') return btn('RECONOCIDA', 'Reconocer') + ' ' + btn('RESUELTA', 'Resolver');
    if (a.status === 'RECONOCIDA') return btn('RESUELTA', 'Resolver');
    return btn('ABIERTA', 'Reabrir');
  }

  function render() {
    const q = $('alFilter').value.trim().toLowerCase();
    const sev = $('alSevFilter').value;
    const st = $('alStatusFilter').value;
    const shown = alerts.filter((a) => (!sev || a.severity === sev) && (!st || a.status === st)
      && (!q || [a.title, a.description, a.deviceName].some((v) => v && v.toLowerCase().includes(q))));

    $('alRows').innerHTML = shown.length ? shown.map((a) => `<tr>
      <td>${sevBadge(a.severity)}</td>
      <td><div class="fw-semibold">${escapeHtml(a.title)}</div>
        ${a.description ? `<div class="small text-secondary small-desc">${escapeHtml(a.description)}</div>` : ''}</td>
      <td>${escapeHtml(a.deviceName || '—')}</td>
      <td>${alBadge(a.status)}</td>
      <td>${fmtDate(a.createdAt)}${a.resolvedAt ? `<div class="small text-secondary">Resuelta: ${fmtDate(a.resolvedAt)}</div>` : ''}</td>
      <td class="text-end text-nowrap">${actions(a)}</td></tr>`).join('')
      : `<tr><td colspan="6" class="text-center text-secondary py-4">${alerts.length
        ? 'No hay alertas que coincidan con los filtros.'
        : 'No hay alertas registradas.'}</td></tr>`;
  }

  async function load() {
    [alerts, devices] = await Promise.all([Api.get('/api/alerts'), Api.get('/api/devices')]);
    $('aDevice').innerHTML = '<option value="">Sin dispositivo</option>'
      + devices.map((d) => `<option value="${d.id}">${escapeHtml(d.name)}</option>`).join('');
    render();
  }

  App.register({
    id: 'alertas',
    title: 'Alertas',
    init() {
      if (App.canEdit()) $('alNew').classList.remove('d-none');
      $('alNew').addEventListener('click', () => {
        $('alertError').classList.add('d-none');
        $('alertForm').reset();
        modal().show();
      });
      ['alFilter'].forEach((id) => $(id).addEventListener('input', render));
      ['alSevFilter', 'alStatusFilter'].forEach((id) => $(id).addEventListener('change', render));

      $('alRows').addEventListener('click', async (e) => {
        const id = e.target.dataset.id;
        if (!id) return;
        try {
          await Api.patch(`/api/alerts/${id}/status`, { status: e.target.dataset.status });
          await load();
        } catch (err) { App.flash(err.message, 'danger'); }
      });

      $('alertForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('alertError');
        box.classList.add('d-none');
        const deviceId = $('aDevice').value;
        try {
          await Api.post('/api/alerts', {
            title: $('aTitle').value.trim(),
            description: $('aDescription').value.trim(),
            severity: $('aSeverity').value,
            deviceId: deviceId ? Number(deviceId) : null,
          });
          modal().hide();
          App.flash('Alerta registrada.');
          await load();
        } catch (err) {
          box.textContent = err.message;
          box.classList.remove('d-none');
        }
      });
    },
    show: load,
  });
})();

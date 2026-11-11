(() => {
  let vulns = [];
  let devices = [];
  const modal = () => bootstrap.Modal.getOrCreateInstance($('vulnModal'));

  function actions(v) {
    if (!App.canEdit()) return '';
    const btn = (st, text) => `<button class="btn btn-sm btn-outline-secondary" data-vstatus="${st}" data-id="${v.id}">${text}</button>`;
    if (v.status === 'ABIERTA') return btn('EN_PROCESO', 'Iniciar') + ' ' + btn('CORREGIDA', 'Corregida');
    if (v.status === 'EN_PROCESO') return btn('CORREGIDA', 'Corregida');
    return btn('ABIERTA', 'Reabrir');
  }

  function render() {
    const q = $('vFilter').value.trim().toLowerCase();
    const sev = $('vSevFilter').value;
    const st = $('vStatusFilter').value;
    const shown = vulns.filter((v) => (!sev || v.severity === sev) && (!st || v.status === st)
      && (!q || [v.title, v.cve, v.description, v.deviceName].some((x) => x && x.toLowerCase().includes(q))));

    $('vRows').innerHTML = shown.length ? shown.map((v) => `<tr>
      <td>${sevBadge(v.severity)}</td>
      <td><div class="fw-semibold">${escapeHtml(v.title)}${v.cve ? ` <span class="badge badge-off ms-1">${escapeHtml(v.cve)}</span>` : ''}</div>
        ${v.description ? `<div class="small text-secondary small-desc">${escapeHtml(v.description)}</div>` : ''}</td>
      <td>${escapeHtml(v.deviceName || '—')}</td>
      <td>${vsBadge(v.status)}</td>
      <td>${fmtDate(v.createdAt)}${v.fixedAt ? `<div class="small text-secondary">Corregida: ${fmtDate(v.fixedAt)}</div>` : ''}</td>
      <td class="text-end text-nowrap">${actions(v)}</td></tr>`).join('')
      : `<tr><td colspan="6" class="text-center text-secondary py-4">${vulns.length
        ? 'No hay vulnerabilidades que coincidan con los filtros.' : 'No hay vulnerabilidades registradas.'}</td></tr>`;
  }

  async function load() {
    [vulns, devices] = await Promise.all([Api.get('/api/vulnerabilities'), Api.get('/api/devices')]);
    $('vDevice').innerHTML = '<option value="">Sin dispositivo</option>'
      + devices.map((d) => `<option value="${d.id}">${escapeHtml(d.name)}</option>`).join('');
    render();
  }

  App.register({
    id: 'vulnerabilidades',
    title: 'Vulnerabilidades',
    init() {
      if (App.canEdit()) $('vNew').classList.remove('d-none');
      $('vNew').addEventListener('click', () => {
        $('vulnError').classList.add('d-none');
        $('vulnForm').reset();
        modal().show();
      });
      $('vFilter').addEventListener('input', render);
      ['vSevFilter', 'vStatusFilter'].forEach((id) => $(id).addEventListener('change', render));

      $('vRows').addEventListener('click', async (e) => {
        const id = e.target.dataset.id;
        if (!id) return;
        try { await Api.patch(`/api/vulnerabilities/${id}/status`, { status: e.target.dataset.vstatus }); await load(); }
        catch (err) { App.flash(err.message, 'danger'); }
      });

      $('vulnForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('vulnError');
        box.classList.add('d-none');
        const deviceId = $('vDevice').value;
        try {
          await Api.post('/api/vulnerabilities', {
            title: $('vTitle').value.trim(),
            cve: $('vCve').value.trim(),
            description: $('vDescription').value.trim(),
            severity: $('vSeverity').value,
            deviceId: deviceId ? Number(deviceId) : null,
          });
          modal().hide();
          App.flash('Vulnerabilidad registrada.');
          await load();
        } catch (err) { box.textContent = err.message; box.classList.remove('d-none'); }
      });
    },
    show: load,
  });
})();

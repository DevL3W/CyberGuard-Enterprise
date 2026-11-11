(() => {
  let incidents = [];
  let devices = [];
  let people = [];
  const modal = () => bootstrap.Modal.getOrCreateInstance($('incModal'));

  function statusCell(i) {
    if (!App.canEdit()) return inBadge(i.status);
    return `<select class="form-select form-select-sm w-auto" data-inc-status="${i.id}" aria-label="Estado de ${escapeHtml(i.title)}">
      ${Object.keys(LABEL.incStatus).map((s) => `<option value="${s}" ${s === i.status ? 'selected' : ''}>${LABEL.incStatus[s]}</option>`).join('')}</select>`;
  }

  function assigneeCell(i) {
    if (!App.canEdit()) return escapeHtml(i.assigneeName || 'Sin asignar');
    return `<select class="form-select form-select-sm w-auto" data-inc-assignee="${i.id}" aria-label="Responsable de ${escapeHtml(i.title)}">
      <option value="" ${i.assigneeId == null ? 'selected' : 'disabled'}>Sin asignar</option>
      ${people.map((p) => `<option value="${p.id}" ${p.id === i.assigneeId ? 'selected' : ''}>${escapeHtml(p.fullName)}</option>`).join('')}</select>`;
  }

  function render() {
    const q = $('incFilter').value.trim().toLowerCase();
    const sev = $('incSevFilter').value;
    const st = $('incStatusFilter').value;
    const shown = incidents.filter((i) => (!sev || i.severity === sev) && (!st || i.status === st)
      && (!q || [i.title, i.description, i.deviceName, i.assigneeName].some((x) => x && x.toLowerCase().includes(q))));

    $('incRows').innerHTML = shown.length ? shown.map((i) => `<tr>
      <td>${sevBadge(i.severity)}</td>
      <td><div class="fw-semibold">${escapeHtml(i.title)}</div>
        ${i.description ? `<div class="small text-secondary small-desc">${escapeHtml(i.description)}</div>` : ''}
        <div class="small text-secondary">Registrado por ${escapeHtml(i.createdByName || '—')}</div></td>
      <td>${escapeHtml(i.deviceName || '—')}</td>
      <td>${statusCell(i)}</td>
      <td>${assigneeCell(i)}</td>
      <td>${fmtDate(i.createdAt)}${i.closedAt ? `<div class="small text-secondary">Cerrado: ${fmtDate(i.closedAt)}</div>` : ''}</td></tr>`).join('')
      : `<tr><td colspan="6" class="text-center text-secondary py-4">${incidents.length
        ? 'No hay incidentes que coincidan con los filtros.' : 'No hay incidentes registrados.'}</td></tr>`;
  }

  async function load() {
    [incidents, devices, people] = await Promise.all([
      Api.get('/api/incidents'), Api.get('/api/devices'), Api.get('/api/directory')]);
    $('iDevice').innerHTML = '<option value="">Sin dispositivo</option>'
      + devices.map((d) => `<option value="${d.id}">${escapeHtml(d.name)}</option>`).join('');
    $('iAssignee').innerHTML = '<option value="">Sin asignar</option>'
      + people.map((p) => `<option value="${p.id}">${escapeHtml(p.fullName)}</option>`).join('');
    render();
  }

  async function patch(id, body) {
    try { await Api.patch('/api/incidents/' + id, body); }
    catch (err) { App.flash(err.message, 'danger'); }
    await load();
  }

  App.register({
    id: 'incidentes',
    title: 'Incidentes',
    init() {
      if (App.canEdit()) $('incNew').classList.remove('d-none');
      $('incNew').addEventListener('click', () => {
        $('incError').classList.add('d-none');
        $('incForm').reset();
        modal().show();
      });
      $('incFilter').addEventListener('input', render);
      ['incSevFilter', 'incStatusFilter'].forEach((id) => $(id).addEventListener('change', render));

      $('incRows').addEventListener('change', (e) => {
        const t = e.target.dataset;
        if (t.incStatus) patch(t.incStatus, { status: e.target.value });
        if (t.incAssignee && e.target.value) patch(t.incAssignee, { assigneeId: Number(e.target.value) });
      });

      $('incForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('incError');
        box.classList.add('d-none');
        const deviceId = $('iDevice').value;
        const assigneeId = $('iAssignee').value;
        try {
          await Api.post('/api/incidents', {
            title: $('iTitle').value.trim(),
            description: $('iDescription').value.trim(),
            severity: $('iSeverity').value,
            deviceId: deviceId ? Number(deviceId) : null,
            assigneeId: assigneeId ? Number(assigneeId) : null,
          });
          modal().hide();
          App.flash('Incidente registrado.');
          await load();
        } catch (err) { box.textContent = err.message; box.classList.remove('d-none'); }
      });
    },
    show: load,
  });
})();

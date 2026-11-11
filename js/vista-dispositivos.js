(() => {
  let devices = [];
  const modal = () => bootstrap.Modal.getOrCreateInstance($('deviceModal'));

  function render() {
    const q = $('devFilter').value.trim().toLowerCase();
    const st = $('devStatusFilter').value;
    const edit = App.canEdit();
    const del = App.isAdmin();
    const shown = devices.filter((d) => (!st || d.status === st)
      && (!q || [d.name, d.hostname, d.ipAddress, d.operatingSystem].some((v) => v && v.toLowerCase().includes(q))));

    $('devRows').innerHTML = shown.length ? shown.map((d) => `<tr>
      <td class="fw-semibold">${escapeHtml(d.name)}${d.hasAgent ? ' <span class="badge badge-on ms-1">Agente</span>' : ''}</td>
      <td>${escapeHtml(d.hostname || '—')}<div class="small text-secondary">${escapeHtml(d.ipAddress || '')}</div></td>
      <td>${escapeHtml(LABEL.type[d.type] || d.type)}</td>
      <td>${escapeHtml(d.operatingSystem || '—')}</td>
      <td>${devBadge(d.status)}</td>
      <td>${fmtDate(d.lastSeen)}</td>
      <td class="text-end text-nowrap">
        ${edit ? `<button class="btn btn-sm btn-outline-secondary" data-edit="${d.id}">Editar</button>
          <button class="btn btn-sm btn-outline-secondary" data-agent="${d.id}">Agente</button>` : ''}
        ${del ? `<button class="btn btn-sm btn-outline-danger" data-del="${d.id}">Eliminar</button>` : ''}
      </td></tr>`).join('')
      : `<tr><td colspan="7" class="text-center text-secondary py-4">${devices.length
        ? 'No hay dispositivos que coincidan con los filtros.'
        : 'Todavía no hay dispositivos registrados.' + (edit ? ' Use "Registrar dispositivo" para añadir el primero.' : '')}</td></tr>`;
  }

  async function load() {
    devices = await Api.get('/api/devices');
    render();
  }

  function openForm(d) {
    $('deviceError').classList.add('d-none');
    $('deviceModalTitle').textContent = d ? 'Editar dispositivo' : 'Registrar dispositivo';
    $('dId').value = d ? d.id : '';
    $('dName').value = d ? d.name : '';
    $('dHostname').value = d && d.hostname ? d.hostname : '';
    $('dIp').value = d && d.ipAddress ? d.ipAddress : '';
    $('dType').value = d ? d.type : 'COMPUTADOR';
    $('dStatus').value = d ? d.status : 'OFFLINE';
    $('dOs').value = d && d.operatingSystem ? d.operatingSystem : '';
    modal().show();
  }

  async function createAgentKey(id) {
    const d = devices.find((x) => String(x.id) === id);
    const msg = d.hasAgent
      ? `"${d.name}" ya tiene un agente. Una clave nueva desconectará al agente actual hasta que lo actualice. ¿Continuar?`
      : `Se generará la clave del agente para "${d.name}". Se muestra una sola vez. ¿Continuar?`;
    if (!confirm(msg)) return;
    try {
      const res = await Api.post(`/api/devices/${id}/agent-key`);
      $('agDevice').textContent = d.name;
      $('agKey').value = res.key;
      $('agCmd').value = `java CyberGuardAgent.java --server ${location.origin} --device-id ${res.deviceId} --key ${res.key}`;
      bootstrap.Modal.getOrCreateInstance($('agentModal')).show();
      await load();
    } catch (err) { App.flash(err.message, 'danger'); }
  }

  // Botones "Copiar" de la ventana del agente
  document.addEventListener('click', async (e) => {
    const from = e.target.dataset && e.target.dataset.copyFrom;
    if (!from) return;
    try { await navigator.clipboard.writeText($(from).value); e.target.textContent = 'Copiado'; }
    catch (_) { $(from).select(); }
    setTimeout(() => { e.target.textContent = 'Copiar'; }, 1500);
  });

  App.register({
    id: 'dispositivos',
    title: 'Dispositivos',
    init() {
      if (App.canEdit()) $('devNew').classList.remove('d-none');
      $('devNew').addEventListener('click', () => openForm(null));
      $('devFilter').addEventListener('input', render);
      $('devStatusFilter').addEventListener('change', render);

      $('devRows').addEventListener('click', async (e) => {
        const editId = e.target.dataset.edit;
        const delId = e.target.dataset.del;
        if (e.target.dataset.agent) await createAgentKey(e.target.dataset.agent);
        if (editId) openForm(devices.find((x) => String(x.id) === editId));
        if (delId) {
          const d = devices.find((x) => String(x.id) === delId);
          if (!confirm(`¿Eliminar el dispositivo "${d.name}"? Sus alertas se conservarán sin dispositivo asociado.`)) return;
          try { await Api.del('/api/devices/' + delId); App.flash('Dispositivo eliminado.'); await load(); }
          catch (err) { App.flash(err.message, 'danger'); }
        }
      });

      $('deviceForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('deviceError');
        box.classList.add('d-none');
        const id = $('dId').value;
        const body = {
          name: $('dName').value.trim(),
          hostname: $('dHostname').value.trim(),
          ipAddress: $('dIp').value.trim(),
          type: $('dType').value,
          operatingSystem: $('dOs').value.trim(),
          status: $('dStatus').value,
        };
        try {
          if (id) await Api.put('/api/devices/' + id, body); else await Api.post('/api/devices', body);
          modal().hide();
          App.flash('Dispositivo guardado.');
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

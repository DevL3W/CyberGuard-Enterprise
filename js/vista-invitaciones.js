(() => {
  const BASE_KEY = 'cg_base_url';
  const STATUS = { ACTIVA: 'Activa', REVOCADA: 'Revocada', VENCIDA: 'Vencida', AGOTADA: 'Agotada' };
  let items = [];
  let lastToken = null; // solo en memoria mientras el modal está abierto: el servidor no lo guarda

  const isLocal = (url) => /^https?:\/\/(localhost|127\.\d+\.\d+\.\d+|\[::1\])(:|\/|$)/i.test(url);

  function savedBase() {
    try { return localStorage.getItem(BASE_KEY); } catch (_) { return null; }
  }
  function saveBase(v) {
    try { localStorage.setItem(BASE_KEY, v); } catch (_) { /* sin almacenamiento: no pasa nada */ }
  }

  // Dirección del servidor sin barra final; devuelve null si no es una dirección http(s) válida
  function base() {
    const v = $('baseUrl').value.trim().replace(/\/+$/, '');
    try {
      const u = new URL(v);
      return (u.protocol === 'http:' || u.protocol === 'https:') && !u.search && !u.hash ? v : null;
    } catch (_) { return null; }
  }

  // Rellena un bloque con código QR + enlace (el mismo bloque sirve para el acceso y para la invitación)
  function fill(box, url) {
    box.dataset.url = url;
    box.querySelector('[data-link]').value = url;
    box.querySelector('[data-status]').textContent = '';
    try {
      box.querySelector('[data-qr]').innerHTML = QR.svg(url);
    } catch (err) {
      box.querySelector('[data-qr]').innerHTML = `<span class="small text-danger">${escapeHtml(err.message)}</span>`;
    }
    if (navigator.share) box.querySelector('[data-share-act="share"]').classList.remove('d-none');
  }

  function refresh() {
    const b = base();
    const box = $('accessBox');
    $('baseError').classList.toggle('d-none', !!b);
    if (!b) {
      $('baseError').textContent = 'Escriba una dirección que empiece por http:// o https://, sin parámetros.';
      $('localWarn').classList.add('d-none');
      box.classList.add('d-none');
      return;
    }
    box.classList.remove('d-none');
    $('localWarn').classList.toggle('d-none', !isLocal(b));
    fill(box, b + '/login.html');
    if (lastToken) {
      fill($('inviteBox'), b + '/join.html#' + lastToken);
      $('inviteLocalWarn').classList.toggle('d-none', !isLocal(b));
    }
  }

  async function onShareAction(e) {
    const btn = e.target.closest('[data-share-act]');
    const box = btn && btn.closest('.share-box');
    if (!box || !box.dataset.url) return;
    const status = box.querySelector('[data-status]');
    const url = box.dataset.url;
    try {
      if (btn.dataset.shareAct === 'copy') {
        await navigator.clipboard.writeText(url);
        status.textContent = 'Enlace copiado.';
      } else if (btn.dataset.shareAct === 'png') {
        const a = document.createElement('a');
        a.href = QR.png(url);
        a.download = box.dataset.filename || 'cyberguard-qr.png';
        document.body.appendChild(a);
        a.click();
        a.remove();
        status.textContent = 'Imagen del QR descargada.';
      } else if (btn.dataset.shareAct === 'share') {
        await navigator.share({ title: 'CyberGuard Enterprise', url });
      }
    } catch (err) {
      if (err && err.name === 'AbortError') return; // la persona cerró el menú de compartir
      status.textContent = btn.dataset.shareAct === 'copy'
        ? 'No se pudo copiar. Selecciónelo en el campo y cópielo a mano.' : 'No se pudo completar la acción.';
    }
  }

  function render() {
    $('invRows').innerHTML = items.length ? items.map((i) => `<tr>
      <td>${escapeHtml(ROLE_LABEL[i.role] || i.role)}</td>
      <td>${escapeHtml(i.createdBy || '—')}</td>
      <td>${fmtDate(i.expiresAt)}</td>
      <td>${i.uses} / ${i.maxUses}</td>
      <td><span class="badge ${i.status === 'ACTIVA' ? 'badge-on' : 'badge-off'}">${escapeHtml(STATUS[i.status] || i.status)}</span></td>
      <td class="text-end">${i.status === 'ACTIVA'
        ? `<button class="btn btn-sm btn-outline-secondary" data-revoke="${i.id}">Revocar</button>` : ''}</td></tr>`).join('')
      : '<tr><td colspan="6" class="text-center text-secondary py-4">Todavía no hay invitaciones. Cree una para compartir un enlace o un QR.</td></tr>';
  }

  async function load() {
    items = await Api.get('/api/invitations');
    render();
  }

  function showForm() {
    $('inviteForm').classList.remove('d-none');
    $('inviteResult').classList.add('d-none');
    $('inviteModalTitle').textContent = 'Nueva invitación';
    $('inviteError').classList.add('d-none');
  }

  App.register({
    id: 'invitaciones',
    title: 'Invitaciones',
    roles: ['ADMIN'],
    admin: true,
    init() {
      const saved = savedBase();
      $('baseUrl').value = saved && /^https?:\/\//i.test(saved) ? saved : location.origin;
      $('baseUrl').addEventListener('input', () => { refresh(); const b = base(); if (b) saveBase(b); });
      document.addEventListener('click', onShareAction);

      $('newInvite').addEventListener('click', () => {
        if (!base()) { App.flash('Primero escriba una dirección del servidor válida.', 'danger'); return; }
        $('inviteForm').reset();
        showForm();
        bootstrap.Modal.getOrCreateInstance($('inviteModal')).show();
      });

      // Al cerrar el modal el token se descarta de la memoria
      $('inviteModal').addEventListener('hidden.bs.modal', () => {
        lastToken = null;
        $('inviteBox').dataset.url = '';
        $('inviteBox').querySelector('[data-link]').value = '';
        $('inviteBox').querySelector('[data-qr]').innerHTML = '';
        showForm();
        load().catch(() => {});
      });

      $('inviteForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('inviteError');
        box.classList.add('d-none');
        const maxUses = parseInt($('iUses').value, 10);
        if (!(maxUses >= 1 && maxUses <= 100)) {
          box.textContent = 'El número de personas debe estar entre 1 y 100.';
          box.classList.remove('d-none');
          return;
        }
        try {
          const res = await Api.post('/api/invitations', {
            role: $('iRole').value, days: parseInt($('iDays').value, 10), maxUses,
          });
          lastToken = res.token;
          $('inviteSummary').textContent = `Rol: ${ROLE_LABEL[res.invitation.role]} · hasta ${res.invitation.maxUses} persona(s) · vence el ${fmtDate(res.invitation.expiresAt)}.`;
          $('inviteForm').classList.add('d-none');
          $('inviteResult').classList.remove('d-none');
          $('inviteModalTitle').textContent = 'Invitación lista para compartir';
          refresh();
        } catch (err) {
          box.textContent = err.message;
          box.classList.remove('d-none');
        }
      });

      $('invRows').addEventListener('click', async (e) => {
        const id = e.target.dataset.revoke;
        if (!id) return;
        if (!confirm('¿Revocar esta invitación? Su enlace y su QR dejarán de funcionar de inmediato. Las sesiones ya creadas se conservan.')) return;
        try { await Api.del('/api/invitations/' + id); App.flash('Invitación revocada.'); await load(); }
        catch (err) { App.flash(err.message, 'danger'); }
      });
    },
    async show() {
      refresh();
      await load();
    },
  });
})();

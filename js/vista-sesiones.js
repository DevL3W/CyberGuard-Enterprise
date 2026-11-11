(() => {
  let users = [];

  function render() {
    const me = App.me;
    const q = $('filter').value.trim().toLowerCase();
    const shown = users.filter((u) => !q || u.fullName.toLowerCase().includes(q) || u.username.includes(q));
    $('statTotal').textContent = users.length;
    $('statActive').textContent = users.filter((u) => u.enabled).length;
    $('statOff').textContent = users.filter((u) => !u.enabled).length;

    $('rows').innerHTML = shown.length ? shown.map((u) => {
      const locked = u.id === me.id || u.protectedAccount;
      return `<tr>
        <td>${escapeHtml(u.fullName)} ${u.protectedAccount ? '<span class="badge badge-main ms-1">Principal</span>' : ''}</td>
        <td>${escapeHtml(u.username)}</td>
        <td><select class="form-select form-select-sm w-auto" data-role="${u.id}" ${locked ? 'disabled' : ''} aria-label="Rol de ${escapeHtml(u.username)}">
          ${Object.keys(ROLE_LABEL).map((r) => `<option value="${r}" ${r === u.role ? 'selected' : ''}>${ROLE_LABEL[r]}</option>`).join('')}
        </select></td>
        <td><span class="badge ${u.enabled ? 'badge-on' : 'badge-off'}">${u.enabled ? 'Activa' : 'Desactivada'}</span></td>
        <td class="text-end text-nowrap">
          <button class="btn btn-sm btn-outline-secondary" data-reset="${u.id}" ${locked ? 'disabled' : ''}>Restablecer clave</button>
          <button class="btn btn-sm btn-outline-secondary" data-toggle="${u.id}" ${locked ? 'disabled' : ''}>
          ${u.enabled ? 'Desactivar' : 'Activar'}</button></td></tr>`;
    }).join('') : '<tr><td colspan="5" class="text-center text-secondary py-4">No hay sesiones que coincidan con la búsqueda.</td></tr>';
  }

  async function load() {
    users = await Api.get('/api/users');
    render();
  }

  App.register({
    id: 'sesiones',
    title: 'Sesiones',
    roles: ['ADMIN'],
    admin: true,
    init() {
      $('filter').addEventListener('input', render);

      $('rows').addEventListener('click', (e) => {
        const id = e.target.dataset.reset;
        if (!id) return;
        const u = users.find((x) => String(x.id) === id);
        $('rpError').classList.add('d-none');
        $('resetForm').reset();
        $('rpId').value = id;
        $('rpUser').textContent = u.username;
        bootstrap.Modal.getOrCreateInstance($('resetModal')).show();
      });

      $('resetForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('rpError');
        box.classList.add('d-none');
        try {
          await Api.patch(`/api/users/${$('rpId').value}/password`, { newPassword: $('rpPassword').value });
          bootstrap.Modal.getInstance($('resetModal')).hide();
          App.flash('Contraseña restablecida.');
        } catch (err) { box.textContent = err.message; box.classList.remove('d-none'); }
      });

      $('rows').addEventListener('change', async (e) => {
        const id = e.target.dataset.role;
        if (!id) return;
        try { await Api.patch('/api/users/' + id, { role: e.target.value }); App.flash('Rol actualizado.'); }
        catch (err) { App.flash(err.message, 'danger'); }
        await load();
      });

      $('rows').addEventListener('click', async (e) => {
        const id = e.target.dataset.toggle;
        if (!id) return;
        const u = users.find((x) => String(x.id) === id);
        try { await Api.patch('/api/users/' + id, { enabled: !u.enabled }); await load(); }
        catch (err) { App.flash(err.message, 'danger'); }
      });

      $('userForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const box = $('formError');
        box.classList.add('d-none');
        try {
          await Api.post('/api/users', {
            fullName: $('fFullName').value.trim(),
            username: $('fUsername').value.trim(),
            password: $('fPassword').value,
            role: $('fRole').value,
          });
          bootstrap.Modal.getInstance($('userModal')).hide();
          $('userForm').reset();
          App.flash('Sesión creada.');
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

// Núcleo del panel: rol de la sesión, navegación por rol y utilidades comunes.
const ROLE_LABEL = { ADMIN: 'Administrador', ANALISTA: 'Analista', LECTOR: 'Solo consulta' };
const LABEL = {
  type: { SERVIDOR: 'Servidor', COMPUTADOR: 'Computador', RED: 'Equipo de red', OTRO: 'Otro' },
  devStatus: { ONLINE: 'En línea', OFFLINE: 'Sin conexión', MANTENIMIENTO: 'Mantenimiento' },
  severity: { CRITICA: 'Crítica', ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' },
  alertStatus: { ABIERTA: 'Abierta', RECONOCIDA: 'Reconocida', RESUELTA: 'Resuelta' },
  vulnStatus: { ABIERTA: 'Abierta', EN_PROCESO: 'En proceso', CORREGIDA: 'Corregida' },
  incStatus: { ABIERTO: 'Abierto', EN_INVESTIGACION: 'En investigación', CONTENIDO: 'Contenido', CERRADO: 'Cerrado' },
};

const $ = (id) => document.getElementById(id);
const initials = (name) => (name || '?').trim().split(/\s+/).slice(0, 2).map((w) => w[0].toUpperCase()).join('');
const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString('es', { dateStyle: 'medium', timeStyle: 'short' }) : '—');
const sevBadge = (v) => `<span class="badge badge-sev-${v}">${escapeHtml(LABEL.severity[v] || v)}</span>`;
const devBadge = (v) => `<span class="badge badge-dev-${v}">${escapeHtml(LABEL.devStatus[v] || v)}</span>`;
const alBadge = (v) => `<span class="badge badge-al-${v}">${escapeHtml(LABEL.alertStatus[v] || v)}</span>`;
const vsBadge = (v) => `<span class="badge badge-vs-${v}">${escapeHtml(LABEL.vulnStatus[v] || v)}</span>`;
const inBadge = (v) => `<span class="badge badge-in-${v}">${escapeHtml(LABEL.incStatus[v] || v)}</span>`;

const App = (() => {
  const views = [];
  const soon = [];
  let me = null;
  let flashTimer = null;

  const allowed = (v) => !v.roles || v.roles.includes(me.role);
  const link = (v) => `<a class="nav-item-cg" href="#${v.id}" data-view="${v.id}">${escapeHtml(v.title)}</a>`;

  function buildNav() {
    const main = views.filter((v) => allowed(v) && !v.admin && !v.account);
    const acct = views.filter((v) => allowed(v) && v.account);
    const admin = views.filter((v) => allowed(v) && v.admin);
    let html = main.map(link).join('');
    if (admin.length) html += '<div class="nav-label">Administración</div>' + admin.map(link).join('');
    if (acct.length) html += '<div class="nav-label">Mi cuenta</div>' + acct.map(link).join('');
    if (soon.length) {
      html += '<div class="nav-label">Próximamente</div>'
        + soon.map((n) => `<span class="nav-item-cg disabled">${n}</span>`).join('');
    }
    $('nav').innerHTML = html;
  }

  function flash(message, type) {
    const box = $('flash');
    clearTimeout(flashTimer);
    box.className = 'alert alert-' + (type || 'success');
    box.textContent = message;
    if ((type || 'success') === 'success') flashTimer = setTimeout(clearFlash, 5000);
  }
  function clearFlash() { $('flash').className = 'alert d-none'; }

  async function show() {
    const wanted = location.hash.slice(1);
    const v = views.find((x) => x.id === wanted && allowed(x)) || views.find(allowed);
    document.querySelectorAll('.view').forEach((el) => el.classList.add('d-none'));
    $('view-' + v.id).classList.remove('d-none');
    document.querySelectorAll('#nav a').forEach((a) => a.classList.toggle('active', a.dataset.view === v.id));
    $('viewTitle').textContent = v.title;
    clearFlash();
    try { if (v.show) await v.show(); } catch (err) { flash(err.message, 'danger'); }
  }

  async function start() {
    if (!Api.getToken()) { window.location.href = 'login.html'; return; }
    Splash.text('Verificando su sesión…');
    me = await Api.get('/api/auth/me');
    $('companyName').textContent = me.companyName;
    $('meName').textContent = me.fullName;
    $('meAvatar').textContent = initials(me.fullName);
    $('meRole').textContent = ROLE_LABEL[me.role] || me.role;
    $('roleBadge').textContent = ROLE_LABEL[me.role] || me.role;
    if (me.role === 'LECTOR') $('readOnlyNote').classList.remove('d-none');
    buildNav();
    for (const v of views) if (allowed(v) && v.init) v.init();
    window.addEventListener('hashchange', show);
    Splash.text('Cargando panel…');
    await show();
  }

  return {
    register: (v) => views.push(v),
    start, flash,
    setName(n) { me.fullName = n; $('meName').textContent = n; $('meAvatar').textContent = initials(n); },
    get me() { return me; },
    canEdit: () => me.role === 'ADMIN' || me.role === 'ANALISTA',
    isAdmin: () => me.role === 'ADMIN',
  };
})();

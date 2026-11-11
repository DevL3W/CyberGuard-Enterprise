// El token viaja en la parte #fragmento del enlace: el navegador no lo envía al servidor ni en los registros.
const form = document.getElementById('joinForm');
const errorBox = document.getElementById('error');
const submit = document.getElementById('submit');
const val = (id) => document.getElementById(id).value;
const token = decodeURIComponent(location.hash.slice(1));
const ROLES = { ADMIN: 'Administrador', ANALISTA: 'Analista', LECTOR: 'Solo consulta' };

function showError(msg) {
  errorBox.textContent = msg;
  errorBox.classList.remove('d-none');
}

function invalid(msg) {
  if (msg) document.getElementById('invalidText').textContent = msg;
  document.getElementById('invalid').classList.remove('d-none');
}

async function start() {
  if (!/^[A-Za-z0-9_-]{20,100}$/.test(token)) { invalid(); return; }
  history.replaceState(null, '', location.pathname); // quita el token de la barra de direcciones
  try {
    const inv = await Api.get('/api/join/' + token);
    document.getElementById('company').textContent = inv.companyName;
    document.getElementById('role').textContent = ROLES[inv.role] || inv.role;
    form.classList.remove('d-none');
  } catch (err) {
    invalid(err.status === 404 || err.status === 401 ? null : err.message);
  }
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorBox.classList.add('d-none');
  const fullName = val('fullName').trim();
  const username = val('username').trim();
  const password = val('password');
  if (!fullName || !username || !password) return showError('Complete todos los campos.');
  if (password.length < 10) return showError('La contraseña debe tener al menos 10 caracteres.');
  if (password !== val('password2')) return showError('Las contraseñas no coinciden.');

  submit.disabled = true;
  submit.textContent = 'Creando su sesión…';
  try {
    const res = await Api.post('/api/join/' + token, { fullName, username, password });
    Api.setToken(res.token);
    window.location.href = 'dashboard.html';
  } catch (err) {
    if (err.status === 404) { form.classList.add('d-none'); invalid(); return; }
    showError(err.message);
    submit.disabled = false;
    submit.textContent = 'Crear mi sesión';
  }
});

start().finally(() => Splash.hide());

const form = document.getElementById('loginForm');
const errorBox = document.getElementById('error');
const submit = document.getElementById('submit');
const guestBtn = document.getElementById('guestBtn');
const pw = document.getElementById('password');
const togglePw = document.getElementById('togglePw');

if (Api.getToken()) {
  Splash.text('Abriendo su sesión…');
  window.location.href = 'dashboard.html';
} else {
  window.addEventListener('load', () => Splash.hide());
}

function showError(msg) {
  errorBox.textContent = msg;
  errorBox.classList.remove('d-none');
}

function setBusy(busy, label) {
  submit.disabled = busy;
  guestBtn.disabled = busy;
  if (label) submit.textContent = label;
}

togglePw.addEventListener('click', () => {
  const show = pw.type === 'password';
  pw.type = show ? 'text' : 'password';
  togglePw.textContent = show ? 'Ocultar' : 'Mostrar';
  togglePw.setAttribute('aria-pressed', String(show));
  togglePw.setAttribute('aria-label', show ? 'Ocultar contraseña' : 'Mostrar contraseña');
});

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorBox.classList.add('d-none');
  const username = document.getElementById('username').value.trim();
  const password = pw.value;
  if (!username || !password) {
    showError('Escriba el nombre de su sesión y su contraseña.');
    return;
  }
  setBusy(true, 'Iniciando sesión…');
  try {
    const res = await Api.post('/api/auth/login', { username, password });
    Api.setToken(res.token);
    window.location.href = 'dashboard.html';
  } catch (err) {
    showError(err.message);
    setBusy(false, 'Iniciar sesión');
  }
});

// Modo invitado: sesión de solo consulta (el servidor rechaza cualquier cambio)
guestBtn.addEventListener('click', async () => {
  errorBox.classList.add('d-none');
  setBusy(true);
  guestBtn.textContent = 'Entrando…';
  try {
    const res = await Api.post('/api/auth/guest');
    Api.setToken(res.token);
    window.location.href = 'dashboard.html';
  } catch (err) {
    showError(err.message);
    setBusy(false);
    guestBtn.textContent = 'Entrar solo para ver';
  }
});

// Mostrar "Crear cuenta" y el modo invitado solo si el servidor los tiene activados
Api.get('/api/auth/config')
  .then((c) => {
    if (!c) return;
    if (c.registrationEnabled) document.getElementById('registerLink').classList.remove('d-none');
    if (c.guestEnabled) document.getElementById('guestBox').classList.remove('d-none');
  })
  .catch(() => {});

const form = document.getElementById('registerForm');
const errorBox = document.getElementById('error');
const submit = document.getElementById('submit');
const val = (id) => document.getElementById(id).value;

function showError(msg) {
  errorBox.textContent = msg;
  errorBox.classList.remove('d-none');
}

if (Api.getToken()) {
  Splash.text('Abriendo su sesión…');
  window.location.href = 'dashboard.html';
} else {
  window.addEventListener('load', () => Splash.hide());
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorBox.classList.add('d-none');
  const companyName = val('companyName').trim();
  const fullName = val('fullName').trim();
  const username = val('username').trim();
  const password = val('password');

  if (!companyName || !fullName || !username || !password) return showError('Complete todos los campos.');
  if (password.length < 10) return showError('La contraseña debe tener al menos 10 caracteres.');
  if (password !== val('password2')) return showError('Las contraseñas no coinciden.');

  submit.disabled = true;
  submit.textContent = 'Creando cuenta…';
  try {
    const res = await Api.post('/api/auth/register', { companyName, fullName, username, password });
    Api.setToken(res.token);
    window.location.href = 'dashboard.html';
  } catch (err) {
    showError(err.message);
    submit.disabled = false;
    submit.textContent = 'Crear cuenta';
  }
});

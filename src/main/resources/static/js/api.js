// Cliente mínimo para la API REST. El token vive en sessionStorage (se borra al cerrar la pestaña).
const Api = (() => {
  const KEY = 'cg_token';
  const getToken = () => sessionStorage.getItem(KEY);
  const setToken = (t) => sessionStorage.setItem(KEY, t);
  const clear = () => sessionStorage.removeItem(KEY);

  async function request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
    let data = null;
    try { data = await res.json(); } catch (_) { /* respuesta sin cuerpo */ }
    if (res.status === 401 && !path.endsWith('/login')) {
      clear();
      window.location.href = 'login.html';
      throw new Error('Sesión expirada');
    }
    if (!res.ok) {
      const msg = (data && data.error)
        || (res.status === 403 ? 'No tiene permiso para realizar esta acción.' : 'Error inesperado (' + res.status + ')');
      const err = new Error(msg);
      err.status = res.status;
      throw err;
    }
    return data;
  }

  // Descarga un archivo protegido (el token va en la cabecera, por eso no sirve un enlace normal)
  async function download(path, filename) {
    const res = await fetch(path, { headers: { Authorization: 'Bearer ' + getToken() } });
    if (res.status === 401) { clear(); window.location.href = 'login.html'; throw new Error('Sesión expirada'); }
    if (!res.ok) throw new Error('No se pudo descargar el informe (' + res.status + ')');
    const url = URL.createObjectURL(await res.blob());
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  return {
    getToken, setToken, clear, download,
    get: (p) => request('GET', p),
    post: (p, b) => request('POST', p, b),
    patch: (p, b) => request('PATCH', p, b),
    put: (p, b) => request('PUT', p, b),
    del: (p) => request('DELETE', p),
  };
})();

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// Pantalla de carga inicial
const Splash = (() => ({
  text(t) { const x = document.getElementById('splashText'); if (x) x.textContent = t; },
  hide() {
    const s = document.getElementById('splash');
    if (!s) return;
    s.classList.add('hide');
    setTimeout(() => s.remove(), 400);
  },
}))();

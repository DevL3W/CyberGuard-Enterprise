$('logout').addEventListener('click', () => { Api.clear(); window.location.href = 'login.html'; });

App.start()
  .catch((err) => {
    console.error(err);
    $('loadError').textContent = 'No se pudo cargar el panel: ' + err.message;
    $('loadError').classList.remove('d-none');
  })
  .finally(() => Splash.hide());

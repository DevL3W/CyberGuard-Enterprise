(() => {
  function block(title, data, labels) {
    const rows = Object.keys(data).map((k) =>
      `<tr><td>${escapeHtml(labels[k] || k)}</td><td>${data[k]}</td></tr>`).join('');
    return `<div class="col-md-6 col-xl-4"><div class="panel h-100">
      <div class="panel-head"><h3>${escapeHtml(title)}</h3></div>
      <table class="table rep-table mb-0"><tbody>${rows}</tbody></table></div></div>`;
  }

  App.register({
    id: 'informes',
    title: 'Informes',
    init() {
      document.querySelectorAll('[data-export]').forEach((btn) => btn.addEventListener('click', async () => {
        const name = btn.dataset.export;
        try { await Api.download('/api/reports/export/' + name, `cyberguard-${name}.csv`); }
        catch (err) { App.flash(err.message, 'danger'); }
      }));
      $('repPrint').addEventListener('click', () => window.print());
    },
    async show() {
      const o = await Api.get('/api/reports/overview');
      $('repMeta').textContent = 'Resumen generado el ' + fmtDate(o.generatedAt) + '. Tiempo medio de resolución de alertas: '
        + (o.avgAlertResolutionHours == null ? 'sin alertas resueltas todavía' : o.avgAlertResolutionHours + ' horas') + '.';
      $('repBody').innerHTML =
        block('Dispositivos por tipo', o.devicesByType, LABEL.type) +
        block('Dispositivos por estado', o.devicesByStatus, LABEL.devStatus) +
        block('Alertas por severidad (total)', o.alertsBySeverity, LABEL.severity) +
        block('Alertas por estado', o.alertsByStatus, LABEL.alertStatus) +
        block('Vulnerabilidades abiertas por severidad', o.openVulnerabilitiesBySeverity, LABEL.severity) +
        block('Incidentes por estado', o.incidentsByStatus, LABEL.incStatus);
    },
  });
})();

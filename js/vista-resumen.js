(() => {
  const charts = {};

  function draw(id, config) {
    if (typeof Chart === 'undefined') return; // sin conexión al CDN de Chart.js
    if (charts[id]) charts[id].destroy();
    charts[id] = new Chart($(id), config);
  }
  const stat = (n, label, cls) =>
    `<div class="col-6 col-md-4 col-xl"><div class="panel stat ${cls || ''}"><div class="n">${n}</div><div class="l">${label}</div></div></div>`;
  const toggle = (box, empty, isEmpty) => {
    $(box).classList.toggle('d-none', isEmpty);
    $(empty).classList.toggle('d-none', !isEmpty);
  };

  App.register({
    id: 'resumen',
    title: 'Resumen',
    async show() {
      const d = await Api.get('/api/dashboard');

      $('resStats').innerHTML =
        stat(d.totalDevices, 'Dispositivos') +
        stat(d.devicesOnline, 'En línea', 'ok') +
        stat(d.devicesOffline, 'Sin conexión', 'off') +
        stat(d.openAlerts, 'Alertas abiertas') +
        stat(d.criticalOpen, 'Críticas abiertas', 'danger') +
        stat(d.openVulnerabilities, 'Vulnerabilidades abiertas') +
        stat(d.openIncidents, 'Incidentes abiertos');

      toggle('boxDevices', 'emptyDevices', d.totalDevices === 0);
      if (d.totalDevices > 0) {
        draw('chDevices', {
          type: 'doughnut',
          data: {
            labels: ['En línea', 'Sin conexión', 'Mantenimiento'],
            datasets: [{ data: [d.devicesOnline, d.devicesOffline, d.devicesMaintenance],
              backgroundColor: ['#1F7A5A', '#8DA9C4', '#B7791F'], borderWidth: 0 }],
          },
          options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
        });
      }

      const sevKeys = ['CRITICA', 'ALTA', 'MEDIA', 'BAJA'];
      const sevData = sevKeys.map((k) => d.openBySeverity[k] || 0);
      toggle('boxSeverity', 'emptySeverity', d.openAlerts === 0);
      if (d.openAlerts > 0) {
        draw('chSeverity', {
          type: 'bar',
          data: { labels: sevKeys.map((k) => LABEL.severity[k]),
            datasets: [{ data: sevData, backgroundColor: ['#B3261E', '#C2570C', '#B7791F', '#5B7FA6'] }] },
          options: { maintainAspectRatio: false, plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } },
        });
      }

      draw('chWeek', {
        type: 'line',
        data: {
          labels: d.alertsLast7Days.map((x) => x.date.slice(8, 10) + '/' + x.date.slice(5, 7)),
          datasets: [{ data: d.alertsLast7Days.map((x) => x.count), borderColor: '#1D5FA8',
            backgroundColor: 'rgba(29,95,168,.12)', fill: true, tension: 0.3 }],
        },
        options: { maintainAspectRatio: false, plugins: { legend: { display: false } },
          scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } },
      });

      $('resRecent').innerHTML = d.recentAlerts.length ? d.recentAlerts.map((a) => `<tr>
        <td>${sevBadge(a.severity)}</td>
        <td>${escapeHtml(a.title)}</td>
        <td>${escapeHtml(a.deviceName || '—')}</td>
        <td>${alBadge(a.status)}</td>
        <td>${fmtDate(a.createdAt)}</td></tr>`).join('')
        : '<tr><td colspan="5" class="text-center text-secondary py-4">Todavía no hay alertas.</td></tr>';
    },
  });
})();

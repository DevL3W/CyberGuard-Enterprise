// Generador de códigos QR sin dependencias externas (modo byte, corrección de errores M, versiones 1 a 10).
// Basta para direcciones de hasta 213 caracteres. Uso: QR.svg(texto) o QR.png(texto).
const QR = (() => {
  // Nivel M: [palabras de corrección por bloque, [[número de bloques, palabras de datos por bloque], ...]]
  const EC = [null,
    [10, [[1, 16]]], [16, [[1, 28]]], [26, [[1, 44]]], [18, [[2, 32]]], [24, [[2, 43]]],
    [16, [[4, 27]]], [18, [[4, 31]]], [22, [[2, 38], [2, 39]]], [22, [[3, 36], [2, 37]]], [26, [[4, 43], [1, 44]]]];
  const ALIGN = [null, [], [6, 18], [6, 22], [6, 26], [6, 30], [6, 34], [6, 22, 38], [6, 24, 42], [6, 26, 46], [6, 28, 50]];
  const MAX_VERSION = 10;

  // Aritmética en GF(256) para Reed-Solomon
  const EXP = new Array(512);
  const LOG = new Array(256);
  for (let i = 0, x = 1; i < 255; i++) {
    EXP[i] = x; LOG[x] = i;
    x <<= 1;
    if (x & 256) x ^= 0x11D;
  }
  for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
  const mul = (a, b) => (a && b ? EXP[LOG[a] + LOG[b]] : 0);

  function generator(n) {
    let g = [1];
    for (let i = 0; i < n; i++) {
      const next = new Array(g.length + 1).fill(0);
      for (let j = 0; j < g.length; j++) {
        next[j] ^= g[j];
        next[j + 1] ^= mul(g[j], EXP[i]);
      }
      g = next;
    }
    return g;
  }

  function remainder(data, n) {
    const g = generator(n);
    const res = new Array(n).fill(0);
    for (const b of data) {
      const f = b ^ res.shift();
      res.push(0);
      for (let i = 0; i < n; i++) res[i] ^= mul(g[i + 1], f);
    }
    return res;
  }

  const dataCapacity = (ver) => EC[ver][1].reduce((s, [n, len]) => s + n * len, 0);

  function utf8(text) { return Array.from(new TextEncoder().encode(text)); }

  function codewords(bytes, ver) {
    const total = dataCapacity(ver);
    const bits = [];
    const put = (v, len) => { for (let i = len - 1; i >= 0; i--) bits.push((v >>> i) & 1); };
    put(4, 4);                                  // modo byte
    put(bytes.length, ver < 10 ? 8 : 16);       // longitud
    bytes.forEach((b) => put(b, 8));
    put(0, Math.min(4, total * 8 - bits.length)); // terminador
    while (bits.length % 8) bits.push(0);
    const data = [];
    for (let i = 0; i < bits.length; i += 8) data.push(parseInt(bits.slice(i, i + 8).join(''), 2));
    for (let pad = 0xEC; data.length < total; pad ^= 0xEC ^ 0x11) data.push(pad);

    const [eccLen, groups] = EC[ver];
    const blocks = [];
    let pos = 0;
    for (const [count, len] of groups) {
      for (let i = 0; i < count; i++) {
        const d = data.slice(pos, pos + len);
        pos += len;
        blocks.push({ d, e: remainder(d, eccLen) });
      }
    }
    const out = [];
    const longest = Math.max(...blocks.map((b) => b.d.length));
    for (let i = 0; i < longest; i++) for (const b of blocks) if (i < b.d.length) out.push(b.d[i]);
    for (let i = 0; i < eccLen; i++) for (const b of blocks) out.push(b.e[i]);
    return out;
  }

  const bit = (x, i) => ((x >>> i) & 1) !== 0;
  const MASKS = [
    (x, y) => (x + y) % 2 === 0,
    (x, y) => y % 2 === 0,
    (x, y) => x % 3 === 0,
    (x, y) => (x + y) % 3 === 0,
    (x, y) => (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0,
    (x, y) => ((x * y) % 2) + ((x * y) % 3) === 0,
    (x, y) => (((x * y) % 2) + ((x * y) % 3)) % 2 === 0,
    (x, y) => ((((x + y) % 2) + ((x * y) % 3)) % 2) === 0,
  ];

  function penalty(m) {
    const n = m.length;
    let p = 0;
    for (let a = 0; a < n; a++) {
      for (const rows of [true, false]) {
        const at = (i) => (rows ? m[a][i] : m[i][a]);
        let run = 1;
        for (let i = 1; i < n; i++) {
          if (at(i) === at(i - 1)) { run++; if (run === 5) p += 3; else if (run > 5) p++; } else run = 1;
        }
        for (let i = 0; i + 10 < n; i++) { // patrones parecidos a los marcadores de posición
          const w = Array.from({ length: 11 }, (_, k) => (at(i + k) ? 1 : 0)).join('');
          if (w === '10111010000' || w === '00001011101') p += 40;
        }
      }
    }
    for (let y = 0; y < n - 1; y++) {
      for (let x = 0; x < n - 1; x++) {
        if (m[y][x] === m[y][x + 1] && m[y][x] === m[y + 1][x] && m[y][x] === m[y + 1][x + 1]) p += 3;
      }
    }
    const dark = m.reduce((s, r) => s + r.filter(Boolean).length, 0);
    p += (Math.ceil(Math.abs(dark * 20 - n * n * 10) / (n * n)) - 1) * 10;
    return p;
  }

  function matrixFor(text) {
    const bytes = utf8(text);
    let ver = 1;
    while (ver <= MAX_VERSION && 4 + (ver < 10 ? 8 : 16) + bytes.length * 8 > dataCapacity(ver) * 8) ver++;
    if (ver > MAX_VERSION) throw new Error('La dirección es demasiado larga para el código QR');
    const size = 17 + 4 * ver;
    const m = Array.from({ length: size }, () => new Array(size).fill(false));
    const fn = Array.from({ length: size }, () => new Array(size).fill(false));
    const setFn = (r, c, v) => { m[r][c] = v; fn[r][c] = true; };

    for (let i = 0; i < size; i++) { setFn(6, i, i % 2 === 0); setFn(i, 6, i % 2 === 0); }
    for (const [cr, cc] of [[3, 3], [3, size - 4], [size - 4, 3]]) {
      for (let dr = -4; dr <= 4; dr++) {
        for (let dc = -4; dc <= 4; dc++) {
          const r = cr + dr, c = cc + dc;
          if (r < 0 || c < 0 || r >= size || c >= size) continue;
          const d = Math.max(Math.abs(dr), Math.abs(dc));
          setFn(r, c, d !== 2 && d !== 4);
        }
      }
    }
    const al = ALIGN[ver];
    for (let i = 0; i < al.length; i++) {
      for (let j = 0; j < al.length; j++) {
        if ((i === 0 && j === 0) || (i === 0 && j === al.length - 1) || (i === al.length - 1 && j === 0)) continue;
        for (let dr = -2; dr <= 2; dr++) {
          for (let dc = -2; dc <= 2; dc++) setFn(al[i] + dr, al[j] + dc, Math.max(Math.abs(dr), Math.abs(dc)) !== 1);
        }
      }
    }
    if (ver >= 7) {
      let rem = ver;
      for (let i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
      const bits = (ver << 12) | rem;
      for (let i = 0; i < 18; i++) {
        const a = size - 11 + (i % 3), b = Math.floor(i / 3);
        setFn(a, b, bit(bits, i)); setFn(b, a, bit(bits, i));
      }
    }

    function drawFormat(mask) {
      const data = mask; // nivel M = 00
      let rem = data;
      for (let i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
      const bits = ((data << 10) | rem) ^ 0x5412;
      for (let i = 0; i <= 5; i++) setFn(i, 8, bit(bits, i));
      setFn(7, 8, bit(bits, 6)); setFn(8, 8, bit(bits, 7)); setFn(8, 7, bit(bits, 8));
      for (let i = 9; i < 15; i++) setFn(8, 14 - i, bit(bits, i));
      for (let i = 0; i < 8; i++) setFn(8, size - 1 - i, bit(bits, i));
      for (let i = 8; i < 15; i++) setFn(size - 15 + i, 8, bit(bits, i));
      setFn(size - 8, 8, true);
    }
    drawFormat(0); // reserva la zona; se reescribe al elegir la máscara

    const data = codewords(bytes, ver);
    let k = 0;
    for (let right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right = 5;
      for (let vert = 0; vert < size; vert++) {
        for (let j = 0; j < 2; j++) {
          const x = right - j;
          const y = ((right + 1) & 2) === 0 ? size - 1 - vert : vert;
          if (!fn[y][x] && k < data.length * 8) { m[y][x] = bit(data[k >>> 3], 7 - (k & 7)); k++; }
        }
      }
    }

    let best = 0, bestScore = Infinity;
    for (let mask = 0; mask < 8; mask++) {
      const t = m.map((r) => r.slice());
      for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) if (!fn[y][x] && MASKS[mask](x, y)) t[y][x] = !t[y][x];
      const saved = m.map((r) => r.slice());
      for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) m[y][x] = t[y][x];
      drawFormat(mask);
      const score = penalty(m);
      if (score < bestScore) { bestScore = score; best = mask; }
      for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) m[y][x] = saved[y][x];
    }
    for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) if (!fn[y][x] && MASKS[best](x, y)) m[y][x] = !m[y][x];
    drawFormat(best);
    return m;
  }

  // Siempre negro sobre blanco y con margen de 4 módulos, para que cualquier cámara lo lea
  function svg(text) {
    const m = matrixFor(text);
    const margin = 4, n = m.length + margin * 2;
    let d = '';
    m.forEach((row, y) => row.forEach((on, x) => { if (on) d += `M${x + margin},${y + margin}h1v1h-1z`; }));
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${n} ${n}" shape-rendering="crispEdges" role="img" aria-label="Código QR">`
      + `<rect width="${n}" height="${n}" fill="#fff"/><path d="${d}" fill="#000"/></svg>`;
  }

  function png(text, scale = 10) {
    const m = matrixFor(text);
    const margin = 4, n = m.length + margin * 2;
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = n * scale;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#000';
    m.forEach((row, y) => row.forEach((on, x) => { if (on) ctx.fillRect((x + margin) * scale, (y + margin) * scale, scale, scale); }));
    return canvas.toDataURL('image/png');
  }

  return { svg, png, matrix: matrixFor };
})();

if (typeof module !== 'undefined') module.exports = QR; // solo para las pruebas con Node

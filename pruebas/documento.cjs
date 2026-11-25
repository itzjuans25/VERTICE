const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { execFileSync } = require('node:child_process');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');

const directory = 'docs/capturas';
const output = 'output/pdf/galeria/guia-vertice.pdf';

async function main() {
  const readme = fs.readFileSync('README.md', 'utf8').replace(/^!\[[^\n]+\]\(https:\/\/img\.shields\.io[^\n]+\)\r?\n/gm, '')
    .replace(/### Mediciones anteriores a la galería[\s\S]*?(?=## Despliegue público)/, '')
    .replace(/^\x60{3}mermaid\r?\n[\s\S]*?^\x60{3}/m,
    'React → Identidad, Catálogo y pedidos, y Subastas → PostgreSQL\n\nProductores → RabbitMQ → Notificaciones → Mailpit en Windows / Brevo en Render\n\nCatálogo y Subastas → Frankfurter y Nager.Date. Subastas → Open-Meteo.');
  const sustentacion = fs.readFileSync('docs/Sustentacion-Vertice.md', 'utf8')
    .replace(/^# /, '## ');
  const rendimiento = fs.readFileSync('resultados/vertice-galeria-rendimiento.md', 'utf8')
    .split('## Rutas por encima de 500 ms')[0].replace(/^# /, '## ');
  const rendered = execFileSync(process.env.PYTHON_PATH || 'python', ['-c',
    'from markdown_it import MarkdownIt; import sys; sys.stdout.write(MarkdownIt("gfm-like", {"linkify": False}).render(sys.stdin.read()))'],
  { input: [readme, rendimiento, sustentacion].join('\n\n'), encoding: 'utf8', env: { ...process.env, PYTHONIOENCODING: 'utf-8' } });
  const capturas = ['inicio-1440','subastas-1440','lote-1440','puja-adjudicada','puja-correo-ganada','compra-compra','avisos'];
  const gallery = capturas.map(paso => 'vertice-galeria-' + paso + '.png')
    .filter(name => fs.existsSync(path.resolve(directory,name)))
    .map(name => '<figure><img src="' + pathToFileURL(path.resolve(directory, name)).href + '"><figcaption>' + name.slice(0, -4).replace(/-/g, ' ') + '</figcaption></figure>').join('');
  const rootUrl = pathToFileURL(path.resolve('.') + path.sep).href;
  const html = '<!doctype html><html lang="es"><meta charset="utf-8"><base href="' + rootUrl + '">' +
    '<title>Guía de VÉRTICE</title><style>' +
    '@page{size:A4;margin:17mm 17mm 19mm}body{font:11pt/1.45 "Segoe UI",sans-serif;color:#17202c}' +
    'h1,h2,h3{color:#25354c;break-after:avoid}h1{border-bottom:2px solid #298ba6}a{color:#215482}' +
    'table{border-collapse:collapse;width:100%;font-size:9.5pt}thead{display:table-header-group}' +
    'td,th{border-bottom:1px solid #d8dde5;padding:5px;text-align:left;vertical-align:top}th{background:#edf1f5}tr{break-inside:avoid}' +
    'pre{background:#f2f4f7;padding:9px;white-space:pre-wrap;font-size:9pt}code{overflow-wrap:anywhere}' +
    'img{max-width:100%;height:auto}figure{margin:0 0 10mm;break-inside:avoid}figcaption{color:#4c596e;font-size:9pt}' +
    '.galeria img{max-height:104mm;display:block;margin:auto}' +
    '</style><body>' + rendered + '<section class="galeria"><h2>Capturas de la ejecución local</h2>' + gallery + '</section></body></html>';
  const tmp = path.join(process.env.TEMP, 'ecommerce-frank-documento.html');
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(tmp, html);
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage();
    await page.goto(pathToFileURL(tmp).href, { waitUntil: 'load' });
    const errors = await page.locator('img').evaluateAll(images => images.filter(img => !img.complete || !img.naturalWidth).map(img => img.src));
    if (errors.length) throw new Error('Imágenes inválidas: ' + errors.join(', '));
    await page.pdf({ path: output, printBackground: true, preferCSSPageSize: true });
  } finally {
    await browser.close();
    fs.unlinkSync(tmp);
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });

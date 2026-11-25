const fs = require('node:fs');
const path = require('node:path');
const { marca, tituloMarca } = require('../src/marca');
const publico = path.join(__dirname, '..', 'public');
const html = path.join(publico, 'index.html');
const { colores, simbolo } = marca;
const escape = s => s.replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
const rect = ([x,y,w,h,rx]) => '<rect x="' + x + '" y="' + y + '" width="' + w + '" height="' + h + '" rx="' + rx + '"/>';
const paths = '<g fill="' + colores.accent + '" transform="rotate(-38 32 26)">' + simbolo.martillo.map(rect).join('') + '</g><g fill="' + colores.text + '">' + rect(simbolo.base) + '</g>';
const favicon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="' + simbolo.vista + '"><rect width="64" height="64" rx="12" fill="' + colores.background + '"/>' + paths + '</svg>';
fs.writeFileSync(path.join(publico, 'favicon.svg'), favicon + '\n');
const meta = [
  '<meta name="theme-color" content="' + colores.background + '" />',
  '<meta name="description" content="' + escape(marca.descripcion) + '" />',
  '<meta property="og:title" content="' + escape(tituloMarca()) + '" />',
  '<meta property="og:description" content="' + escape(marca.descripcion) + '" />',
  '<meta property="og:image" content="%PUBLIC_URL%/remata-social.png" />',
  '<link rel="icon" type="image/svg+xml" href="%PUBLIC_URL%/favicon.svg" />',
  '<link rel="apple-touch-icon" href="%PUBLIC_URL%/favicon-180.png" />',
  '<title>' + escape(tituloMarca()) + '</title>',
  '<style>:root{' + Object.entries(colores).map(([k,v]) => '--tech-' + k + ':' + v).join(';') + '}</style>',
];
const markup = fs.readFileSync(html, 'utf8').replace(/<!-- marca:inicio -->[\s\S]*?<!-- marca:fin -->/, '<!-- marca:inicio -->\n    ' + meta.join('\n    ') + '\n    <!-- marca:fin -->');
fs.writeFileSync(html, markup);
if (process.argv.includes('--imagenes')) {
  const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');
  async function render() {
    const browser = await chromium.launch({ channel: 'msedge' });
    try {
      const page = await browser.newPage();
      for (const lado of [32, 180, 512]) {
        await page.setViewportSize({width:lado,height:lado});
        await page.setContent('<style>body{margin:0}svg{display:block;width:100vw;height:100vh}</style>' + favicon);
        await page.screenshot({path:path.join(publico, 'favicon-' + lado + '.png'),omitBackground:true});
      }
      await page.setViewportSize({width:1200,height:630});
      await page.setContent('<meta charset="utf-8"><style>body{margin:0;background:' + colores.background + ';color:' + colores.text + ';display:grid;place-content:center;text-align:center;height:100vh;font:44px system-ui}svg{margin:auto;width:172px;height:172px}h1{font:120px Georgia;margin:4px}p{color:' + colores.accent + ';margin:0}</style>' + favicon + '<h1>' + escape(marca.nombre) + '</h1><p>' + escape(marca.lema) + '</p>');
      await page.screenshot({path:path.join(publico,'remata-social.png')});
    } finally { await browser.close(); }
  }
  render().catch(error => { console.error(error.message); process.exitCode = 1; });
}

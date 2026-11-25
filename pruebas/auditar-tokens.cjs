const fs = require('node:fs');
const { execFileSync } = require('node:child_process');
const postcss = require(require.resolve('postcss',{ paths:['./frontend'] }));
const valores = require(require.resolve('postcss-value-parser',{ paths:['./frontend'] }));
const base = process.argv[2];
const desdeGit = path => execFileSync('git',['show',base+':'+path],{encoding:'utf8'});
const leer = file => base ? desdeGit(file) : fs.readFileSync(file,'utf8');
const inventario = base
  ? ['ls-tree','-r','--name-only','-z',base,'--','frontend/src']
  : ['ls-files','-z','--','frontend/src'];
const archivos = execFileSync('git',inventario).toString().split('\0');
const resultado = { colores:0, medidas:0, tipografias:0, esquemas:0, puntosDeRuptura:0 };
const colores = /^(#[0-9a-f]{3,8}|white|black|transparent|red|gray|grey|blue|yellow|orange|green)$/i;
const medidas = /^-?(\d+|\d*\.\d+)(px|rem|em|ch|vh|vw|dvh|dvw|vmin|vmax|s|ms|deg)$/i;
function contenido(texto){
  const css=postcss.parse(texto);
  css.walkAtRules('media',r=>{ resultado.puntosDeRuptura += (r.params.match(/(min|max)-width:\s*[\d.]+(?:px|em|rem)/ig) || []).length; });
  css.walkDecls(d=>{
    if(d.prop==='color-scheme' && /^(light|dark)$/.test(d.value)) resultado.esquemas++;
    if(d.prop==='font-family' && !/^(var\(|inherit|initial|unset)/.test(d.value)) resultado.tipografias++;
    valores(d.value).walk(n=>{
      if(n.type==='function' && n.value==='url') return false;
      if(n.type==='function' && /^(rgb|rgba|hsl|hsla)$/.test(n.value)) {resultado.colores++;return false;}
      if(n.type==='word' && colores.test(n.value)) resultado.colores++;
      if(n.type==='word' && medidas.test(n.value)) resultado.medidas++;
    });
  });
}
for(const path of archivos.filter(path => path.endsWith('.css') && !path.endsWith('/tokens.css'))) contenido(leer(path));
for(const m of leer('frontend/public/index.html').matchAll(/<style>([\s\S]*?)<\/style>/g)) contenido(m[1]);
for(const path of archivos.filter(x=>x.endsWith('.js'))) {
  for(const style of leer(path).matchAll(/style=\{\{([\s\S]*?)\}\}/g)) {
    for(const literal of style[1].matchAll(/:\s*['"]([^'"]+)['"]/g)) {
      if (colores.test(literal[1])) resultado.colores++;
      if (medidas.test(literal[1])) resultado.medidas++;
    }
  }
}
resultado.totalVisual=resultado.colores+resultado.medidas+resultado.tipografias+resultado.esquemas;
console.log(JSON.stringify(resultado));

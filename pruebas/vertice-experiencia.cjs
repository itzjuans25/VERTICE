const assert = require('node:assert/strict');
const { randomBytes } = require('node:crypto');
const { readFileSync, writeFileSync } = require('node:fs');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');
const { marca } = require('../frontend/src/marca');

const url = 'http://localhost:3000';
const auth = 'http://localhost:9090/api/ecommerce/usuario';
const catalogo = 'http://localhost:9092/api/ecommerce/producto';
const subastas = 'http://localhost:9094/api/ecommerce/subastas';
const archivo = process.env.VERTICE_EXPERIENCIA_JSON || 'resultados/vertice-experiencia.json';
const salida = { marca: marca.nombre, vistas: [], integracion: {}, contraste: {} };
const soloIntegracion = process.env.REMATA_SOLO_INTEGRACION === 'true';
if (soloIntegracion) salida.vistas = JSON.parse(readFileSync(archivo,'utf8')).vistas;

async function json(url, opciones) {
  const r = await fetch(url, { ...opciones, signal: AbortSignal.timeout(12000) });
  assert.ok(r.ok, url + ' HTTP ' + r.status);
  return r.json();
}
async function abrir(page, ruta) {
  await page.goto(url + ruta);
  await page.locator('main h1').first().waitFor();
}
async function captura(page, nombre) {
  await page.screenshot({ path: 'docs/capturas/' + (process.env.VERTICE_EXPERIENCIA_CAPTURAS || 'vertice-') + nombre + '.png', animations:'disabled' });
}
function luminancia(hex) {
  return hex.slice(1).match(/../g).map(h=>parseInt(h,16)/255)
    .map(v=>v<.04045?v/12.92:((v+.055)/1.055)**2.4)
    .reduce((a,v,i)=>a+v*[.2126,.7152,.0722][i],0);
}
function contraste(a,b) {
  const valores = [luminancia(a),luminancia(b)].sort((x,y)=>y-x);
  return +( (valores[0]+.05)/(valores[1]+.05) ).toFixed(2);
}
async function main() {
  const browser = await chromium.launch({ channel:'msedge' });
  const escritorio = await browser.newPage({viewport:{width:1440,height:940},reducedMotion:'reduce'});
  try {
    const { colores } = marca;
    salida.contraste.textoFondo = contraste(colores.text,colores.background);
    salida.contraste.textoSuaveSuperficie = contraste(colores.muted,colores.surface);
    salida.contraste.accion = contraste(colores.ink,colores.accent);
    assert.ok(Object.values(salida.contraste).every(v=>v>=4.5));
    const tienda = await json(catalogo+'/explorar?categoria=Relojes&tamano=5');
    assert.ok(tienda.total>=3);
    const lote = (await json(subastas+'?grupo=RELOJES&tamano=5')).items.find(l=>l.estado==='ACTIVA');
    assert.ok(lote);
    const producto = tienda.items.find(p=>p.nombre.includes('Reloj')) || tienda.items[0];
    const ficha = '/subastas/' + lote.id;

    for(const ancho of soloIntegracion ? [] : [390,768,1280,1440,1920]) {
      const page = await browser.newPage({viewport:{width:ancho,height:950},reducedMotion:'reduce'});
      await abrir(page,'/');
      await page.getByRole('heading',{name:marca.lema,exact:true}).waitFor();
      assert.equal(await page.locator('main a[href^="/productos"]').count(), 0);
      assert.equal(await page.locator('#mercado-navigation .nav-link').first().textContent(), 'Subastas');
      assert.ok(await page.getByRole('link',{name:'Explorar subastas'}).isVisible());
      const anchoDoc = await page.evaluate(()=>document.documentElement.scrollWidth);
      assert.equal(anchoDoc,ancho);
      const registro = { ancho,documento:anchoDoc,desbordes:[],axe:[] };
      const pantallas = [['inicio','/'],['tienda','/productos'],['categoria','/categoria/relojes?ver=subastas'],['subastas','/subastas'],['lote',ficha]];
      const probarAxe = [390,768,1440].includes(ancho);
        for(const [pantalla,ruta] of pantallas) {
          await abrir(page,ruta);
          await page.locator('.mercado-layout img').first().waitFor({timeout:4000}).catch(()=>{});
          const anchoPantalla = await page.evaluate(()=>document.documentElement.scrollWidth);
          if (anchoPantalla > ancho) registro.desbordes.push({ pantalla, anchoPantalla });
          if (probarAxe && process.env.TECHSTORE_AXE_JS) {
            await page.addScriptTag({path:process.env.TECHSTORE_AXE_JS});
            const a=await page.evaluate(()=>window.axe.run({runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa']}}));
            registro.axe.push({pantalla,reglas:a.passes.length,fallos:a.violations.map(x=>({id:x.id,nodos:x.nodes.length}))});
          }
          if (ancho === 1440 || ancho === 390 && ['inicio','subastas'].includes(pantalla)) await captura(page,pantalla+'-'+ancho);
        }
      assert.deepEqual(registro.desbordes, [], 'Desborde horizontal a ' + ancho + ' px');
      salida.vistas.push(registro);
      await page.close();
    }

    await abrir(escritorio,'/');
    await escritorio.evaluate(()=>window.comprobacionSPA=27);
    const buscador = escritorio.getByLabel('Buscar lotes de subasta');
    await buscador.fill('Cronógrafo');
    await buscador.press('Enter');
    await escritorio.waitForURL('**/buscar?**');
    await escritorio.getByRole('heading',{ name:/Lotes · [1-9]/ }).waitFor();
    assert.equal(await escritorio.getByRole('heading',{name:/Catálogo ·/}).count(), 0);
    assert.equal(await escritorio.getByRole('link',{name:/Ensayo curado|Serie Boreal/}).count(), 0);
    assert.equal(await escritorio.evaluate(()=>window.comprobacionSPA),27);
    salida.integracion.busquedaSinRecarga = true;
    await captura(escritorio,'busqueda-1440');
    await abrir(escritorio,'/productos/'+producto.id);
    await escritorio.getByRole('heading',{name:'También en subasta'}).waitFor();
    await abrir(escritorio,ficha);
    await escritorio.getByRole('button',{name:'Ficha técnica'}).click();
    await escritorio.getByRole('button',{name:'Historial de pujas'}).click();
    await escritorio.getByRole('heading',{name:'Entrega simulada'}).waitFor();
    salida.integracion.fichaSeparada = true;
    assert.equal(await escritorio.title(),lote.titulo+' | '+marca.nombre);
    const vehiculos = await json(subastas+'?grupo=CARROS%2CMOTOS&tamano=24');
    assert.ok(vehiculos.items.some(l => l.categoria === 'CARROS'));
    assert.ok(vehiculos.items.some(l => l.categoria === 'MOTOS'));
    salida.integracion.vehiculosAgrupadosApi = true;
    const grupo = await browser.newPage({viewport:{width:1440,height:920}});
    await abrir(grupo,'/categoria/vehiculos?ver=subastas');
    try {
      await grupo.locator('main .mercado-card').first().waitFor({timeout:6000});
      await captura(grupo,'vehiculos-subastas');
      salida.integracion.vehiculosAgrupadosVista = true;
    } catch (error) {
      salida.integracion.vehiculosAgrupadosVista = false;
      salida.integracion.detalleVehiculos = await grupo.locator('main').innerText();
    } finally {
      await grupo.close();
    }

    const cliente = { nombre:'QA Vértice',email:'qa-remata-'+Date.now()+'@example.test',password:'QA-'+randomBytes(14).toString('hex') };
    await json(auth+'/save',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(cliente)});
    await abrir(escritorio,'/login');
    await escritorio.locator('#login-email').fill(cliente.email);
    await escritorio.locator('#login-password').fill(cliente.password);
    await escritorio.getByRole('button',{name:/Iniciar sesión/i}).click();
    await escritorio.waitForURL('**/subastas');
    await escritorio.locator('.mercado-cuenta summary').click();
    await escritorio.getByRole('link',{name:'Mi cuenta',exact:true}).click();
    await escritorio.getByRole('heading',{name:'Mis pujas',exact:true}).waitFor();
    await escritorio.getByRole('link',{name:'Avisos',exact:true}).click();
    await escritorio.getByText(/^Bienvenido a VÉRTICE$/).waitFor();
    await captura(escritorio,'avisos');
    salida.integracion.avisoLocal = true;
    await escritorio.getByRole('link',{name:'Seguridad',exact:true}).click();
    await escritorio.getByLabel('Nombre',{exact:true}).fill('QA Vértice actualizado');
    await escritorio.getByRole('button',{name:'Guardar perfil'}).click();
    await escritorio.getByText('Perfil actualizado.',{exact:true}).waitFor();
    salida.integracion.perfil = true;
    await escritorio.getByRole('link',{name:'Actividad',exact:true}).click();
    await escritorio.getByRole('link',{name:'Compras',exact:true}).click();
    await escritorio.getByRole('heading',{name:'Mis compras',exact:true}).waitFor();
    salida.cuenta = [];
    if (process.env.TECHSTORE_AXE_JS) {
      for (const ancho of [390,768,1440]) {
        await escritorio.setViewportSize({width:ancho,height:940});
        await escritorio.addScriptTag({path:process.env.TECHSTORE_AXE_JS});
        const a = await escritorio.evaluate(()=>window.axe.run({runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa']}}));
        salida.cuenta.push({ancho,reglas:a.passes.length,fallos:a.violations.map(x=>({id:x.id,nodos:x.nodes.length}))});
      }
    }
    salida.ok = true;
  } finally {
    await browser.close();
    writeFileSync(archivo,JSON.stringify(salida,null,2)+'\n');
  }
}
main().catch(error=>{ console.error(error.message); process.exitCode=1; });

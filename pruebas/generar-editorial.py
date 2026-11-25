import json, re
from pathlib import Path
import os

results=Path('resultados')
name='vertice-editorial-'
def load(s, actual=False):
    prefijo = 'vertice-curado-' if actual else name
    return json.loads((results / f'{prefijo}{s}.json').read_text(encoding='utf-8-sig'))
def read(s): return (results / f'{name}{s}').read_text(encoding='utf-8-sig')
def fmt(n): return f'{n:.2f}'.replace('.',',')
def ruta(e): return e['ruta'] or '/api/ecommerce/subastas'
def latex(s):
    return ''.join({'#':r'\#','&':r'\&','%':r'\%','_':r'\_',':':':','{':r'\{','}':r'\}'}.get(c,c) for c in str(s))
vu=[3,6,8]
loads=[load('k6-'+str(n), True) for n in vu]
flows=load('puja', True)
buy=load('compra')
nm=load('postman', True)
cv=load('cobertura', True)
edicion=load('e2e', True)['edicion']
datos=load('datos', True)
phases=load('k6-fases-8')['fases']
assert flows['ok']
assert buy['stockAntes'] == 1 and buy['stockDespues'] == 0
assert all(t['peticiones'] and len(t['endpoints'])==52 for t in loads)
assert all(set(c) == {'unitarias', 'conjunta'} and all('porcentaje' in item for item in c.values()) for c in cv.values())
reg=Path(os.environ['LOCALAPPDATA'])/'EcommerceDev'
servicios={'auth':'Auth','catalogo':'Catálogo','notificaciones':'Notificaciones','subastas':'Subastas'}
cold={}
for k,label in servicios.items():
    log=(reg/f'{k}.log').read_text(encoding='utf-8',errors='replace')
    m=re.search(r'Started [A-Za-z]+Application in ([0-9.]+) seconds',log)
    if m: cold[label]=m[1].replace('.',',')
plan=re.search(r'Execution Time: ([0-9.]+) ms',read('plan.txt'))[1].replace('.',',')
plan_compras=re.search(r'Execution Time: ([0-9.]+) ms',read('plan-compras.txt'))[1].replace('.',',')
ops={'auth':'Auth','cat':'Catálogo','notif':'Notificaciones','sub':'Subastas'}
peak=[]
for n,t in zip(vu,loads):
    found=[(ops[e['nombre'].split('_')[0]],e) for e in t['endpoints'] if e.get('max',0)>500]
    peak.append((n,found))
lines=['# Rendimiento de la galería curada VÉRTICE','',
       'k6 abrió sesiones y comprobó 52 rutas en cada perfil con los servicios activos. La revisión de navegador coincidió con los primeros perfiles. Las cifras describen esta ejecución local y el máximo de 500 ms se evalúa por ruta.','',
       '| VU | Peticiones | req/s | Media ms | p95 ms | p99 ms | Máximo ms | Errores |',
       '|---:|---:|---:|---:|---:|---:|---:|---:|']
for n,t in zip(vu,loads):
    lines.append(f"| {n} | {t['peticiones']} | {fmt(t['rps'])} | {fmt(t['avg'])} | {fmt(t['p(95)'])} | {fmt(t['p(99)'])} | {fmt(t['max'])} | {t['errores']} |")
lines+=['','## Rutas por encima de 500 ms','','| VU | Servicio | Ruta | p95 ms | Máximo ms |','|---:|---|---|---:|---:|']
for n,found in peak:
    if not found: lines.append(f'| {n} | — | Ninguna | — | — |')
    for svc,e in found: lines.append(f"| {n} | {svc} | `{e['metodo']} {ruta(e)}` | {fmt(e['p(95)'])} | {fmt(e['max'])} |")
lines+=['','## Resultados por ruta','','| Operación | 3 VU p95 / máx. | 6 VU p95 / máx. | 8 VU p95 / máx. |','|---|---:|---:|---:|']
bypos=[{e['nombre']:e for e in t['endpoints']} for t in loads]
for k, e in bypos[0].items():
    values=[' / '.join(fmt(p[k][a]) for a in ('p(95)','max')) if p[k]['peticiones'] else '—' for p in bypos]
    lines.append(f"| {ops[k.split('_')[0]]} `{e['metodo']} {ruta(e)}` | "+' | '.join(values)+' |')
lines+=['','## Newman','',f"{nm['peticiones']} peticiones, {nm['peticionesFallidas']} fallos HTTP, {nm['aserciones']} aserciones y {nm['asercionesFallidas']} fallos de aserción. Media {fmt(nm['promedioMs'])} ms. Máximo {fmt(nm['maximoMs'])} ms.",'']
lines.extend(f"- {x['caso']}. {x['mensaje']}." for x in nm['fallos'])
lines.extend(['','## Diagnóstico previo y arranque en frío','',
             'Spring Boot registró '+', '.join(f'{label} {n} s' for label,n in cold.items())+'. Los JAR iniciaron en paralelo. Es tiempo de inicialización, separado de la latencia de solicitudes con los servicios activos. La nube sigue pendiente.','',
             f'PostgreSQL ejecutó los planes aislados de portada y compras en {plan} y {plan_compras} ms, respectivamente. Las muestras con caché no establecen la causa de los picos HTTP de Catálogo.','',
             '| Ruta Auth | Fase | Muestras | Media ms |','|---|---|---:|---:|'])
lines+= [f"| {x['ruta']} | {x['fase']} | {int(x['muestras'])} | {fmt(x['mediaMs'])} |" for x in phases]
failures=[e for t in loads for e in t['endpoints'] if e['errores']]
if failures:
    lines.extend(['','## Errores funcionales en k6',''])
    for n,t in zip(vu,loads):
      lines.extend(f"- {n} VU. {ops[e['nombre'].split('_')[0]]} `{e['metodo']} {e['ruta']}`, {e['errores']} errores." for e in t['endpoints'] if e['errores'])

lines+=['','[3 VU](vertice-curado-k6-3.json) · [6 VU](vertice-curado-k6-6.json) · [8 VU](vertice-curado-k6-8.json) · [Fases Auth previas](vertice-editorial-k6-fases-8.json) · [Newman](vertice-curado-postman.json) · [Plan PostgreSQL](vertice-editorial-plan.txt)','']
(results/'vertice-curado-rendimiento.md').write_text('\n'.join(lines),encoding='utf-8')
T=Path('docs/articulo')
tex=r'''\newcommand{\GaleriaLatencia}{%
\begin{table}[!ht]
\caption{Latencia HTTP de la galería en milisegundos}
\centering\footnotesize
\begin{tabular}{rrrrrrr}
\toprule
VU & Media & p95 & p99 & Máximo & req/s & Errores\\\midrule
'''
for n,t in zip(vu,loads): tex+=' & '.join([str(n),*(fmt(t[k]) for k in ('avg','p(95)','p(99)','max','rps')),str(t['errores'])])+(chr(92)*2)+'\n'
tex+=r'''\bottomrule
\end{tabular}
\end{table}}
\newcommand{\GaleriaCobertura}{%
\begin{table}[!ht]
\caption{JaCoCo de líneas por servicio}
\centering\footnotesize
\begin{tabular}{lrr}
\toprule
Servicio & Unidad & Con integración\\\midrule
'''
for k in servicios: tex+=' & '.join([servicios[k],*(fmt(cv[k][s]['porcentaje'])+r'\,\%' for s in ('unitarias','conjunta'))])+(chr(92)*2)+'\n'
tex+=r'''\bottomrule
\end{tabular}
\end{table}}
'''
tex+=r'\newcommand{\GaleriaFlujos}{'+f"El lote {flows['lote']} registró dos postores y activó la extensión de cierre. La adjudicación {flows['adjudicacion']} mantuvo la orden al repetir el pago. Mailpit recibió ambos avisos. La compra {buy['pedido']} agotó la última unidad y recibió el correo. La actividad mostró ambos pagos y conservó su referencia al repetirse. La compra de edición {edicion['compra']} verificó cupo y reintento concurrente."+'}\n'
tex+=r'\newcommand{\GaleriaNewman}{'+f"Newman midió {nm['peticiones']} peticiones con {nm['peticionesFallidas']} errores HTTP y {nm['asercionesFallidas']} aserciones fallidas de {nm['aserciones']}. Máximo de {fmt(nm['maximoMs'])} ms."+'}\n'
h=next(x for x in phases if x['ruta']=='cambiar-clave' and x['fase']=='hash')
tex+=r'\newcommand{\GaleriaDiagnostico}{'+f"En la medición previa a 8 VU, cambiar la clave consumió {fmt(h['mediaMs'])} ms por hash en promedio. PostgreSQL ejecutó el plan aislado de la portada en {plan} ms. Los tiempos del arranque en frío se registraron aparte. El umbral por ruta y la cobertura unitaria del 80 "+r'\%'+ " siguen pendientes.}\n"
tex+=r'\newcommand{\GaleriaPicos}{' + r'\paragraph{Picos por perfil.}' + ' ' + ' '.join(f"{n} VU registró {len(rows)} rutas por encima de 500 ms." for n,rows in peak) + " Los resultados adjuntos enumeran los máximos y p95 por ruta.}\n"
(T/'metricas-editorial.tex').write_text(tex,encoding='utf-8')
rows={}
for n,found in peak:
    for svc, item in found:
        key=(svc,item['metodo']+' '+(item['ruta'].split('?')[0] or '/'))
        rows.setdefault(key,{})[n]=fmt(item['max'])
tex += r'\newcommand{\GaleriaRutas}{%'
if rows:
    tex += '\n'+r'''\begin{table}[!ht]
\caption{Máximos superiores a 500 ms}
\centering\scriptsize
\setlength{\tabcolsep}{3pt}
\begin{tabular}{llrrr}
\toprule
Servicio & Ruta & 3 VU & 6 VU & 8 VU\\\midrule
'''
    for (svc,route), values in sorted(rows.items()):
        tex += ' & '.join([svc,r'\texttt{'+latex(route)+'}',*(values.get(n,'--') for n in vu)])+chr(92)*2+'\n'
    tex += '\n'+r'''\bottomrule
\end{tabular}
\end{table}
'''
tex += '}\n'
(T/'metricas-editorial.tex').write_text(tex,encoding='utf-8')


def add(path,anchor,section):
    p=Path(path)
    src=p.read_text(encoding='utf-8')
    if section.strip() not in src:
        p.write_text(src.replace(anchor,section+anchor,1),encoding='utf-8')

unit=' / '.join(fmt(cv[k]['unitarias']['porcentaje']) for k in servicios)
combo=' / '.join(fmt(cv[k]['conjunta']['porcentaje']) for k in servicios)
maximos=' / '.join(fmt(t['max']) for t in loads)
intro=f"""La galería editorial comprobó 52 rutas k6. Los máximos con 3, 6 y 8 usuarios fueron {maximos} ms. Newman hizo {nm['peticiones']} peticiones y {nm['aserciones']} aserciones, con {nm['asercionesFallidas']} fallos. JaCoCo en unitarias {unit} % y suite conjunta {combo} %, en orden Auth, Catálogo, Notificaciones y Subastas. [Rendimiento por ruta](resultados/vertice-curado-rendimiento.md)

### Mediciones anteriores a la galería editorial

"""
add('README.md','La galería editorial comprobó 54 rutas',intro)
revision=f"""## Cierre de la galería curada

- Kit Cúspide v2 aplicado al tema claro. El panel de puja conserva la variante oscura. El inicio presenta cuatro cierres, tres lanzamientos y siete categorías cuyos totales se comprueban contra el listado activo. [Auditoría de tokens](resultados/vertice-editorial-tokens.json) · [Créditos](docs/CREDITOS_IMAGENES.md)
- La edición del pedido {edicion['compra']} comprobó cupo por cliente y confirmó un solo pago con dos llamadas concurrentes. La compra {buy['pedido']} agotó la última unidad. El lote {flows['lote']} verificó extensión de plazo, adjudicación {flows['adjudicacion']}, pago y Mailpit. [Edición](resultados/vertice-curado-e2e.json) · [Compra](resultados/vertice-editorial-compra.json) · [Subasta](resultados/vertice-curado-puja.json)
- Cuatro servicios locales. La configuración Render se validó para TLS, health, rutas de Docker y secretos externos. La publicación requiere las cuentas de destino. [Blueprint](resultados/vertice-editorial-cloud.json)
- k6 recorrió 52 rutas. Newman ejecutó {nm['peticiones']} peticiones y JaCoCo midió unidad e integración por separado. [Rendimiento](resultados/vertice-curado-rendimiento.md) · [Cobertura](resultados/vertice-curado-cobertura.json)

"""
add('REVISION_FINAL.md','## Mediciones de la primera galería editorial',revision)

audit=Path('docs/Auditoria.md')
previous=audit.read_text(encoding='utf-8')
first, separator, history=previous.partition('## Evidencia histórica')
if not separator:
    history=previous.split('\n',2)[2]
summary=f'''# Auditoría VÉRTICE

## Cierre curatorial · última ejecución local

| Requisito | Estado | Evidencia |
| --- | --- | --- |
| Núcleo de comercio | Cumple | Compra {buy['pedido']} agotó la unidad. Edición {edicion['compra']} verificó cupo y un solo comprobante. Lote {flows['lote']} adjudicado y pagado. |
| Mínimo tres microservicios | Cumple | Auth, Catálogo, Notificaciones y Subastas con OpenAPI y health. |
| Base de datos | Cumple | PostgreSQL conserva {datos['tecnologiaArchivada']} SKUs tecnológicos archivados, 18 lotes curados y tres ediciones. [Datos](../resultados/vertice-curado-datos.json) |
| Notificaciones | Cumple | Mailpit recibió comprobantes y avisos de puja. RabbitMQ separa eventos y entrega. El correo externo requiere credenciales. |
| Cinco APIs propias por servicio | Cumple | [Reglas y endpoints](../README.md#cinco-apis-no-crud-por-microservicio). |
| Cobertura unitaria de 80 % por servicio | No cumple | Unidad {unit} %. Con integración {combo} %. Orden Auth, Catálogo, Notificaciones y Subastas. |
| Dos APIs externas | Cumple | Frankfurter, Nager.Date y Open-Meteo devolvieron datos mediante las APIs de VÉRTICE. [Respuestas](../resultados/vertice-editorial-integraciones.json) |
| Despliegue completo | Parcial | Windows nativo operativo. Compose y Blueprint validados. Publicación y pruebas en la nube pendientes. |
| Carga y estrés | Cumple | 52 rutas con k6 por perfil. Newman comprobó {nm['peticiones']} peticiones. |
| Máximo de 500 ms por API | No cumple | Máximos con 3, 6 y 8 VU {maximos} ms. Newman {fmt(nm['maximoMs'])} ms. |
| Artículo IEEE de máximo cuatro páginas | Cumple | [Artículo corto](../output/pdf/editorial/articulo-ieee-4p.pdf) y [fuente IEEEtran](articulo/articulo-ieee-4p.tex). |

[Rendimiento por ruta](../resultados/vertice-curado-rendimiento.md) · [JaCoCo](../resultados/vertice-curado-cobertura.json) · [Compra](../resultados/vertice-editorial-compra.json) · [Edición](../resultados/vertice-curado-e2e.json) · [Subasta](../resultados/vertice-editorial-puja.json) · [Capturas](capturas/)

## Evidencia histórica
'''
audit.write_text(summary+history,encoding='utf-8')

from pathlib import Path
from xml.etree import ElementTree
import json

from generar_techstore_grafica import grafica

root = Path(__file__).resolve().parent.parent
resultados = root / 'resultados'
articulo = root / 'docs' / 'articulo'
perfiles = ('carga', 'estres-6vu', 'estres')
modulos = (('Auth','auth'), ('Catálogo','catalogo'), ('Notificaciones','notificaciones'))


def leer(ruta):
    return json.loads(ruta.read_text(encoding='utf-8-sig'))


def n(valor):
    return f'{valor:.2f}'.replace('.', ',')


def guardar(ruta, contenido):
    ruta.write_text(contenido.strip() + '\n', encoding='utf-8')


def cobertura():
    reporte = []
    for servicio, ruta in modulos:
        t = root / ruta / 'target'
        j = ElementTree.parse(t / 'site/jacoco/jacoco.xml').find("counter[@type='LINE']")
        c, m = int(j.get('covered')), int(j.get('missed'))
        suites = [ElementTree.parse(xml).getroot() for xml in (t/'surefire-reports').glob('TEST-*.xml')]
        assert suites and all(s.get('failures') == s.get('errors') == '0' for s in suites)
        reporte.append(dict(servicio=servicio, cubiertas=c, total=c+m, porcentaje=100*c/(c+m),
                            pruebas=sum(int(s.get('tests')) for s in suites)))
    return reporte


def main():
    coh = cobertura()
    corridas = [leer(resultados/(p+'-techstore.json')) for p in perfiles]
    e2e = leer(resultados/'e2e-techstore.json')
    pm = leer(resultados/'postman-local.json')
    pm_ini = leer(resultados/'postman-revision-inicial.json')
    assert all(len(p['endpoints']) == 36 and all(e['peticiones'] > 0 for e in p['endpoints']) for p in corridas)
    guardar(resultados/'cobertura-techstore.json',json.dumps(coh,ensure_ascii=False,indent=2))
    estadistica = ['| Usuarios | Promedio ms | p95 ms | p99 ms | Máximo ms | Errores | Peticiones | req/s |',
                   '| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |']
    excepciones, rutas_tex = [], []
    informe = ['# k6 · TechStore por ruta','', 'Duraciones en milisegundos. El calentamiento y la limpieza no cuentan.','']
    for etiqueta, p in zip(perfiles,corridas):
        vus = str(p['vusMaximos'])
        estadistica.append('| '+' | '.join([vus,*(n(p[k]) for k in ('avg','p(95)','p(99)','max')),
                                             str(p['errores']),str(p['peticiones']),n(p['rps'])])+' |')
        slow = [e for e in p['endpoints'] if e['max']>500]
        clave = next(e for e in p['endpoints'] if e['nombre']=='auth_cambiar')
        detalle = ', '.join(f"{e['metodo']} {e['ruta']} ({n(e['max'])} ms)" for e in slow) or 'ninguna'
        excepciones.append(f"- **{vus} usuarios.** Rutas por encima de 500 ms: {detalle}. Cambio de contraseña: máximo {n(clave['max'])} ms y p95 {n(clave['p(95)'])} ms.")
        rutas_tex.append(rf'\textbf{{{vus} usuarios.}} Cambio de contraseña: máximo {n(clave["max"])} ms y p95 {n(clave["p(95)"])} ms. '
                         + ('El perfil superó el umbral en ' + ', '.join(r'\texttt{'+e['nombre'].replace('_',r'\_')+'}' for e in slow)+'.' if slow else 'Las 36 rutas quedaron dentro del límite durante esta corrida.'))
        informe.extend([f'## {vus} usuarios','',f'[JSON de k6]({etiqueta}-techstore.json)','',
                        '| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |',
                        '| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |'])
        for e in p['endpoints']:
            celdas = [e['nombre'],e['metodo']+' '+e['ruta'],*(n(e[k]) for k in ('avg','p(95)','p(99)','max')),
                      str(e['errores']),str(e['peticiones']),n(e['rps'])]
            informe.append('| '+' | '.join(celdas)+' |')
        informe.append('')
    guardar(resultados/'latencias-techstore.md','\n'.join(informe))
    cobertura_texto=', '.join(f"{m['servicio']} {n(m['porcentaje'])} %" for m in coh)
    total=sum(m['pruebas'] for m in coh)
    compra=f"El pedido #{e2e['pedido']} de {e2e['producto']} por {e2e['totalCop']:,.0f} COP se confirmó en el navegador y se encontró el correo correspondiente en Mailpit.".replace(',', '.')
    newman=f"Newman ejecutó {pm['peticiones']} peticiones y {pm['aserciones']} aserciones. Hubo {pm['asercionesFallidas']} aserciones fallidas en la corrida de revisión, con máximo {n(pm['maximoMs'])} ms. En la primera corrida de la revisión hubo {pm_ini['asercionesFallidas']} fallos de tiempo y un máximo de {n(pm_ini['maximoMs'])} ms."
    resumen='\n'.join([f"Pasaron **{total} pruebas Java** y **12 React**. JaCoCo de líneas en la suite Java completa: **{cobertura_texto}**.", '',
                       compra, '', newman, '', *estadistica, '', *excepciones])
    r = root/'README.md'
    inicio,fin='<!-- RESULTADOS TECHSTORE -->','<!-- FIN RESULTADOS TECHSTORE -->'
    antes,despues=r.read_text(encoding='utf-8').split(inicio,1)
    if fin in despues:
        despues=despues.split(fin,1)[1]
    guardar(r,antes+inicio+'\n'+resumen+'\n'+fin+despues)

    auditoria=f'''# Auditoría TechStore

Mediciones locales del 22 de noviembre de 2026. Los resultados anteriores se conservan como evidencia histórica.

| Requisito del profesor | Estado | Evidencia |
| --- | --- | --- |
| Core e-commerce | Cumple | Registro, inventario, carrito, pedido {e2e['pedido']} e historial. |
| Tres microservicios | Cumple | Auth, Catálogo y Notificaciones con HTTP y Swagger independientes. |
| Base de datos | Cumple | PostgreSQL. 45 productos en 11 categorías, todos con imagen y descripción. [Verificación](../resultados/pgadmin-local.txt) |
| Notificaciones | Cumple local | RabbitMQ, Mailpit y SMS simulado. Confirmación recibida para el pedido {e2e['pedido']}. |
| Cinco APIs no CRUD por servicio | Cumple | [Rutas y lógica](../README.md#cinco-apis-no-crud-por-microservicio) |
| 80 % por microservicio | Cumple en suite completa | {cobertura_texto}. La suite incluye pruebas unitarias e integración. |
| Dos APIs externas | Cumple | Frankfurter y Nager.Date respondieron en Newman y en el calentamiento de k6. |
| Despliegue completo | Cumple local | Windows activo con PostgreSQL, RabbitMQ y Mailpit. Blueprint Render preparado sin publicación. |
| Carga y estrés | Cumple | 36 rutas con 3, 6 y 8 usuarios. [Desglose](../resultados/latencias-techstore.md) |
| Máximo de 500 ms | No cumple en todos los perfiles | Mayor máximo observado en la revisión: {n(max(p['max'] for p in corridas))} ms. Véase el detalle por ruta. |
| Artículo IEEE | Cumple alcance actualizado | Versión en IEEEtran de dos columnas con métricas nuevas, diagramas y evidencia local. [PDF](../output/pdf/articulo-ieee.pdf) |

## Resultados

{resumen}

Argon2id conserva 19 MiB, dos iteraciones y paralelismo uno. Auth verifica la credencial previa y deriva la nueva fuera de la transacción de escritura. Las muestras HTTP muestran el costo conjunto y la cola en concurrencia. La instrumentación no atribuye por separado los máximos a CPU, GC o PostgreSQL.

Render es opcional para la entrega local. Para obtener un enlace público hace falta conectar el repositorio en tu cuenta, crear RabbitMQ en CloudAMQP y añadir un remitente y credenciales de Brevo. [Pasos](Render.md)
'''
    guardar(root/'docs/Auditoria.md',auditoria)

    tab_cob='\n'.join(f"{d['servicio']} & {d['pruebas']} & {d['cubiertas']}/{d['total']} & {n(d['porcentaje'])}\\% \\\\" for d in coh)
    tab_k6='\n'.join(' & '.join([str(p['vusMaximos']),*(n(p[k]) for k in ('avg','p(95)','p(99)','max')),str(p['errores']),n(p['rps'])])+r' \\' for p in corridas)
    tex=r'''
\newcommand{\ResultadosCobertura}{
\begin{table}[!ht]
\caption{Cobertura Java de la suite completa}
\centering\footnotesize
\begin{tabular}{lrrr}
\toprule
Servicio & Pruebas & Líneas & Cobertura \\\midrule
%%COB%%
\bottomrule
\end{tabular}
\end{table}
Pasaron %%TOTAL%% pruebas Java y 12 pruebas React.}
\newcommand{\ResultadosCarga}{
\begin{table}[!ht]
\caption{Resultados globales k6. Tiempos en ms}
\label{tab:perfiles}
\centering\scriptsize
\setlength{\tabcolsep}{3pt}
\begin{tabular}{rrrrrrr}
\toprule
VU & Media & p95 & p99 & Máx. & Err. & req/s \\\midrule
%%K6%%
\bottomrule
\end{tabular}
\end{table}}
\newcommand{\ResultadoCompra}{%%COMPRA%%}
\newcommand{\ResultadoPostman}{%%POSTMAN%%}
\newcommand{\ResultadoRutas}{%%RUTAS%%}
'''
    for marca,valor in [('COB',tab_cob),('TOTAL',str(total)),('K6',tab_k6),('COMPRA',compra.replace('#',r'\#')),('POSTMAN',newman),('RUTAS','\n\n'.join(rutas_tex))]:
        tex=tex.replace('%%'+marca+'%%',valor)
    guardar(articulo/'metricas-techstore.tex',tex)
    grafica(corridas, articulo/'latencia-techstore.png')


if __name__ == '__main__':
    main()


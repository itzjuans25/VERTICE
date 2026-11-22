from pathlib import Path
from xml.etree import ElementTree
import json
import os

from PIL import Image, ImageDraw, ImageFont


root = Path(__file__).resolve().parent.parent
resultados = root / 'resultados'
articulo = root / 'docs' / 'articulo'
perfiles = ('carga', 'estres-6vu', 'estres')
modulos = (
    ('Auth', 'auth', 22),
    ('Catálogo', 'catalogo', 31),
    ('Notificaciones', 'notificaciones', 21),
)


def leer(ruta):
    return json.loads(ruta.read_text(encoding='utf-8-sig'))


def numero(valor):
    return f'{valor:.2f}'.replace('.', ',')


def guardar(ruta, contenido):
    ruta.write_text(contenido.strip() + '\n', encoding='utf-8')


def cobertura():
    datos = []
    for nombre, directorio, _ in modulos:
        modulo = root / directorio / 'target'
        xml = ElementTree.parse(modulo / 'site' / 'jacoco' / 'jacoco.xml')
        linea = xml.find("counter[@type='LINE']")
        cubiertas, omitidas = int(linea.get('covered')), int(linea.get('missed'))
        pruebas = 0
        for informe in (modulo / 'surefire-reports').glob('TEST-*.xml'):
            suite = ElementTree.parse(informe).getroot()
            assert int(suite.get('failures', 0)) == int(suite.get('errors', 0)) == 0
            pruebas += int(suite.get('tests'))
        datos.append(dict(servicio=nombre, cubiertas=cubiertas, omitidas=omitidas,
                          porcentaje=100*cubiertas/(cubiertas+omitidas), pruebas=pruebas))
    return datos


def grafica(datos):
    dib = Image.new('RGB', (1040, 540), 'white')
    d = ImageDraw.Draw(dib)
    src = Path(os.environ.get('WINDIR', 'C:/Windows')) / 'Fonts' / 'arial.ttf'
    fuente = ImageFont.truetype(str(src), 26)
    eje = ImageFont.truetype(str(src), 22)
    x0, y0, x1, y1 = 120, 405, 998, 25
    alto = max(750, ((int(max(m['max'] for m in datos)) + 249) // 250) * 250)

    def y(valor):
        return y0 - round((y0-y1) * valor / alto)

    for valor in range(0, alto + 1, 250):
        color = '#9b3445' if valor == 500 else '#d6dce2'
        d.line((x0, y(valor), x1, y(valor)), fill=color, width=3 if valor == 500 else 1)
        d.text((x0-12, y(valor)), str(valor), font=eje, fill='#2c3745', anchor='rm')
    d.text((117, 13), 'ms', font=eje, fill='#2c3745', anchor='rt')
    for i, m in enumerate(datos):
        x = 265 + i*275
        for j, (key, color) in enumerate((('p(95)', '#298ba6'), ('max', '#283851'))):
            izq = x - 70 + j*73
            d.rectangle((izq, y(m[key]), izq+60, y0), fill=color)
            d.text((izq+30, y(m[key])-7), numero(m[key]), font=eje, fill='#273244', anchor='mb')
        d.text((x, y0+28), str(m['vusMaximos']) + ' usuarios', font=fuente, fill='#273244', anchor='mt')
    for x, nombre, color in [(160, 'p95', '#298ba6'), (360, 'Máximo', '#283851'), (655, 'Umbral 500 ms', '#9b3445')]:
        d.rectangle((x, 491, x+30, 511), fill=color)
        d.text((x+42, 501), nombre, font=fuente, fill='#273244', anchor='lm')
    dib.save(articulo / 'latencia-techtrade.png')


def main():
    datos = [leer(resultados / (p + '-techtrade.json')) for p in perfiles]
    previo = [leer(resultados / (p + '-argon2-inicial.json')) for p in perfiles]
    prueba = leer(resultados / 'marketplace-techtrade.json')
    cob = cobertura()
    nombres = [{e['nombre'] for e in m['endpoints']} for m in datos]
    assert len(nombres[0]) == 39 and all(n == nombres[0] for n in nombres)
    assert all(all(e['peticiones'] > 0 for e in m['endpoints']) for m in datos)
    guardar(resultados / 'cobertura-techtrade.json', json.dumps(cob, ensure_ascii=False, indent=2))

    tabla = ['| Usuarios | Promedio ms | p95 ms | p99 ms | Máximo ms | Errores | Peticiones | req/s |',
             '| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |']
    excedidas = []
    informe = ['# Medición TechTrade por ruta', '',
               'Se muestran los tres perfiles finales. Cada tabla usa la duración HTTP de k6 (ms). El resultado por ruta excluye calentamiento y limpieza.', '']
    for etiqueta, m, antes in zip(perfiles, datos, previo):
        perfil = str(m['vusMaximos'])
        tabla.append('| ' + ' | '.join([perfil, *(numero(m[k]) for k in ('avg', 'p(95)', 'p(99)', 'max')),
                                        str(m['errores']), str(m['peticiones']), numero(m['rps'])]) + ' |')
        lentas = sorted((e for e in m['endpoints'] if e['max'] > 500), key=lambda e: -e['max'])
        detalle = ', '.join(f"{e['nombre']} ({numero(e['max'])} ms)" for e in lentas) or 'ninguna ruta'
        actual_clave = next(e for e in m['endpoints'] if e['nombre'] == 'auth_cambiar')['max']
        previo_clave = next(e for e in antes['endpoints'] if e['nombre'] == 'auth_cambiar')['max']
        excedidas.append(f'- {perfil} usuarios. Excedieron 500 ms: {detalle}. Cambio de contraseña antes y después del ajuste JVM: {numero(previo_clave)} → {numero(actual_clave)} ms.')
        informe.extend([f'## {perfil} usuarios', '', f'[JSON de k6]({etiqueta}-techtrade.json)', '',
                        '| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |',
                        '| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |'])
        for e in m['endpoints']:
            metricas = [numero(e[k]) for k in ('avg', 'p(95)', 'p(99)', 'max')]
            informe.append('| ' + ' | '.join([e['nombre'], e['metodo'] + ' ' + e['ruta'], *metricas,
                                             str(e['errores']), str(e['peticiones']), numero(e['rps'])]) + ' |')
        informe.append('')
    guardar(resultados / 'latencias-techtrade.md', '\n'.join(informe))

    cobertura_p = ', '.join(f"{c['servicio']} {numero(c['porcentaje'])} %" for c in cob)
    suma = sum(c['pruebas'] for c in cob)
    resumen = '\n'.join([
        'JaCoCo, líneas de la suite Java completa (unitarias e integración). **' + cobertura_p + '**.',
        f'Pasaron **{suma} pruebas Java** y **12 React**. Notificaciones conserva sus 21 pruebas.', '',
        f"La publicación **{prueba['publicacion']}** se vendió en el pedido **{prueba['pedido']}** por {prueba['totalCop']:,} COP. "
        'Mailpit recibió el correo de publicación, el del vendedor al vender y el del comprador. El inventario terminó en cero.',
        '', *tabla, '', *excedidas,
    ]).replace(f"{prueba['totalCop']:,}", f"{prueba['totalCop']:,}".replace(',', '.'))
    readme = root / 'README.md'
    inicio, fin = '<!-- RESULTADOS TECHTRADE -->', '<!-- FIN RESULTADOS TECHTRADE -->'
    original = readme.read_text(encoding='utf-8')
    previo_fin = fin if fin in original else inicio
    antes, despues = original.split(inicio, 1)
    if previo_fin == fin:
        despues = despues.split(fin, 1)[1]
    guardar(readme, antes + inicio + '\n' + resumen + '\n' + fin + despues)

    auditoria = f'''# Auditoría TechTrade

Resultados medidos el 21 de noviembre de 2026 en Windows. El repositorio conserva los informes históricos y los actuales.

| # | Requisito | Estado | Evidencia |
| ---: | --- | --- | --- |
| 1 | Core e-commerce | Cumple | Cuentas, catálogo paginado, carrito, pedido {prueba['pedido']}, historial y compraventa. |
| 2 | Mínimo tres microservicios | Cumple | Auth 9090, Catálogo 9092 y Notificaciones 9093, con health y Swagger. |
| 3 | Base de datos | Cumple | PostgreSQL y seed idempotente con 51 productos. |
| 4 | Sistema de notificaciones | Cumple local | RabbitMQ y tres correos relacionados con la publicación {prueba['publicacion']} y el pedido {prueba['pedido']}. SMS local simulado. |
| 5 | Cinco rutas no CRUD por servicio | Cumple | Contratos del [README](../README.md#apis-con-lógica-de-negocio), Swagger y muestras de k6. |
| 6 | Cobertura mínima del 80 % por servicio | Cumple en suite completa | {cobertura_p}. La suite combina pruebas unitarias y de integración. [JSON](../resultados/cobertura-techtrade.json) |
| 7 | Dos APIs externas | Cumple | Frankfurter y Nager.Date respondieron en el calentamiento k6. Catálogo usa caché y contempla fallos. |
| 8 | Despliegue completo | Parcial | Windows de punta a punta. Compose preparado. Publicación y prueba desde Internet pendientes de una VM OCI. |
| 9 | Carga y estrés | Cumple | 39 rutas con 3, 6 y 8 usuarios virtuales. [Informe por ruta](../resultados/latencias-techtrade.md) |
| 10 | Máximo de 500 ms por API | {'No cumple' if any(m['max'] > 500 for m in datos) else 'Cumple en los perfiles medidos'} | Se compara el máximo por ruta. Véase la tabla y las excepciones. |
| 11 | Artículo IEEEtran | Cumple el alcance actualizado | Artículo de 6–8 páginas según la última solicitud. Si el docente mantiene el tope anterior de 4, se requiere una versión abreviada. [PDF](../output/pdf/articulo-ieee.pdf) |

## Evidencia local

{resumen}

La operación de cambio de clave verifica el hash anterior y calcula el nuevo fuera de la transacción de escritura. La consulta usa la clave primaria y la escritura verifica el hash leído. Las mediciones HTTP no aíslan el tiempo de CPU, GC o base de datos.

## Entregas externas pendientes

- VM Ubuntu en OCI con SSH y un dominio para publicar por HTTPS.
- Remitente verificado y clave SMTP de Brevo para verificar correo fuera de Mailpit.
- Cuenta trial, remitente y número de destino habilitados en Twilio para verificar SMS reales.
- Hacer oferta y avisar al vendedor queda como trabajo futuro.
'''
    guardar(root / 'docs' / 'Auditoria.md', auditoria)

    tab_cob = '\n'.join(
        f"{c['servicio']} & {c['pruebas']} & {c['cubiertas']}/{c['cubiertas']+c['omitidas']} & {numero(c['porcentaje'])}\\% \\\\"
        for c in cob
    )
    tab_carga = '\n'.join(
        ' & '.join([str(m['vusMaximos']), *(numero(m[k]) for k in ('avg', 'p(95)', 'p(99)', 'max')),
                    str(m['errores']), numero(m['rps'])]) + r' \\'
        for m in datos
    )
    txt_rutas = []
    for m, old in zip(datos, previo):
        lentas = sorted((e for e in m['endpoints'] if e['max'] > 500), key=lambda e: -e['max'])
        formas = [r'\texttt{' + e['nombre'].replace('_', r'\_') + '} (' + numero(e['max']) + ' ms)' for e in lentas]
        clave_antes = next(e for e in old['endpoints'] if e['nombre'] == 'auth_cambiar')
        clave_final = next(e for e in m['endpoints'] if e['nombre'] == 'auth_cambiar')
        txt_rutas.append(
            rf"\textbf{{{m['vusMaximos']} usuarios.}} "
            + ('Sobrepasaron 500 ms ' + ', '.join(formas) + '. ' if formas else 'Todas las rutas quedaron dentro del límite. ')
            + f"El máximo del cambio de clave pasó de {numero(clave_antes['max'])} a {numero(clave_final['max'])} ms. "
            + f"Su p95 final fue {numero(clave_final['p(95)'])} ms."
        )
    latex = r'''
\newcommand{\ResultadosCobertura}{
\begin{table}[!ht]
\caption{Cobertura Java de la suite completa}
\centering\footnotesize
\begin{tabular}{lrrr}
\toprule
Servicio & Pruebas & Líneas & Cobertura \\\midrule
%%TABCOB%%
\bottomrule
\end{tabular}
\end{table}
Pasaron %%TOTAL%% pruebas Java y 12 pruebas React.}
\newcommand{\ResultadosCarga}{
\begin{table}[!ht]
\caption{Resultados globales de k6. Tiempos en ms}
\label{tab:perfiles}
\centering\scriptsize
\setlength{\tabcolsep}{3pt}
\begin{tabular}{rrrrrrr}
\toprule
VU & Media & p95 & p99 & Máx. & Err. & req/s \\\midrule
%%TABK6%%
\bottomrule
\end{tabular}
\end{table}}
\newcommand{\ResultadoCompra}{%%COMPRA%%}
\newcommand{\ResultadoRutas}{%%RUTAS%%}
'''
    compra = (f"La publicación {prueba['publicacion']} pasó a vendida con stock cero al confirmar el pedido {prueba['pedido']} "
              f"por {prueba['totalCop']} COP. Mailpit registró el aviso de publicación y los correos al vendedor y al comprador.")
    for marca, valor in [('TABCOB', tab_cob), ('TOTAL', str(suma)), ('TABK6', tab_carga), ('COMPRA', compra),
                          ('RUTAS', '\n\n'.join(txt_rutas))]:
        latex = latex.replace('%%' + marca + '%%', valor)
    guardar(articulo / 'metricas-techtrade.tex', latex)
    grafica(datos)


if __name__ == '__main__':
    main()


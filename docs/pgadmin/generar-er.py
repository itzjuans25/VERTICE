from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
import math
import os

root=Path(__file__).resolve().parent
s=json.loads((root/'esquema-local.json').read_text(encoding='utf-8'))
columnas={}
for col in s['tablas']:
    columnas.setdefault(col['tabla'], set()).add(col['columna'])
seleccion={
    'usuario': ['id', 'email', 'role'],
    'products': ['id', 'sku_demo', 'condicion', 'grado', 'stock', 'especificaciones'],
    'carrito': ['id', 'usuario_id'],
    'compras': ['id', 'usuario_id', 'estado', 'total'],
    'item_carrito': ['id', 'id_carrito', 'compra_id', 'producto_id', 'cantidad', 'precio_unitario'],
    'sms_simulado': ['id', 'destinatario', 'creado_en'],
    'notificaciones_pendientes': ['id', 'intentos', 'siguiente_intento'],
}
coords={
    'usuario': (35,52,290,182),
    'products': (35,237,290,445),
    'carrito': (420,40,670,150),
    'compras': (420,182,670,342),
    'item_carrito': (820,96,1105,310),
    'sms_simulado':(420,399,670,527),
    'notificaciones_pendientes':(820,399,1125,527)
}
canvas=Image.new('RGB',(1160,585),'#ffffff')
d=ImageDraw.Draw(canvas)
fuentes=Path(os.environ.get('WINDIR','C:/Windows'))/'Fonts'
normal=ImageFont.truetype(str(fuentes/'arial.ttf'),22)
bold=ImageFont.truetype(str(fuentes/'arialbd.ttf'),22)
small=ImageFont.truetype(str(fuentes/'arial.ttf'),18)

def linea(puntos, color, discontinua=False):
    for a,b in zip(puntos,puntos[1:]):
        if discontinua:
            longitud=math.dist(a,b)
            for i in range(0,round(longitud),14):
                j=min(i+8,longitud)
                d.line((tuple(a[k]+(b[k]-a[k])*i/longitud for k in range(2)),
                        tuple(a[k]+(b[k]-a[k])*j/longitud for k in range(2))),fill=color,width=3)
        else:
            d.line((a,b),fill=color,width=3)
    a,b=puntos[-2:]
    ang=math.atan2(b[1]-a[1],b[0]-a[0])
    d.polygon((b, tuple(b[i]-12*(math.cos(ang-.5) if i==0 else math.sin(ang-.5)) for i in range(2)),
                 tuple(b[i]-12*(math.cos(ang+.5) if i==0 else math.sin(ang+.5)) for i in range(2))),fill=color)

links={
    ('item_carrito','carrito'): [(820,120),(743,120),(743,95),(670,95)],
    ('item_carrito','compras'): [(820,213),(738,213),(738,254),(670,254)]
}
for fk in s['fk']:
    par=fk['origen'],fk['destino']
    if par in links:
        linea(links[par],'#255c7a')
linea([(420,91),(290,91)],'#605d7a',True)
linea([(420,238),(335,238),(335,148),(290,148)],'#605d7a',True)
producto_col=next((col for col in ('producto_id','id_producto') if col in columnas['item_carrito']),None)
if producto_col:
    seleccion['item_carrito'][3]=producto_col
    linea([(820,287),(775,287),(775,372),(290,372)],'#605d7a',True)

for tabla,rect in coords.items():
    x,y,r,b=rect
    d.rounded_rectangle(rect,8,fill='#edf5fa',outline='#426c85',width=2)
    d.rounded_rectangle((x,y,r,y+39),7,fill='#13445c')
    d.text((x+12,y+7),tabla,font=bold,fill='#fff')
    for idx, col in enumerate(c for c in seleccion[tabla] if c in columnas[tabla]):
        d.text((x+13,y+47+idx*26),col+(' (PK)' if col=='id' else ''),font=normal,fill='#183745')
d.text((44,552),'Línea continua: clave foránea en PostgreSQL · Línea discontinua: referencia lógica de la aplicación',font=small,fill='#274354')
canvas.save(root/'er-techstore.png')


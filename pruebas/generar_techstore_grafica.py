import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


def numero(valor):
    return f'{valor:.2f}'.replace('.', ',')


def grafica(datos, destino):
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
    dib.save(destino)


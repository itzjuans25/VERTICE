from argparse import ArgumentParser
from hashlib import sha256
from pathlib import Path
import json
import random

from PIL import Image, ImageEnhance, ImageFilter, ImageOps


MERCADO = Path("frontend/public/mercado")
EDICIONES = ("edicion-sal", "edicion-aureola", "edicion-pliegue")


def preparar(image, size, focus=(0.5, 0.5)):
    perfil = ImageOps.fit(ImageOps.exif_transpose(image).convert("RGB"), size, Image.Resampling.LANCZOS, centering=focus)
    perfil = ImageEnhance.Color(perfil).enhance(0.94)
    perfil = ImageEnhance.Contrast(perfil).enhance(1.015)
    rng = random.Random(82)
    grano = Image.frombytes("L", (160, 160), bytes(rng.getrandbits(8) for _ in range(160 * 160)))
    grano = grano.resize(size, Image.Resampling.BILINEAR).convert("RGB")
    perfil = Image.blend(perfil, grano, 0.014)
    vineta = Image.radial_gradient("L").resize(size, Image.Resampling.BILINEAR).point(lambda px: int(px * 0.025))
    perfil = Image.composite(Image.new("RGB", size, (35, 30, 24)), perfil, vineta)
    return perfil.filter(ImageFilter.UnsharpMask(radius=0.9, percent=32, threshold=3))


def guardar(imagen, nombre):
    path = MERCADO / (nombre + ".webp")
    imagen.save(path, "WEBP", quality=82, method=6)
    return {"archivo": path.name, "sha256": sha256(path.read_bytes()).hexdigest(), "dimensiones": list(imagen.size)}


def main():
    parser = ArgumentParser(description="Prepara imágenes editoriales locales y elimina metadatos.")
    parser.add_argument("--contacto", type=Path, help="Contacto de origen para las tres ediciones.")
    args = parser.parse_args()
    informe = []
    for numero in range(1, 19):
        nombre = f"vertice-{numero:02}"
        with Image.open(MERCADO / (nombre + ".webp")) as original:
            grande = preparar(original, (1600, 1200))
            informe.append(guardar(grande, nombre + "-editorial"))
            informe.append(guardar(grande.resize((600, 450), Image.Resampling.LANCZOS), nombre + "-mini"))
            w, h = original.size
            detalle = original.crop((round(w * 0.21), round(h * 0.17), round(w * 0.79), round(h * 0.77)))
            informe.append(guardar(preparar(detalle, (1600, 1200)), nombre + "-detalle"))

    if args.contacto:
        with Image.open(args.contacto) as contacto:
            w, h = contacto.size
            for i, nombre in enumerate(EDICIONES):
                toma = contacto.crop((w * i // 3 + 3, 3, w * (i + 1) // 3 - 3, h - 3))
                toma.save(MERCADO / (nombre + "-fuente.webp"), "WEBP", lossless=True)
    for nombre in EDICIONES:
        with Image.open(MERCADO / (nombre + "-fuente.webp")) as toma:
            grande = preparar(toma, (1280, 1600))
            informe.append(guardar(grande, nombre))
            informe.append(guardar(grande.resize((480, 600), Image.Resampling.LANCZOS), nombre + "-mini"))
    assert len({registro["sha256"] for registro in informe}) == len(informe), "Imágenes derivadas duplicadas"
    Path("resultados/vertice-editorial-imagenes.json").write_text(
        json.dumps({"calidadWebP": 82, "derivadas": informe, "duplicadas": 0}, indent=2) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()

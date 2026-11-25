import json
from pathlib import Path

raiz = Path("resultados")
filas = []
desglose = []
for usuarios in (3, 6, 8):
    ruta = raiz / f"remata-k6-{usuarios}.json"
    if not ruta.is_file():
        continue
    perfil = json.loads(ruta.read_text(encoding="utf-8-sig"))
    if not perfil.get("peticiones"):
        desglose.append(f"- {usuarios} VU sin muestras válidas.")
        continue
    medidos = [r for r in perfil.get("endpoints", []) if r.get("peticiones", 0) > 0]
    filas.append((usuarios, len(medidos), perfil))
    for r in medidos:
        if max(r.get("max", 0), r.get("p(95)", 0)) > 500:
            desglose.append(f"- {usuarios} VU · \`{r['nombre']}\` · p95 {r['p(95)']:.2f} ms · máximo {r['max']:.2f} ms.".replace(".", ",").removesuffix(",")+".")
cabecera = [
    "# Rendimiento local de Remata", "",
    "k6 ejecuta un recorrido preliminar sin métricas de carga. Los JSON conservan estadísticas por ruta.",
    "La primera petición fría de un servicio de nube sigue sin medición.",
    "", "| VU | Rutas | Media ms | p95 ms | p99 ms | Máximo ms | Errores | req/s |",
    "| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |",
]
for u, total, p in filas:
    med = [f"{p[k]:.2f}".replace(".", ",") for k in ("avg","p(95)","p(99)","max")]
    rpm = f"{p['rps']:.2f}".replace(".", ",")
    cabecera.append(f"| {u} | {total} | " + " | ".join(med) + f" | {p['errores']} | {rpm} |")
cabecera += ["", "## Endpoints sobre 500 ms", "", *desglose]
(raiz / "remata-rendimiento.md").write_text("\n".join(cabecera) + "\n", encoding="utf-8")

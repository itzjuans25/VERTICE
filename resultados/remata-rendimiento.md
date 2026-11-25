# Rendimiento local de Remata

k6 ejecuta un recorrido preliminar sin métricas de carga. Los JSON conservan estadísticas por ruta.
La primera petición fría de un servicio de nube sigue sin medición.

| VU | Rutas | Media ms | p95 ms | p99 ms | Máximo ms | Errores | req/s |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3 | 49 | 35,71 | 141,15 | 347,62 | 1390,95 | 0 | 51,28 |
| 6 | 49 | 24,27 | 122,80 | 246,14 | 405,71 | 0 | 115,19 |
| 8 | 49 | 28,47 | 143,09 | 302,06 | 970,74 | 0 | 127,93 |

## Endpoints sobre 500 ms

- 3 VU · \`cat_carrito\` · p95 591,40 ms · máximo 730,63 ms.
- 3 VU · \`cat_compras\` · p95 513,09 ms · máximo 542,15 ms.
- 3 VU · \`notif_validar\` · p95 1127,08 ms · máximo 1390,95 ms.
- 8 VU · \`auth_registrar\` · p95 299,57 ms · máximo 970,74 ms.
- 8 VU · \`auth_renovar\` · p95 256,40 ms · máximo 696,59 ms.
- 8 VU · \`auth_cambiar\` · p95 478,50 ms · máximo 860,25 ms.
- 8 VU · \`cat_agregar\` · p95 59,32 ms · máximo 685,05 ms.

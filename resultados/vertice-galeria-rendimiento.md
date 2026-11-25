# Rendimiento de la galería VÉRTICE

k6 abrió sesiones y comprobó 50 rutas en cada perfil con los servicios activos. La métrica de errores y el máximo de 500 ms se evalúan por separado.

| VU | Peticiones | req/s | Media ms | p95 ms | p99 ms | Máximo ms | Errores |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 3 | 1836 | 65,26 | 29,12 | 138,07 | 256,15 | 417,65 | 0 |
| 6 | 3060 | 76,57 | 37,10 | 156,71 | 363,16 | 2404,86 | 0 |
| 8 | 2991 | 69,78 | 55,00 | 225,62 | 616,37 | 2494,37 | 6 |

## Rutas por encima de 500 ms

| VU | Servicio | Ruta | p95 ms | Máximo ms |
|---:|---|---|---:|---:|
| 3 | — | Ninguna | — | — |
| 6 | Auth | `POST /save` | 472,89 | 2377,37 |
| 6 | Auth | `PUT /update` | 61,18 | 1530,49 |
| 6 | Auth | `DELETE /:id` | 80,85 | 577,14 |
| 6 | Auth | `POST /renovar` | 236,33 | 2179,05 |
| 6 | Auth | `POST /cambiar-clave` | 552,93 | 2404,86 |
| 6 | Catálogo | `GET /carrito` | 38,03 | 519,61 |
| 6 | Catálogo | `DELETE /carrito/eliminar/:id` | 95,84 | 518,86 |
| 6 | Catálogo | `GET /compras` | 286,83 | 1006,96 |
| 8 | Auth | `POST /save` | 356,57 | 2212,37 |
| 8 | Auth | `DELETE /:id` | 119,99 | 899,67 |
| 8 | Auth | `POST /cambiar-clave` | 1512,01 | 1650,77 |
| 8 | Catálogo | `GET /producto/:id` | 17,67 | 520,13 |
| 8 | Catálogo | `GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc` | 41,15 | 586,02 |
| 8 | Catálogo | `GET /producto/categorias` | 72,63 | 2494,37 |
| 8 | Catálogo | `GET /producto/marcas` | 135,01 | 725,88 |
| 8 | Catálogo | `POST /producto` | 897,34 | 1558,37 |
| 8 | Catálogo | `GET /carrito` | 258,83 | 1152,08 |
| 8 | Catálogo | `POST /carrito/agregar` | 150,87 | 615,02 |
| 8 | Catálogo | `PATCH /compras/:id/estado` | 327,66 | 529,54 |
| 8 | Catálogo | `GET /compras` | 566,35 | 628,54 |

## Resultados por ruta

| Operación | 3 VU p95 / máx. | 6 VU p95 / máx. | 8 VU p95 / máx. |
|---|---:|---:|---:|
| Auth `POST /save` | 245,49 / 326,22 | 472,89 / 2377,37 | 356,57 / 2212,37 |
| Auth `POST /login` | 203,34 / 215,10 | 223,23 / 402,44 | 354,78 / 401,56 |
| Auth `GET /:id` | 12,79 / 21,86 | 19,32 / 95,97 | 36,43 / 65,77 |
| Auth `GET /api/auth/perfil` | 11,98 / 14,16 | 15,67 / 43,77 | 39,46 / 77,77 |
| Auth `GET /:id/existe` | 10,27 / 20,52 | 12,76 / 20,33 | 30,90 / 165,29 |
| Auth `PUT /update` | 23,70 / 41,62 | 61,18 / 1530,49 | 79,20 / 82,04 |
| Auth `DELETE /:id` | 43,82 / 49,93 | 80,85 / 577,14 | 119,99 / 899,67 |
| Auth `GET /sesion` | 20,28 / 28,29 | 20,07 / 213,63 | 31,89 / 61,94 |
| Auth `POST /evaluar-clave` | 14,97 / 21,08 | 20,84 / 48,57 | 31,16 / 71,16 |
| Auth `POST /renovar` | 176,58 / 238,15 | 236,33 / 2179,05 | 361,99 / 420,01 |
| Auth `POST /cambiar-clave` | 379,59 / 417,65 | 552,93 / 2404,86 | 1512,01 / 1650,77 |
| Catálogo `GET /producto` | 24,13 / 28,27 | 23,54 / 29,73 | 23,83 / 163,59 |
| Catálogo `GET /inicio` | 12,07 / 21,28 | 15,14 / 72,85 | 40,81 / 105,05 |
| Catálogo `GET /producto/:id` | 10,96 / 12,84 | 17,30 / 26,93 | 17,67 / 520,13 |
| Catálogo `GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc` | 21,99 / 30,60 | 20,38 / 95,26 | 41,15 / 586,02 |
| Catálogo `GET /producto/categorias` | 5,93 / 14,39 | 11,12 / 27,54 | 72,63 / 2494,37 |
| Catálogo `GET /producto/marcas` | 8,85 / 9,57 | 11,22 / 164,83 | 135,01 / 725,88 |
| Catálogo `POST /producto` | 53,42 / 67,68 | 87,90 / 184,62 | 897,34 / 1558,37 |
| Catálogo `PUT /producto/:id` | 21,37 / 34,65 | 29,54 / 44,62 | 33,88 / 79,29 |
| Catálogo `DELETE /producto/:id` | 22,79 / 58,92 | 38,91 / 52,14 | 127,28 / 197,01 |
| Catálogo `GET /producto/:id/precio-usd` | 17,74 / 46,12 | 17,95 / 27,84 | 16,27 / 74,28 |
| Catálogo `GET /producto/entrega?diasHabiles=3` | 6,76 / 9,11 | 12,31 / 26,48 | 11,18 / 49,92 |
| Catálogo `GET /carrito` | 45,10 / 49,44 | 38,03 / 519,61 | 258,83 / 1152,08 |
| Catálogo `GET /carrito/resumen` | 34,83 / 35,71 | 41,10 / 80,60 | 131,72 / 246,73 |
| Catálogo `POST /carrito/agregar` | 57,05 / 81,64 | 62,31 / 170,14 | 150,87 / 615,02 |
| Catálogo `PUT /carrito/actualizar/:id?cantidad=2` | 58,84 / 111,13 | 80,08 / 275,82 | 112,87 / 130,15 |
| Catálogo `DELETE /carrito/eliminar/:id` | 51,17 / 60,84 | 95,84 / 518,86 | 130,38 / 202,80 |
| Catálogo `DELETE /carrito/vaciar` | 26,87 / 47,00 | 103,09 / 221,19 | 66,11 / 422,59 |
| Catálogo `POST /compras/realizar` | 74,57 / 114,92 | 115,96 / 180,68 | 304,66 / 457,47 |
| Catálogo `GET /compras/usuario/:id` | 63,17 / 193,87 | 31,61 / 44,28 | 55,32 / 86,54 |
| Catálogo `GET /compras/:id` | 19,09 / 25,97 | 30,70 / 66,32 | 28,13 / 100,61 |
| Catálogo `PATCH /compras/:id/estado` | 66,51 / 97,28 | 113,18 / 274,03 | 327,66 / 529,54 |
| Catálogo `GET /compras` | 240,44 / 256,35 | 286,83 / 1006,96 | 566,35 / 628,54 |
| Notificaciones `POST /validar` | 38,17 / 130,35 | 23,26 / 30,57 | 45,93 / 165,44 |
| Notificaciones `GET /api/notificaciones/mis-avisos?tamano=10` | 65,77 / 70,97 | 34,83 / 120,71 | 37,98 / 207,91 |
| Notificaciones `POST /previsualizar/bienvenida` | 13,55 / 62,70 | 15,64 / 37,07 | 13,24 / 30,13 |
| Notificaciones `POST /previsualizar/compra` | 10,51 / 162,40 | 13,13 / 21,95 | 12,50 / 18,66 |
| Notificaciones `GET /plan-reintentos` | 12,98 / 17,78 | 31,36 / 115,20 | 16,95 / 92,58 |
| Notificaciones `POST /encolar` | 18,05 / 25,78 | 52,31 / 75,42 | 91,12 / 280,85 |
| Subastas `POST ` | 36,24 / 64,72 | 47,39 / 52,91 | 108,48 / 244,49 |
| Subastas `GET /api/subastas/inicio` | 18,43 / 44,25 | 20,75 / 77,09 | 100,79 / 276,80 |
| Subastas `GET /api/subastas/lote/:id/precio-usd` | 28,88 / 83,51 | 18,76 / 45,13 | 32,08 / 45,32 |
| Subastas `GET /api/subastas/entrega?diasHabiles=3` | 11,41 / 16,76 | 13,24 / 15,39 | 8,52 / 22,20 |
| Subastas `GET /api/subastas/clima` | 30,94 / 82,60 | 21,20 / 162,80 | 20,27 / 103,11 |
| Subastas `GET ?categoria=HOGAR&minimo=100&maximo=500&tamano=4` | 23,10 / 27,86 | 19,26 / 55,48 | 61,14 / 100,51 |
| Subastas `GET /:id` | 15,51 / 33,50 | 15,41 / 56,94 | 69,26 / 135,56 |
| Subastas `POST /:id/pujas` | 27,92 / 37,30 | 60,15 / 156,97 | 118,70 / 450,56 |
| Subastas `GET /mias` | 20,36 / 58,66 | 19,89 / 24,04 | 29,98 / 119,54 |
| Subastas `POST /:id/pago` | 13,78 / 18,92 | 22,38 / 33,30 | 21,49 / 103,08 |
| Subastas `POST /:id/cancelar` | 25,72 / 43,77 | 35,35 / 49,03 | 106,04 / 260,35 |

## Newman

88 peticiones, 0 fallos HTTP, 263 aserciones y 0 fallos de aserción. Media 58,47 ms. Máximo 266,00 ms.


## Diagnóstico y arranque en frío

Spring Boot registró Auth 31,388 s, Catálogo 33,414 s, Notificaciones 15,932 s, Subastas 16,224 s. Los JAR iniciaron en paralelo. Es tiempo de inicialización, separado de la latencia de solicitudes con los servicios activos. La nube sigue pendiente.

El plan `EXPLAIN (ANALYZE, BUFFERS)` de la consulta de portada tomó 0,278 ms de ejecución en PostgreSQL. Es una muestra aislada de esa consulta, con caché en la base. No atribuye por sí sola los picos HTTP de otras rutas.

| Ruta Auth | Fase | Muestras | Media ms |
|---|---|---:|---:|
| cambiar-clave | consulta | 62 | 3,16 |
| cambiar-clave | escritura | 59 | 33,81 |
| cambiar-clave | espera_hash | 123 | 48,38 |
| cambiar-clave | hash | 120 | 196,59 |
| login | consulta | 66 | 4,20 |
| login | espera_hash | 66 | 14,38 |
| login | hash | 66 | 137,28 |
| renovar | consulta | 62 | 1,97 |
| renovar | espera_hash | 62 | 15,01 |
| renovar | hash | 62 | 141,44 |
| save | consulta | 74 | 5,61 |
| save | escritura | 74 | 6,49 |
| save | espera_hash | 74 | 6,60 |
| save | hash | 74 | 127,37 |

## Errores funcionales en k6

- 8 VU. Auth `POST /login`, 3 errores.
- 8 VU. Auth `POST /cambiar-clave`, 3 errores.

[3 VU](vertice-galeria-k6-3.json) · [6 VU](vertice-galeria-k6-6.json) · [8 VU](vertice-galeria-k6-8.json) · [Fases Auth](vertice-galeria-k6-fases-8.json) · [Newman](vertice-galeria-postman.json) · [Plan PostgreSQL](vertice-galeria-plan.txt)

## Repetición a 8 VU y validez

En la corrida principal, cambiar la contraseña devolvió tres HTTP 503. El código del servicio usa ese estado al agotar su admisión o espera de hash. El guion anterior alternaba la contraseña esperada por número de iteración incluso cuando ese cambio fallaba. Los tres 401 posteriores de login son compatibles con esa secuencia.

Se corrigió el guion. Cada usuario virtual conserva la última clave confirmada por HTTP 204 y mantiene la pausa entre iteraciones fallidas. La repetición posterior terminó con `context deadline exceeded` al cabo de 398,41 s. Registró 1016 peticiones, diez errores y un máximo de 8245,25 ms. Su duración quedó fuera del perfil de 36 s más periodos de gracia. Se conserva como incidencia y no reemplaza la corrida principal.

[Salida de la repetición](vertice-galeria-k6-8-repeticion.json) · [Fases de la repetición](vertice-galeria-k6-fases-8-repeticion.json)

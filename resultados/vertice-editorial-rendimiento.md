# Rendimiento de la galería editorial VÉRTICE

k6 abrió sesiones y comprobó 54 rutas en cada perfil con los servicios activos. La métrica de errores y el máximo de 500 ms se evalúan por separado.

| VU | Peticiones | req/s | Media ms | p95 ms | p99 ms | Máximo ms | Errores |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 3 | 1320 | 41,80 | 43,52 | 194,10 | 443,14 | 1139,82 | 0 |
| 6 | 3080 | 75,93 | 38,83 | 188,28 | 377,23 | 1359,34 | 0 |
| 8 | 3356 | 86,91 | 46,79 | 237,87 | 572,98 | 1270,45 | 1 |

## Rutas por encima de 500 ms

| VU | Servicio | Ruta | p95 ms | Máximo ms |
|---:|---|---|---:|---:|
| 3 | Auth | `POST /cambiar-clave` | 612,61 | 625,61 |
| 3 | Catálogo | `GET /compras` | 924,86 | 1139,82 |
| 6 | Auth | `POST /renovar` | 264,66 | 1235,13 |
| 6 | Auth | `POST /cambiar-clave` | 517,22 | 1359,34 |
| 6 | Catálogo | `POST /carrito/agregar` | 78,88 | 931,24 |
| 6 | Catálogo | `DELETE /carrito/vaciar` | 70,98 | 741,39 |
| 6 | Catálogo | `POST /compras/:id/pago` | 93,96 | 885,97 |
| 6 | Subastas | `POST /api/ecommerce/subastas` | 103,83 | 913,65 |
| 8 | Auth | `POST /save` | 486,62 | 1033,61 |
| 8 | Auth | `POST /login` | 541,53 | 1169,49 |
| 8 | Auth | `POST /renovar` | 549,76 | 979,30 |
| 8 | Auth | `POST /cambiar-clave` | 1032,93 | 1270,45 |
| 8 | Catálogo | `POST /carrito/agregar` | 72,98 | 561,36 |
| 8 | Catálogo | `PATCH /compras/:id/estado` | 101,05 | 633,62 |

## Resultados por ruta

| Operación | 3 VU p95 / máx. | 6 VU p95 / máx. | 8 VU p95 / máx. |
|---|---:|---:|---:|
| Auth `POST /save` | 270,92 / 362,31 | 368,87 / 449,53 | 486,62 / 1033,61 |
| Auth `POST /login` | 231,81 / 246,46 | 226,04 / 265,86 | 541,53 / 1169,49 |
| Auth `GET /:id` | 26,15 / 38,08 | 16,69 / 28,38 | 29,19 / 58,66 |
| Auth `GET /api/auth/perfil` | 33,99 / 35,95 | 17,36 / 52,16 | 31,51 / 111,08 |
| Auth `GET /:id/existe` | 29,50 / 34,43 | 16,96 / 31,24 | 30,48 / 110,02 |
| Auth `PUT /update` | 34,99 / 37,61 | 37,09 / 101,82 | 50,16 / 112,18 |
| Auth `DELETE /:id` | 24,55 / 26,77 | 32,97 / 47,25 | 56,32 / 92,10 |
| Auth `GET /sesion` | 22,01 / 31,26 | 20,15 / 31,60 | 20,83 / 35,80 |
| Auth `POST /evaluar-clave` | 16,64 / 18,58 | 10,82 / 16,20 | 19,14 / 28,98 |
| Auth `POST /renovar` | 290,44 / 317,04 | 264,66 / 1235,13 | 549,76 / 979,30 |
| Auth `POST /cambiar-clave` | 612,61 / 625,61 | 517,22 / 1359,34 | 1032,93 / 1270,45 |
| Catálogo `GET /producto` | 36,04 / 57,11 | 26,99 / 50,75 | 27,45 / 93,32 |
| Catálogo `GET /inicio` | 16,71 / 17,10 | 16,37 / 35,88 | 38,24 / 69,13 |
| Catálogo `GET /lanzamientos` | 38,75 / 39,80 | 24,84 / 30,49 | 36,11 / 137,26 |
| Catálogo `GET /certificados/:id` | 28,02 / 37,94 | 20,06 / 24,72 | 29,56 / 121,16 |
| Catálogo `GET /producto/:id` | 25,24 / 27,30 | 17,97 / 31,53 | 28,15 / 55,52 |
| Catálogo `GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc` | 30,71 / 52,15 | 24,88 / 37,35 | 41,55 / 73,03 |
| Catálogo `GET /producto/categorias` | 10,63 / 18,10 | 13,84 / 17,75 | 18,30 / 84,13 |
| Catálogo `GET /producto/marcas` | 13,56 / 14,19 | 22,57 / 129,67 | 23,43 / 32,93 |
| Catálogo `POST /producto` | 43,11 / 103,44 | 48,67 / 126,73 | 89,40 / 133,30 |
| Catálogo `PUT /producto/:id` | 29,47 / 33,19 | 23,16 / 64,30 | 40,45 / 142,26 |
| Catálogo `DELETE /producto/:id` | 26,39 / 28,83 | 55,54 / 302,05 | 53,43 / 75,37 |
| Catálogo `GET /producto/:id/precio-usd` | 15,21 / 17,06 | 18,94 / 30,12 | 40,07 / 231,78 |
| Catálogo `GET /producto/entrega?diasHabiles=3` | 9,31 / 9,66 | 13,78 / 19,93 | 20,23 / 26,84 |
| Catálogo `GET /carrito` | 43,16 / 99,86 | 35,78 / 51,50 | 43,05 / 61,95 |
| Catálogo `GET /carrito/resumen` | 45,47 / 48,52 | 36,35 / 43,52 | 52,88 / 86,91 |
| Catálogo `POST /carrito/agregar` | 101,63 / 158,64 | 78,88 / 931,24 | 72,98 / 561,36 |
| Catálogo `PUT /carrito/actualizar/:id?cantidad=2` | 66,80 / 74,16 | 78,31 / 324,79 | 79,07 / 212,73 |
| Catálogo `DELETE /carrito/eliminar/:id` | 72,64 / 81,13 | 50,99 / 88,68 | 69,00 / 95,66 |
| Catálogo `DELETE /carrito/vaciar` | 74,35 / 86,73 | 70,98 / 741,39 | 69,25 / 134,31 |
| Catálogo `POST /compras/realizar` | 86,52 / 120,05 | 116,15 / 217,23 | 95,91 / 101,67 |
| Catálogo `POST /compras/:id/pago` | 104,71 / 114,55 | 93,96 / 885,97 | 87,63 / 213,08 |
| Catálogo `GET /compras/usuario/:id` | 62,58 / 85,44 | 49,95 / 155,13 | 71,58 / 119,70 |
| Catálogo `GET /compras/:id` | 59,35 / 210,57 | 29,78 / 67,14 | 38,02 / 60,61 |
| Catálogo `PATCH /compras/:id/estado` | 123,57 / 142,69 | 117,20 / 148,73 | 101,05 / 633,62 |
| Catálogo `GET /compras` | 924,86 / 1139,82 | 355,13 / 445,25 | 389,36 / 448,89 |
| Notificaciones `POST /validar` | 21,76 / 37,10 | 19,10 / 160,32 | 19,99 / 21,38 |
| Notificaciones `GET /api/notificaciones/mis-avisos?tamano=10` | 44,47 / 49,17 | 29,98 / 40,09 | 29,97 / 89,33 |
| Notificaciones `POST /previsualizar/bienvenida` | 8,21 / 10,76 | 18,01 / 27,68 | 23,49 / 39,03 |
| Notificaciones `POST /previsualizar/compra` | 15,28 / 29,57 | 18,43 / 25,80 | 34,90 / 97,12 |
| Notificaciones `GET /plan-reintentos` | 7,23 / 9,81 | 17,29 / 63,25 | 16,72 / 31,27 |
| Notificaciones `POST /encolar` | 20,37 / 20,60 | 28,14 / 46,12 | 29,38 / 59,38 |
| Subastas `POST /api/ecommerce/subastas` | 57,11 / 78,56 | 103,83 / 913,65 | 51,14 / 241,88 |
| Subastas `GET /api/subastas/inicio` | 31,77 / 35,48 | 13,93 / 118,05 | 21,79 / 31,17 |
| Subastas `GET /api/subastas/certificados/:id` | 36,58 / 46,45 | 32,06 / 80,02 | 22,43 / 71,98 |
| Subastas `GET /api/subastas/lote/:id/precio-usd` | 49,94 / 87,50 | 25,94 / 52,15 | 29,79 / 85,41 |
| Subastas `GET /api/subastas/entrega?diasHabiles=3` | 36,61 / 48,50 | 17,85 / 28,17 | 20,26 / 82,80 |
| Subastas `GET /api/subastas/clima` | 11,13 / 12,27 | 41,73 / 261,55 | 17,50 / 43,01 |
| Subastas `GET ?categoria=HOGAR&minimo=100&maximo=500&tamano=4` | 23,81 / 62,09 | 45,25 / 78,77 | 31,59 / 46,93 |
| Subastas `GET /:id` | 27,07 / 29,89 | 25,36 / 47,95 | 27,48 / 32,01 |
| Subastas `POST /:id/pujas` | 38,41 / 43,72 | 52,09 / 126,73 | 58,97 / 112,01 |
| Subastas `GET /mias` | 46,28 / 49,93 | 27,65 / 43,57 | 30,02 / 52,47 |
| Subastas `POST /:id/pago` | 68,96 / 81,04 | 29,89 / 75,53 | 31,48 / 80,89 |
| Subastas `POST /:id/cancelar` | 51,95 / 66,93 | 46,71 / 137,18 | 40,01 / 101,96 |

## Newman

93 peticiones, 0 fallos HTTP, 278 aserciones y 6 fallos de aserción. Media 136,78 ms. Máximo 1370,00 ms.

- Auth activo. expected 1078 to be below 500.
- Auth Swagger JSON. expected 797 to be below 500.
- Catálogo activo. expected 515 to be below 500.
- Catálogo Swagger JSON. expected 554 to be below 500.
- Registrar cliente. expected 1370 to be below 500.
- Cambiar contraseña. expected 634 to be below 500.

## Diagnóstico y arranque en frío

Spring Boot registró Auth 50,649 s, Catálogo 55,527 s, Notificaciones 31,29 s, Subastas 31,514 s. Los JAR iniciaron en paralelo. Es tiempo de inicialización, separado de la latencia de solicitudes con los servicios activos. La nube sigue pendiente.

PostgreSQL ejecutó los planes aislados de portada y compras en 0,529 y 5,819 ms, respectivamente. Las muestras con caché no establecen la causa de los picos HTTP de Catálogo.

| Ruta Auth | Fase | Muestras | Media ms |
|---|---|---:|---:|
| cambiar-clave | consulta | 62 | 3,18 |
| cambiar-clave | escritura | 62 | 24,81 |
| cambiar-clave | espera_hash | 124 | 21,99 |
| cambiar-clave | hash | 124 | 234,77 |
| login | consulta | 63 | 6,10 |
| login | espera_hash | 63 | 33,40 |
| login | hash | 63 | 208,71 |
| renovar | consulta | 62 | 2,12 |
| renovar | espera_hash | 62 | 16,60 |
| renovar | hash | 62 | 222,18 |
| save | consulta | 72 | 5,63 |
| save | escritura | 71 | 6,98 |
| save | espera_hash | 72 | 20,33 |
| save | hash | 71 | 185,01 |

## Errores funcionales en k6

- 8 VU. Auth `POST /save`, 1 errores.

La ruta de registro devolvió HTTP 503 bajo estrés. Auth usa ese código al saturarse el límite de hashing. Los tiempos de consulta, escritura, espera y hash de cambio de clave se registraron por separado.

[3 VU](vertice-editorial-k6-3.json) · [6 VU](vertice-editorial-k6-6.json) · [8 VU](vertice-editorial-k6-8.json) · [Fases Auth](vertice-editorial-k6-fases-8.json) · [Newman](vertice-editorial-postman.json) · [Plan PostgreSQL](vertice-editorial-plan.txt)

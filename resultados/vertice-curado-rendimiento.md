# Rendimiento de la galería curada VÉRTICE

k6 abrió sesiones y comprobó 52 rutas en cada perfil con los servicios activos. La revisión de navegador coincidió con los primeros perfiles. Las cifras describen esta ejecución local y el máximo de 500 ms se evalúa por ruta.

| VU | Peticiones | req/s | Media ms | p95 ms | p99 ms | Máximo ms | Errores |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 3 | 954 | 25,91 | 63,65 | 245,67 | 793,87 | 4360,50 | 0 |
| 6 | 1917 | 45,58 | 68,28 | 302,11 | 655,40 | 3268,34 | 1 |
| 8 | 3392 | 82,77 | 46,39 | 245,80 | 508,74 | 1056,74 | 0 |

## Rutas por encima de 500 ms

| VU | Servicio | Ruta | p95 ms | Máximo ms |
|---:|---|---|---:|---:|
| 3 | Auth | `POST /save` | 529,76 | 573,64 |
| 3 | Auth | `POST /cambiar-clave` | 996,34 | 1935,41 |
| 3 | Catálogo | `GET /producto` | 183,65 | 994,45 |
| 3 | Catálogo | `GET /producto/:id` | 218,10 | 1361,24 |
| 3 | Catálogo | `POST /producto` | 1484,61 | 4360,50 |
| 3 | Catálogo | `POST /carrito/agregar` | 351,47 | 2284,46 |
| 3 | Catálogo | `GET /compras` | 1165,68 | 1236,84 |
| 6 | Auth | `POST /save` | 1197,89 | 3074,39 |
| 6 | Auth | `POST /renovar` | 512,02 | 3006,68 |
| 6 | Auth | `POST /cambiar-clave` | 3180,58 | 3268,34 |
| 6 | Catálogo | `POST /producto` | 268,07 | 698,14 |
| 6 | Catálogo | `PUT /producto/:id` | 81,17 | 573,67 |
| 6 | Catálogo | `DELETE /producto/:id` | 94,64 | 1300,80 |
| 6 | Catálogo | `GET /producto/entrega?diasHabiles=3` | 26,78 | 874,09 |
| 6 | Catálogo | `DELETE /carrito/vaciar` | 402,25 | 1194,16 |
| 6 | Catálogo | `GET /compras` | 833,57 | 1601,49 |
| 6 | Subastas | `GET /api/subastas/inicio` | 87,30 | 510,15 |
| 6 | Subastas | `GET /:id` | 49,75 | 543,90 |
| 8 | Auth | `POST /save` | 466,94 | 573,52 |
| 8 | Auth | `POST /login` | 638,40 | 799,24 |
| 8 | Auth | `POST /renovar` | 478,84 | 832,97 |
| 8 | Auth | `POST /cambiar-clave` | 861,51 | 1056,74 |
| 8 | Catálogo | `POST /compras/realizar` | 240,00 | 591,28 |
| 8 | Catálogo | `GET /compras` | 455,38 | 661,80 |

## Resultados por ruta

| Operación | 3 VU p95 / máx. | 6 VU p95 / máx. | 8 VU p95 / máx. |
|---|---:|---:|---:|
| Auth `POST /save` | 529,76 / 573,64 | 1197,89 / 3074,39 | 466,94 / 573,52 |
| Auth `POST /login` | 274,66 / 275,29 | 337,90 / 482,26 | 638,40 / 799,24 |
| Auth `GET /:id` | 15,01 / 15,15 | 40,62 / 63,04 | 36,48 / 128,39 |
| Auth `GET /api/auth/perfil` | 28,47 / 31,73 | 28,22 / 80,43 | 39,87 / 239,17 |
| Auth `GET /:id/existe` | 14,06 / 27,41 | 16,41 / 20,31 | 30,08 / 78,90 |
| Auth `PUT /update` | 81,95 / 326,25 | 45,90 / 132,79 | 71,62 / 211,82 |
| Auth `DELETE /:id` | 371,56 / 434,51 | 61,43 / 179,85 | 71,71 / 108,75 |
| Auth `GET /sesion` | 24,55 / 35,33 | 49,45 / 59,98 | 25,54 / 102,65 |
| Auth `POST /evaluar-clave` | 31,16 / 37,30 | 23,86 / 137,50 | 21,16 / 53,68 |
| Auth `POST /renovar` | 285,05 / 307,26 | 512,02 / 3006,68 | 478,84 / 832,97 |
| Auth `POST /cambiar-clave` | 996,34 / 1935,41 | 3180,58 / 3268,34 | 861,51 / 1056,74 |
| Catálogo `GET /producto` | 183,65 / 994,45 | 41,40 / 67,36 | 31,02 / 63,12 |
| Catálogo `GET /inicio` | 12,94 / 36,19 | 49,68 / 93,19 | 41,46 / 123,26 |
| Catálogo `GET /lanzamientos` | 43,05 / 49,68 | 86,15 / 132,35 | 49,27 / 170,90 |
| Catálogo `GET /producto/:id` | 218,10 / 1361,24 | 26,45 / 57,78 | 23,31 / 44,60 |
| Catálogo `GET /producto/explorar?q=bruma&tamano=12&orden=precio-asc` | 71,63 / 306,53 | 43,65 / 357,35 | 36,66 / 53,81 |
| Catálogo `GET /producto/categorias` | 6,55 / 10,01 | 27,89 / 209,15 | 17,55 / 119,97 |
| Catálogo `GET /producto/marcas` | 14,60 / 32,66 | 20,95 / 37,99 | 24,02 / 383,94 |
| Catálogo `POST /producto` | 1484,61 / 4360,50 | 268,07 / 698,14 | 47,75 / 305,43 |
| Catálogo `PUT /producto/:id` | 26,10 / 34,05 | 81,17 / 573,67 | 36,19 / 57,18 |
| Catálogo `DELETE /producto/:id` | 60,60 / 73,46 | 94,64 / 1300,80 | 149,93 / 253,85 |
| Catálogo `GET /producto/:id/precio-usd` | 46,14 / 79,57 | 19,60 / 34,50 | 29,65 / 57,04 |
| Catálogo `GET /producto/entrega?diasHabiles=3` | 13,56 / 27,04 | 26,78 / 874,09 | 16,56 / 41,62 |
| Catálogo `GET /carrito` | 35,09 / 44,68 | 40,01 / 69,33 | 59,90 / 167,15 |
| Catálogo `GET /carrito/resumen` | 42,47 / 44,66 | 92,12 / 169,01 | 46,06 / 74,37 |
| Catálogo `POST /carrito/agregar` | 351,47 / 2284,46 | 137,02 / 294,78 | 93,44 / 402,55 |
| Catálogo `PUT /carrito/actualizar/:id?cantidad=2` | 74,38 / 83,11 | 109,36 / 119,63 | 110,63 / 305,64 |
| Catálogo `DELETE /carrito/eliminar/:id` | 57,08 / 65,01 | 141,59 / 221,36 | 62,75 / 316,71 |
| Catálogo `DELETE /carrito/vaciar` | 66,75 / 182,15 | 402,25 / 1194,16 | 87,58 / 237,38 |
| Catálogo `POST /compras/realizar` | 148,67 / 206,30 | 103,58 / 251,68 | 240,00 / 591,28 |
| Catálogo `POST /compras/:id/pago` | 127,03 / 140,08 | 197,83 / 282,23 | 106,90 / 376,22 |
| Catálogo `GET /compras/usuario/:id` | 45,05 / 57,63 | 63,60 / 101,84 | 72,97 / 313,18 |
| Catálogo `GET /compras/:id` | 15,40 / 16,32 | 42,88 / 61,06 | 19,65 / 44,25 |
| Catálogo `PATCH /compras/:id/estado` | 121,53 / 304,45 | 141,27 / 245,54 | 66,21 / 158,46 |
| Catálogo `GET /compras` | 1165,68 / 1236,84 | 833,57 / 1601,49 | 455,38 / 661,80 |
| Notificaciones `POST /validar` | 275,79 / 294,12 | 35,85 / 54,92 | 122,38 / 258,79 |
| Notificaciones `GET /api/notificaciones/mis-avisos?tamano=10` | 60,94 / 104,56 | 44,92 / 72,00 | 36,31 / 108,93 |
| Notificaciones `POST /previsualizar/bienvenida` | 14,15 / 14,99 | 21,17 / 32,39 | 22,23 / 300,01 |
| Notificaciones `POST /previsualizar/compra` | 15,49 / 31,74 | 34,00 / 40,64 | 17,33 / 70,96 |
| Notificaciones `GET /plan-reintentos` | 11,41 / 13,91 | 45,44 / 466,44 | 20,53 / 62,33 |
| Notificaciones `POST /encolar` | 108,21 / 313,24 | 35,26 / 41,31 | 45,72 / 304,29 |
| Subastas `POST /api/ecommerce/subastas` | 86,66 / 102,23 | 186,68 / 263,22 | 65,28 / 121,66 |
| Subastas `GET /api/subastas/inicio` | 25,19 / 26,67 | 87,30 / 510,15 | 39,23 / 46,16 |
| Subastas `GET /api/subastas/lote/:id/precio-usd` | 125,73 / 468,99 | 41,01 / 77,08 | 30,86 / 61,37 |
| Subastas `GET /api/subastas/entrega?diasHabiles=3` | 18,95 / 27,59 | 32,88 / 91,16 | 34,30 / 90,91 |
| Subastas `GET /api/subastas/clima` | 30,52 / 51,38 | 61,50 / 82,50 | 44,93 / 305,20 |
| Subastas `GET ?categoria=HOGAR&minimo=100&maximo=500&tamano=4` | 61,85 / 76,01 | 69,32 / 120,63 | 43,09 / 104,42 |
| Subastas `GET /:id` | 160,66 / 499,32 | 49,75 / 543,90 | 27,52 / 45,92 |
| Subastas `POST /:id/pujas` | 170,10 / 405,83 | 98,74 / 405,07 | 46,37 / 68,43 |
| Subastas `GET /mias` | 98,54 / 161,01 | 141,04 / 241,53 | 27,25 / 47,10 |
| Subastas `POST /:id/pago` | 134,31 / 473,64 | 56,54 / 66,16 | 34,33 / 45,99 |
| Subastas `POST /:id/cancelar` | 67,91 / 73,20 | 58,56 / 82,92 | 55,18 / 90,97 |

## Newman

90 peticiones, 0 fallos HTTP, 269 aserciones y 11 fallos de aserción. Media 207,86 ms. Máximo 1622,00 ms.

- Auth activo. expected 650 to be below 500.
- Auth Swagger JSON. expected 722 to be below 500.
- Catálogo Swagger JSON. expected 1622 to be below 500.
- Notificaciones activo. expected 884 to be below 500.
- Notificaciones Swagger JSON. expected 810 to be below 500.
- Registrar cliente. expected 1347 to be below 500.
- Cambiar contraseña. expected 850 to be below 500.
- Listar productos. expected 558 to be below 500.
- Listado administrativo de pedidos. expected 570 to be below 500.
- Limpiar postor Uno. expected 935 to be at most 500.
- Inicio de tienda. expected 688 to be below 500.

## Diagnóstico previo y arranque en frío

Spring Boot registró Auth 61,439 s, Catálogo 64,539 s, Notificaciones 41,863 s, Subastas 43,005 s. Los JAR iniciaron en paralelo. Es tiempo de inicialización, separado de la latencia de solicitudes con los servicios activos. La nube sigue pendiente.

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

- 6 VU. Auth `POST /cambiar-clave`, 1 errores.

[3 VU](vertice-curado-k6-3.json) · [6 VU](vertice-curado-k6-6.json) · [8 VU](vertice-curado-k6-8.json) · [Fases Auth previas](vertice-editorial-k6-fases-8.json) · [Newman](vertice-curado-postman.json) · [Plan PostgreSQL](vertice-editorial-plan.txt)

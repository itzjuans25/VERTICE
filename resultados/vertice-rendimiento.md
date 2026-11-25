# Rendimiento local de VÉRTICE

k6 ejecutó 50 rutas por perfil con el servicio caliente. El resumen usa tiempos HTTP completos en milisegundos y marca en negrita los máximos superiores a 500 ms. Los errores son respuestas o comprobaciones funcionales fallidas del flujo. El umbral de tiempo se evalúa por separado.

| VU | Peticiones | Peticiones/s | Media ms | p95 ms | p99 ms | Máximo ms | Errores |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 3 | 1326 | 43,67 | 44,91 | 232,34 | 434,89 | 878,79 | 0 |
| 6 | 2958 | 72,60 | 42,38 | 269,85 | 586,99 | 944,28 | 0 |
| 8 | 3927 | 95,23 | 40,55 | 298,68 | 610,06 | 918,93 | 0 |

## Por ruta

| Servicio y ruta | 3 VU p95 / máximo | 6 VU p95 / máximo | 8 VU p95 / máximo |
|---|---:|---:|---:|
| Auth `POST /save` | 386,22 / **878,79** | 470,58 / **590,91** | 531,01 / **624,52** |
| Auth `POST /login` | 309,29 / 400,78 | 460,67 / **519,32** | 467,07 / **519,63** |
| Auth `GET /:id` | 10,38 / 23,69 | 13,88 / 36,89 | 17,92 / 40,12 |
| Auth `GET /api/auth/perfil` | 32,82 / 42,93 | 14,27 / 20,47 | 14,36 / 37,28 |
| Auth `GET /:id/existe` | 16,41 / 71,96 | 9,42 / 113,73 | 13,85 / 23,36 |
| Auth `PUT /update` | 17,16 / 26,54 | 15,99 / 34,26 | 20,09 / 26,01 |
| Auth `DELETE /:id` | 32,02 / 40,32 | 23,35 / 29,57 | 25,32 / 43,68 |
| Auth `GET /sesion` | 19,64 / 42,65 | 10,28 / 18,35 | 10,80 / 29,81 |
| Auth `POST /evaluar-clave` | 21,42 / 28,80 | 13,53 / 28,20 | 6,04 / 18,66 |
| Auth `POST /renovar` | 251,38 / 341,55 | 474,46 / **524,43** | 432,45 / 462,97 |
| Auth `POST /cambiar-clave` | 539,80 / **577,49** | 847,08 / **944,28** | 853,97 / **918,93** |
| Catálogo `GET /producto` | 79,69 / 108,93 | 18,68 / 34,09 | 21,67 / 59,16 |
| Catálogo `GET /inicio` | 36,32 / 71,27 | 7,83 / 20,49 | 18,76 / 28,20 |
| Catálogo `GET /producto/:id` | 40,03 / 50,96 | 14,41 / 35,95 | 12,05 / 29,85 |
| Catálogo `GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc` | 194,90 / 267,19 | 20,57 / 40,36 | 19,55 / 46,33 |
| Catálogo `GET /producto/categorias` | 12,62 / 20,59 | 10,80 / 28,16 | 11,03 / 72,91 |
| Catálogo `GET /producto/marcas` | 9,24 / 256,19 | 11,81 / 103,83 | 11,43 / 18,74 |
| Catálogo `POST /producto` | 103,31 / 157,86 | 40,93 / 114,98 | 28,41 / 87,90 |
| Catálogo `PUT /producto/:id` | 58,03 / 75,42 | 15,12 / 18,26 | 20,60 / 26,85 |
| Catálogo `DELETE /producto/:id` | 27,87 / 148,43 | 29,72 / 49,90 | 23,13 / 41,93 |
| Catálogo `GET /producto/:id/precio-usd` | 189,72 / 276,27 | 12,26 / 19,30 | 15,14 / 34,10 |
| Catálogo `GET /producto/entrega?diasHabiles=3` | 24,96 / 29,01 | 6,29 / 9,95 | 6,73 / 26,98 |
| Catálogo `GET /carrito` | 48,87 / 61,90 | 31,18 / 44,25 | 26,16 / 48,18 |
| Catálogo `GET /carrito/resumen` | 69,55 / 276,37 | 34,27 / 83,81 | 31,42 / 59,16 |
| Catálogo `POST /carrito/agregar` | 109,47 / 303,47 | 44,95 / 109,44 | 38,72 / 76,62 |
| Catálogo `PUT /carrito/actualizar/:id?cantidad=2` | 64,62 / 71,28 | 41,51 / 58,79 | 41,25 / 86,29 |
| Catálogo `DELETE /carrito/eliminar/:id` | 39,75 / 52,46 | 48,19 / 70,44 | 35,82 / 63,47 |
| Catálogo `DELETE /carrito/vaciar` | 66,91 / 77,17 | 30,96 / 191,04 | 25,32 / 57,74 |
| Catálogo `POST /compras/realizar` | 157,95 / 314,11 | 74,92 / 269,19 | 57,44 / 75,35 |
| Catálogo `GET /compras/usuario/:id` | 72,59 / **767,04** | 31,48 / 64,81 | 31,51 / 85,79 |
| Catálogo `GET /compras/:id` | 28,34 / 35,67 | 21,18 / 34,13 | 14,72 / 38,63 |
| Catálogo `PATCH /compras/:id/estado` | 150,52 / 164,28 | 51,38 / 72,66 | 37,01 / 55,89 |
| Catálogo `GET /compras` | 611,81 / **842,43** | 167,41 / 177,42 | 127,59 / 174,98 |
| Notificaciones `POST /validar` | 14,21 / 15,78 | 13,42 / 22,81 | 13,53 / 27,48 |
| Notificaciones `GET /api/notificaciones/mis-avisos?tamano=10` | 20,30 / 27,02 | 19,81 / 22,14 | 15,02 / 41,42 |
| Notificaciones `POST /previsualizar/bienvenida` | 16,21 / 29,07 | 9,01 / 12,37 | 11,41 / 37,47 |
| Notificaciones `POST /previsualizar/compra` | 18,55 / 38,84 | 10,78 / 184,67 | 15,06 / 136,68 |
| Notificaciones `GET /plan-reintentos` | 8,17 / 10,90 | 10,40 / 60,56 | 13,13 / 45,40 |
| Notificaciones `POST /encolar` | 116,06 / **561,12** | 18,31 / 166,08 | 23,27 / 41,79 |
| Subastas `POST ` | 37,98 / 60,31 | 33,20 / 75,08 | 22,88 / 45,40 |
| Subastas `GET /api/subastas/inicio` | 21,07 / 33,19 | 11,92 / 22,54 | 9,05 / 13,68 |
| Subastas `GET /api/subastas/lote/:id/precio-usd` | 20,84 / 22,82 | 14,43 / 18,35 | 13,68 / 28,02 |
| Subastas `GET /api/subastas/entrega?diasHabiles=3` | 10,29 / 31,55 | 11,60 / 22,09 | 8,87 / 14,45 |
| Subastas `GET /api/subastas/clima` | 26,63 / 38,60 | 12,99 / **507,05** | 14,33 / 29,25 |
| Subastas `GET ?categoria=HOGAR&minimo=100&maximo=500&tamano=4` | 25,81 / 32,74 | 21,38 / 77,69 | 16,93 / 29,56 |
| Subastas `GET /:id` | 13,23 / 22,77 | 17,58 / 21,47 | 14,91 / 33,38 |
| Subastas `POST /:id/pujas` | 42,03 / 49,78 | 29,42 / 39,33 | 34,47 / 115,42 |
| Subastas `GET /mias` | 20,82 / 50,23 | 10,54 / 13,00 | 14,29 / 33,36 |
| Subastas `POST /:id/pago` | 36,93 / 253,38 | 20,86 / 34,03 | 15,95 / 36,07 |
| Subastas `POST /:id/cancelar` | 30,21 / 38,34 | 24,63 / 40,27 | 19,91 / 28,49 |

## Arranque en frío de Windows

Spring Boot registró 33,855 s en Auth, 34,169 s en Catálogo, 19,227 s en Notificaciones y 20,144 s en Subastas durante el último arranque de los JAR. Los cuatro procesos arrancaron en paralelo. Son segundos de inicialización de la aplicación, separados de la latencia HTTP de Newman y k6. Los cuatro health checks y sus documentos OpenAPI respondieron después del arranque. Esta medición corresponde a Windows local. Los tiempos de suspensión y arranque en nube quedan pendientes de una URL pública.

## Newman

87 peticiones HTTP, 0 fallos HTTP, 6 incumplimientos de latencia de 259 aserciones. Media 188,20 ms y máximo 3821,00 ms.

- Catálogo activo. expected 542 to be below 500.
- Notificaciones activo. expected 523 to be below 500.
- Listado administrativo de pedidos. expected 955 to be below 500.
- Vaciar carrito. expected 522 to be below 500.
- Limpiar postor Uno. expected 3821 to be at most 500.
- Inicio de tienda. expected 1058 to be below 500.

## Diagnóstico

A 8 VU, cambiar la clave hizo dos operaciones Argon2id por petición. Micrometer registró 156 hashes para 78 cambios. El hash promedió 256,41 ms por ejecución y la espera del permiso 27,28 ms. Las fases de consulta y escritura promediaron 1,65 y 9,39 ms por petición. Las medias de fase no identifican la causa de cada máximo HTTP. El perfil señala el coste del hash y su espera como los principales componentes medidos de Auth.

Los picos de Catálogo, Notificaciones y clima carecen de trazas por petición que permitan atribuir una causa. La integración de clima lee una referencia en caché y actualiza el proveedor en segundo plano. El 507,05 ms aislado no mide el tiempo de la API externa.

[k6 3 VU](vertice-k6-3.json) · [k6 6 VU](vertice-k6-6.json) · [k6 8 VU](vertice-k6-8.json) · [Fases Auth](vertice-k6-fases-8.json) · [Newman](vertice-postman.json)

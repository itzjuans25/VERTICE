# Medición TechTrade por ruta

Se muestran los tres perfiles finales. Cada tabla usa la duración HTTP de k6 (ms). El resultado por ruta excluye calentamiento y limpieza.

## 3 usuarios

[JSON de k6](carga-techtrade.json)

| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| auth_registrar | POST /save | 292,88 | 483,35 | 509,60 | 516,48 | 0 | 24 | 0,70 |
| auth_login | POST /login | 310,89 | 632,20 | 645,09 | 648,51 | 0 | 24 | 0,70 |
| auth_perfil | GET /:id | 10,91 | 14,70 | 14,78 | 14,79 | 0 | 24 | 0,70 |
| auth_existe | GET /:id/existe | 9,19 | 14,02 | 14,17 | 14,18 | 0 | 24 | 0,70 |
| auth_actualizar | PUT /update | 15,95 | 35,17 | 37,49 | 37,62 | 0 | 24 | 0,70 |
| auth_eliminar | DELETE /:id | 15,05 | 24,19 | 24,48 | 24,54 | 0 | 24 | 0,70 |
| auth_sesion | GET /sesion | 10,98 | 16,05 | 26,94 | 30,04 | 0 | 24 | 0,70 |
| auth_evaluar | POST /evaluar-clave | 7,99 | 14,73 | 30,45 | 34,84 | 0 | 24 | 0,70 |
| auth_renovar | POST /renovar | 305,70 | 500,92 | 539,47 | 550,17 | 0 | 24 | 0,70 |
| auth_cambiar | POST /cambiar-clave | 536,54 | 631,31 | 641,45 | 643,73 | 0 | 24 | 0,70 |
| cat_productos | GET /producto | 16,57 | 32,61 | 35,61 | 36,25 | 0 | 24 | 0,70 |
| cat_producto | GET /producto/:id | 10,46 | 17,42 | 18,97 | 19,39 | 0 | 24 | 0,70 |
| cat_explorar | GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc | 17,59 | 34,30 | 50,39 | 54,60 | 0 | 24 | 0,70 |
| cat_categorias | GET /producto/categorias | 6,68 | 10,73 | 11,38 | 11,56 | 0 | 24 | 0,70 |
| cat_publicar | POST /publicaciones | 66,64 | 106,41 | 132,90 | 140,18 | 0 | 24 | 0,70 |
| cat_publicaciones_mias | GET /publicaciones/mias | 13,48 | 20,29 | 24,11 | 25,23 | 0 | 24 | 0,70 |
| cat_publicacion_estado | PATCH /publicaciones/:id/estado | 19,46 | 35,30 | 42,93 | 44,89 | 0 | 24 | 0,70 |
| cat_publicacion_foto | GET /producto/:id/foto/0 | 14,56 | 27,57 | 36,41 | 38,88 | 0 | 24 | 0,70 |
| cat_crear | POST /producto | 23,90 | 57,30 | 57,60 | 57,66 | 0 | 24 | 0,70 |
| cat_actualizar | PUT /producto/:id | 17,68 | 24,48 | 80,54 | 97,07 | 0 | 24 | 0,70 |
| cat_eliminar | DELETE /producto/:id | 16,27 | 33,12 | 33,93 | 33,93 | 0 | 24 | 0,70 |
| cat_precio | GET /producto/:id/precio-usd | 9,23 | 15,24 | 15,55 | 15,64 | 0 | 24 | 0,70 |
| cat_entrega | GET /producto/entrega?diasHabiles=3 | 6,11 | 10,66 | 12,40 | 12,83 | 0 | 24 | 0,70 |
| cat_carrito | GET /carrito | 27,19 | 41,35 | 42,90 | 43,33 | 0 | 24 | 0,70 |
| cat_resumen | GET /carrito/resumen | 32,97 | 60,27 | 63,40 | 64,28 | 0 | 24 | 0,70 |
| cat_agregar | POST /carrito/agregar | 40,08 | 63,26 | 65,58 | 66,37 | 0 | 48 | 1,41 |
| cat_cantidad | PUT /carrito/actualizar/:id?cantidad=2 | 38,86 | 64,94 | 65,97 | 66,11 | 0 | 24 | 0,70 |
| cat_quitar | DELETE /carrito/eliminar/:id | 34,26 | 49,64 | 50,31 | 50,42 | 0 | 24 | 0,70 |
| cat_vaciar | DELETE /carrito/vaciar | 25,22 | 39,27 | 41,66 | 42,27 | 0 | 24 | 0,70 |
| cat_comprar | POST /compras/realizar | 74,29 | 135,60 | 170,26 | 179,66 | 0 | 24 | 0,70 |
| cat_historial | GET /compras/usuario/:id | 34,08 | 53,07 | 66,56 | 70,50 | 0 | 24 | 0,70 |
| cat_compra | GET /compras/:id | 16,59 | 37,65 | 52,27 | 56,03 | 0 | 24 | 0,70 |
| cat_estado | PATCH /compras/:id/estado | 41,30 | 66,93 | 71,48 | 72,63 | 0 | 24 | 0,70 |
| cat_compras | GET /compras | 219,25 | 513,57 | 531,10 | 532,18 | 0 | 24 | 0,70 |
| notif_validar | POST /validar | 8,17 | 20,11 | 21,61 | 22,01 | 0 | 24 | 0,70 |
| notif_bienvenida | POST /previsualizar/bienvenida | 7,00 | 11,02 | 12,80 | 13,31 | 0 | 24 | 0,70 |
| notif_compra | POST /previsualizar/compra | 7,38 | 10,57 | 19,90 | 22,65 | 0 | 24 | 0,70 |
| notif_reintentos | GET /plan-reintentos | 6,06 | 9,91 | 10,78 | 10,98 | 0 | 24 | 0,70 |
| notif_encolar | POST /encolar | 16,33 | 44,40 | 54,08 | 56,84 | 0 | 24 | 0,70 |

## 6 usuarios

[JSON de k6](estres-6vu-techtrade.json)

| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| auth_registrar | POST /save | 350,22 | 478,95 | 647,21 | 777,78 | 0 | 54 | 1,33 |
| auth_login | POST /login | 344,65 | 451,94 | 487,24 | 511,15 | 0 | 54 | 1,33 |
| auth_perfil | GET /:id | 11,55 | 25,77 | 95,30 | 126,83 | 0 | 54 | 1,33 |
| auth_existe | GET /:id/existe | 7,61 | 15,86 | 22,98 | 24,89 | 0 | 54 | 1,33 |
| auth_actualizar | PUT /update | 11,93 | 21,27 | 45,93 | 67,93 | 0 | 54 | 1,33 |
| auth_eliminar | DELETE /:id | 11,23 | 17,90 | 23,25 | 23,70 | 0 | 54 | 1,33 |
| auth_sesion | GET /sesion | 8,08 | 19,77 | 21,11 | 22,45 | 0 | 54 | 1,33 |
| auth_evaluar | POST /evaluar-clave | 5,43 | 12,87 | 24,20 | 35,60 | 0 | 54 | 1,33 |
| auth_renovar | POST /renovar | 332,07 | 469,46 | 523,15 | 531,83 | 0 | 54 | 1,33 |
| auth_cambiar | POST /cambiar-clave | 688,34 | 1069,00 | 1178,40 | 1202,61 | 0 | 54 | 1,33 |
| cat_productos | GET /producto | 11,92 | 19,08 | 28,23 | 36,65 | 0 | 54 | 1,33 |
| cat_producto | GET /producto/:id | 8,47 | 16,09 | 34,00 | 43,73 | 0 | 54 | 1,33 |
| cat_explorar | GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc | 11,80 | 16,38 | 25,26 | 26,40 | 0 | 54 | 1,33 |
| cat_categorias | GET /producto/categorias | 4,60 | 10,12 | 16,26 | 21,61 | 0 | 54 | 1,33 |
| cat_publicar | POST /publicaciones | 54,48 | 83,19 | 119,54 | 158,15 | 0 | 54 | 1,33 |
| cat_publicaciones_mias | GET /publicaciones/mias | 9,41 | 13,87 | 23,71 | 25,24 | 0 | 54 | 1,33 |
| cat_publicacion_estado | PATCH /publicaciones/:id/estado | 14,71 | 24,37 | 32,12 | 35,18 | 0 | 54 | 1,33 |
| cat_publicacion_foto | GET /producto/:id/foto/0 | 9,25 | 17,36 | 20,99 | 21,61 | 0 | 54 | 1,33 |
| cat_crear | POST /producto | 14,50 | 27,68 | 59,60 | 70,79 | 0 | 54 | 1,33 |
| cat_actualizar | PUT /producto/:id | 10,27 | 15,46 | 18,45 | 19,53 | 0 | 54 | 1,33 |
| cat_eliminar | DELETE /producto/:id | 10,99 | 18,58 | 23,53 | 26,58 | 0 | 54 | 1,33 |
| cat_precio | GET /producto/:id/precio-usd | 7,29 | 11,32 | 22,21 | 28,39 | 0 | 54 | 1,33 |
| cat_entrega | GET /producto/entrega?diasHabiles=3 | 5,94 | 10,36 | 30,77 | 35,73 | 0 | 54 | 1,33 |
| cat_carrito | GET /carrito | 24,93 | 28,49 | 196,87 | 374,00 | 0 | 54 | 1,33 |
| cat_resumen | GET /carrito/resumen | 20,91 | 37,70 | 47,16 | 49,86 | 0 | 54 | 1,33 |
| cat_agregar | POST /carrito/agregar | 28,12 | 43,70 | 67,11 | 79,36 | 0 | 108 | 2,66 |
| cat_cantidad | PUT /carrito/actualizar/:id?cantidad=2 | 25,66 | 38,66 | 55,08 | 71,47 | 0 | 54 | 1,33 |
| cat_quitar | DELETE /carrito/eliminar/:id | 24,67 | 36,07 | 51,10 | 52,80 | 0 | 54 | 1,33 |
| cat_vaciar | DELETE /carrito/vaciar | 18,79 | 33,09 | 41,96 | 46,30 | 0 | 54 | 1,33 |
| cat_comprar | POST /compras/realizar | 45,31 | 69,58 | 84,15 | 89,12 | 0 | 54 | 1,33 |
| cat_historial | GET /compras/usuario/:id | 22,22 | 30,38 | 33,19 | 35,63 | 0 | 54 | 1,33 |
| cat_compra | GET /compras/:id | 13,15 | 17,67 | 82,33 | 145,19 | 0 | 54 | 1,33 |
| cat_estado | PATCH /compras/:id/estado | 26,44 | 38,09 | 44,27 | 46,60 | 0 | 54 | 1,33 |
| cat_compras | GET /compras | 82,29 | 139,61 | 187,16 | 225,81 | 0 | 54 | 1,33 |
| notif_validar | POST /validar | 7,03 | 14,18 | 22,39 | 24,92 | 0 | 54 | 1,33 |
| notif_bienvenida | POST /previsualizar/bienvenida | 8,85 | 12,84 | 84,42 | 154,27 | 0 | 54 | 1,33 |
| notif_compra | POST /previsualizar/compra | 7,50 | 10,91 | 45,96 | 61,80 | 0 | 54 | 1,33 |
| notif_reintentos | GET /plan-reintentos | 5,86 | 16,60 | 22,80 | 28,79 | 0 | 54 | 1,33 |
| notif_encolar | POST /encolar | 10,80 | 20,31 | 45,83 | 66,05 | 0 | 54 | 1,33 |

## 8 usuarios

[JSON de k6](estres-techtrade.json)

| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| auth_registrar | POST /save | 377,54 | 579,03 | 704,71 | 718,04 | 0 | 71 | 1,70 |
| auth_login | POST /login | 356,11 | 501,89 | 566,00 | 577,47 | 0 | 71 | 1,70 |
| auth_perfil | GET /:id | 7,46 | 17,26 | 27,02 | 29,49 | 0 | 71 | 1,70 |
| auth_existe | GET /:id/existe | 6,04 | 12,65 | 15,98 | 17,21 | 0 | 71 | 1,70 |
| auth_actualizar | PUT /update | 10,95 | 24,28 | 41,38 | 53,18 | 0 | 71 | 1,70 |
| auth_eliminar | DELETE /:id | 14,18 | 29,94 | 52,88 | 53,24 | 0 | 71 | 1,70 |
| auth_sesion | GET /sesion | 6,53 | 11,57 | 15,60 | 15,98 | 0 | 71 | 1,70 |
| auth_evaluar | POST /evaluar-clave | 4,09 | 9,36 | 13,48 | 20,13 | 0 | 71 | 1,70 |
| auth_renovar | POST /renovar | 343,55 | 498,29 | 626,21 | 673,80 | 0 | 71 | 1,70 |
| auth_cambiar | POST /cambiar-clave | 688,59 | 955,20 | 1091,23 | 1095,08 | 0 | 71 | 1,70 |
| cat_productos | GET /producto | 11,32 | 20,19 | 42,19 | 82,79 | 0 | 71 | 1,70 |
| cat_producto | GET /producto/:id | 6,88 | 13,09 | 20,76 | 32,50 | 0 | 71 | 1,70 |
| cat_explorar | GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc | 10,91 | 19,36 | 26,36 | 28,48 | 0 | 71 | 1,70 |
| cat_categorias | GET /producto/categorias | 4,10 | 9,07 | 16,00 | 25,24 | 0 | 71 | 1,70 |
| cat_publicar | POST /publicaciones | 63,10 | 114,39 | 136,26 | 143,37 | 0 | 71 | 1,70 |
| cat_publicaciones_mias | GET /publicaciones/mias | 8,78 | 14,95 | 26,46 | 37,87 | 0 | 71 | 1,70 |
| cat_publicacion_estado | PATCH /publicaciones/:id/estado | 14,70 | 27,86 | 49,31 | 67,10 | 0 | 71 | 1,70 |
| cat_publicacion_foto | GET /producto/:id/foto/0 | 8,58 | 14,74 | 21,46 | 31,59 | 0 | 71 | 1,70 |
| cat_crear | POST /producto | 12,94 | 20,63 | 26,98 | 34,22 | 0 | 71 | 1,70 |
| cat_actualizar | PUT /producto/:id | 9,57 | 21,21 | 33,63 | 39,63 | 0 | 71 | 1,70 |
| cat_eliminar | DELETE /producto/:id | 11,52 | 18,69 | 29,02 | 35,79 | 0 | 71 | 1,70 |
| cat_precio | GET /producto/:id/precio-usd | 6,34 | 11,78 | 14,68 | 19,56 | 0 | 71 | 1,70 |
| cat_entrega | GET /producto/entrega?diasHabiles=3 | 3,50 | 7,74 | 9,74 | 10,50 | 0 | 71 | 1,70 |
| cat_carrito | GET /carrito | 19,21 | 34,44 | 108,93 | 178,27 | 0 | 71 | 1,70 |
| cat_resumen | GET /carrito/resumen | 20,11 | 27,97 | 105,72 | 243,63 | 0 | 71 | 1,70 |
| cat_agregar | POST /carrito/agregar | 25,84 | 42,90 | 85,70 | 100,83 | 0 | 142 | 3,39 |
| cat_cantidad | PUT /carrito/actualizar/:id?cantidad=2 | 24,78 | 38,21 | 54,66 | 64,82 | 0 | 71 | 1,70 |
| cat_quitar | DELETE /carrito/eliminar/:id | 30,95 | 42,40 | 245,46 | 249,71 | 0 | 71 | 1,70 |
| cat_vaciar | DELETE /carrito/vaciar | 18,26 | 30,76 | 104,79 | 152,33 | 0 | 71 | 1,70 |
| cat_comprar | POST /compras/realizar | 44,57 | 70,53 | 125,12 | 134,47 | 0 | 71 | 1,70 |
| cat_historial | GET /compras/usuario/:id | 25,54 | 46,49 | 129,72 | 205,12 | 0 | 71 | 1,70 |
| cat_compra | GET /compras/:id | 8,86 | 13,07 | 19,06 | 24,53 | 0 | 71 | 1,70 |
| cat_estado | PATCH /compras/:id/estado | 30,56 | 52,41 | 56,47 | 60,85 | 0 | 71 | 1,70 |
| cat_compras | GET /compras | 72,08 | 157,28 | 179,21 | 189,85 | 0 | 71 | 1,70 |
| notif_validar | POST /validar | 5,63 | 9,75 | 21,60 | 38,41 | 0 | 71 | 1,70 |
| notif_bienvenida | POST /previsualizar/bienvenida | 5,24 | 10,21 | 22,12 | 24,79 | 0 | 71 | 1,70 |
| notif_compra | POST /previsualizar/compra | 5,26 | 13,06 | 16,30 | 21,10 | 0 | 71 | 1,70 |
| notif_reintentos | GET /plan-reintentos | 4,09 | 8,63 | 15,14 | 16,04 | 0 | 71 | 1,70 |
| notif_encolar | POST /encolar | 9,24 | 15,98 | 29,22 | 31,94 | 0 | 71 | 1,70 |

# k6 · TechStore por ruta

Duraciones en milisegundos. El calentamiento y la limpieza no cuentan.

## 3 usuarios

[JSON de k6](carga-techstore.json)

| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| auth_registrar | POST /save | 119,10 | 153,76 | 179,28 | 195,34 | 0 | 45 | 1,65 |
| auth_login | POST /login | 103,21 | 124,42 | 155,65 | 167,13 | 0 | 45 | 1,65 |
| auth_perfil | GET /:id | 7,43 | 12,25 | 15,41 | 16,55 | 0 | 45 | 1,65 |
| auth_existe | GET /:id/existe | 6,35 | 11,82 | 12,93 | 13,28 | 0 | 45 | 1,65 |
| auth_actualizar | PUT /update | 9,57 | 17,92 | 19,52 | 19,60 | 0 | 45 | 1,65 |
| auth_eliminar | DELETE /:id | 10,73 | 16,93 | 26,53 | 26,57 | 0 | 45 | 1,65 |
| auth_sesion | GET /sesion | 7,19 | 11,36 | 13,73 | 14,44 | 0 | 45 | 1,65 |
| auth_evaluar | POST /evaluar-clave | 5,03 | 11,27 | 14,39 | 14,70 | 0 | 45 | 1,65 |
| auth_renovar | POST /renovar | 107,43 | 126,59 | 183,86 | 190,53 | 0 | 45 | 1,65 |
| auth_cambiar | POST /cambiar-clave | 209,57 | 247,55 | 257,47 | 257,77 | 0 | 45 | 1,65 |
| cat_productos | GET /producto | 9,91 | 13,75 | 14,68 | 14,74 | 0 | 45 | 1,65 |
| cat_producto | GET /producto/:id | 6,99 | 10,14 | 11,18 | 11,28 | 0 | 45 | 1,65 |
| cat_explorar | GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc | 9,65 | 13,36 | 15,69 | 16,70 | 0 | 45 | 1,65 |
| cat_categorias | GET /producto/categorias | 2,82 | 4,21 | 4,84 | 5,12 | 0 | 45 | 1,65 |
| cat_marcas | GET /producto/marcas | 4,92 | 7,14 | 16,13 | 23,00 | 0 | 45 | 1,65 |
| cat_crear | POST /producto | 13,83 | 24,96 | 40,23 | 44,06 | 0 | 45 | 1,65 |
| cat_actualizar | PUT /producto/:id | 10,20 | 14,65 | 15,36 | 15,79 | 0 | 45 | 1,65 |
| cat_eliminar | DELETE /producto/:id | 9,54 | 14,07 | 17,15 | 17,37 | 0 | 45 | 1,65 |
| cat_precio | GET /producto/:id/precio-usd | 7,00 | 10,97 | 12,55 | 13,29 | 0 | 45 | 1,65 |
| cat_entrega | GET /producto/entrega?diasHabiles=3 | 3,16 | 4,40 | 4,68 | 4,70 | 0 | 45 | 1,65 |
| cat_carrito | GET /carrito | 15,79 | 21,98 | 23,80 | 23,97 | 0 | 45 | 1,65 |
| cat_resumen | GET /carrito/resumen | 20,15 | 28,62 | 45,34 | 57,48 | 0 | 45 | 1,65 |
| cat_agregar | POST /carrito/agregar | 30,82 | 42,75 | 65,15 | 219,34 | 0 | 90 | 3,31 |
| cat_cantidad | PUT /carrito/actualizar/:id?cantidad=2 | 28,09 | 38,82 | 39,56 | 39,66 | 0 | 45 | 1,65 |
| cat_quitar | DELETE /carrito/eliminar/:id | 25,65 | 32,65 | 126,36 | 199,03 | 0 | 45 | 1,65 |
| cat_vaciar | DELETE /carrito/vaciar | 19,00 | 35,48 | 94,57 | 127,51 | 0 | 45 | 1,65 |
| cat_comprar | POST /compras/realizar | 40,42 | 74,60 | 89,25 | 92,45 | 0 | 45 | 1,65 |
| cat_historial | GET /compras/usuario/:id | 21,28 | 30,11 | 66,18 | 70,21 | 0 | 45 | 1,65 |
| cat_compra | GET /compras/:id | 9,99 | 12,51 | 44,89 | 49,51 | 0 | 45 | 1,65 |
| cat_estado | PATCH /compras/:id/estado | 28,24 | 39,79 | 84,46 | 114,26 | 0 | 45 | 1,65 |
| cat_compras | GET /compras | 109,03 | 226,98 | 338,92 | 344,08 | 0 | 45 | 1,65 |
| notif_validar | POST /validar | 8,25 | 25,08 | 67,28 | 67,55 | 0 | 45 | 1,65 |
| notif_bienvenida | POST /previsualizar/bienvenida | 4,79 | 8,42 | 15,11 | 16,26 | 0 | 45 | 1,65 |
| notif_compra | POST /previsualizar/compra | 5,27 | 7,26 | 33,67 | 53,01 | 0 | 45 | 1,65 |
| notif_reintentos | GET /plan-reintentos | 3,76 | 6,69 | 12,24 | 16,37 | 0 | 45 | 1,65 |
| notif_encolar | POST /encolar | 8,32 | 12,90 | 23,54 | 28,14 | 0 | 45 | 1,65 |

## 6 usuarios

[JSON de k6](estres-6vu-techstore.json)

| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| auth_registrar | POST /save | 113,32 | 135,30 | 156,68 | 175,66 | 0 | 108 | 2,82 |
| auth_login | POST /login | 105,51 | 131,28 | 140,11 | 140,59 | 0 | 108 | 2,82 |
| auth_perfil | GET /:id | 4,82 | 6,97 | 10,86 | 20,29 | 0 | 108 | 2,82 |
| auth_existe | GET /:id/existe | 4,22 | 6,08 | 13,29 | 19,16 | 0 | 108 | 2,82 |
| auth_actualizar | PUT /update | 6,38 | 10,91 | 18,32 | 22,86 | 0 | 108 | 2,82 |
| auth_eliminar | DELETE /:id | 9,37 | 16,61 | 24,27 | 64,93 | 0 | 108 | 2,82 |
| auth_sesion | GET /sesion | 4,56 | 5,97 | 7,22 | 15,64 | 0 | 108 | 2,82 |
| auth_evaluar | POST /evaluar-clave | 3,09 | 6,42 | 9,60 | 11,46 | 0 | 108 | 2,82 |
| auth_renovar | POST /renovar | 101,64 | 128,83 | 140,55 | 160,22 | 0 | 108 | 2,82 |
| auth_cambiar | POST /cambiar-clave | 205,53 | 244,85 | 269,02 | 284,96 | 0 | 108 | 2,82 |
| cat_productos | GET /producto | 8,01 | 10,61 | 31,79 | 35,28 | 0 | 108 | 2,82 |
| cat_producto | GET /producto/:id | 4,94 | 6,80 | 8,25 | 11,52 | 0 | 108 | 2,82 |
| cat_explorar | GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc | 6,73 | 9,34 | 11,77 | 17,77 | 0 | 108 | 2,82 |
| cat_categorias | GET /producto/categorias | 2,23 | 3,22 | 6,29 | 9,46 | 0 | 108 | 2,82 |
| cat_marcas | GET /producto/marcas | 3,72 | 5,09 | 12,27 | 24,71 | 0 | 108 | 2,82 |
| cat_crear | POST /producto | 8,91 | 14,30 | 30,30 | 32,64 | 0 | 108 | 2,82 |
| cat_actualizar | PUT /producto/:id | 7,22 | 10,52 | 12,75 | 20,49 | 0 | 108 | 2,82 |
| cat_eliminar | DELETE /producto/:id | 9,51 | 16,55 | 66,66 | 95,69 | 0 | 108 | 2,82 |
| cat_precio | GET /producto/:id/precio-usd | 5,06 | 6,90 | 9,66 | 10,07 | 0 | 108 | 2,82 |
| cat_entrega | GET /producto/entrega?diasHabiles=3 | 2,29 | 3,37 | 5,66 | 6,79 | 0 | 108 | 2,82 |
| cat_carrito | GET /carrito | 11,07 | 18,11 | 22,43 | 23,55 | 0 | 108 | 2,82 |
| cat_resumen | GET /carrito/resumen | 14,61 | 22,62 | 30,69 | 40,57 | 0 | 108 | 2,82 |
| cat_agregar | POST /carrito/agregar | 21,29 | 30,25 | 39,66 | 130,55 | 0 | 216 | 5,64 |
| cat_cantidad | PUT /carrito/actualizar/:id?cantidad=2 | 21,07 | 30,50 | 37,75 | 38,59 | 0 | 108 | 2,82 |
| cat_quitar | DELETE /carrito/eliminar/:id | 16,09 | 23,27 | 26,30 | 27,37 | 0 | 108 | 2,82 |
| cat_vaciar | DELETE /carrito/vaciar | 11,20 | 17,65 | 20,53 | 21,10 | 0 | 108 | 2,82 |
| cat_comprar | POST /compras/realizar | 29,42 | 44,46 | 55,22 | 72,16 | 0 | 108 | 2,82 |
| cat_historial | GET /compras/usuario/:id | 14,12 | 20,26 | 24,52 | 30,67 | 0 | 108 | 2,82 |
| cat_compra | GET /compras/:id | 6,57 | 8,85 | 17,58 | 26,63 | 0 | 108 | 2,82 |
| cat_estado | PATCH /compras/:id/estado | 20,94 | 36,48 | 55,77 | 98,63 | 0 | 108 | 2,82 |
| cat_compras | GET /compras | 58,30 | 81,89 | 99,81 | 107,47 | 0 | 108 | 2,82 |
| notif_validar | POST /validar | 3,41 | 4,90 | 8,14 | 15,44 | 0 | 108 | 2,82 |
| notif_bienvenida | POST /previsualizar/bienvenida | 2,94 | 4,56 | 6,40 | 8,97 | 0 | 108 | 2,82 |
| notif_compra | POST /previsualizar/compra | 3,03 | 4,61 | 5,28 | 17,06 | 0 | 108 | 2,82 |
| notif_reintentos | GET /plan-reintentos | 2,54 | 4,35 | 6,38 | 7,17 | 0 | 108 | 2,82 |
| notif_encolar | POST /encolar | 6,98 | 12,32 | 42,60 | 46,32 | 0 | 108 | 2,82 |

## 8 usuarios

[JSON de k6](estres-techstore.json)

| Operación | Método y ruta | Promedio | p95 | p99 | Máximo | Errores | Peticiones | req/s |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| auth_registrar | POST /save | 191,08 | 465,79 | 799,46 | 861,08 | 0 | 100 | 2,59 |
| auth_login | POST /login | 188,41 | 412,60 | 547,78 | 591,87 | 0 | 100 | 2,59 |
| auth_perfil | GET /:id | 10,85 | 20,71 | 177,44 | 249,27 | 0 | 100 | 2,59 |
| auth_existe | GET /:id/existe | 9,66 | 21,73 | 113,47 | 115,07 | 0 | 100 | 2,59 |
| auth_actualizar | PUT /update | 12,59 | 43,64 | 117,50 | 126,13 | 0 | 100 | 2,59 |
| auth_eliminar | DELETE /:id | 14,30 | 36,69 | 117,85 | 135,66 | 0 | 100 | 2,59 |
| auth_sesion | GET /sesion | 10,20 | 18,25 | 101,96 | 274,95 | 0 | 100 | 2,59 |
| auth_evaluar | POST /evaluar-clave | 8,04 | 15,18 | 53,98 | 279,74 | 0 | 100 | 2,59 |
| auth_renovar | POST /renovar | 191,71 | 437,24 | 557,73 | 629,82 | 0 | 100 | 2,59 |
| auth_cambiar | POST /cambiar-clave | 382,73 | 826,43 | 941,48 | 1086,76 | 0 | 100 | 2,59 |
| cat_productos | GET /producto | 8,36 | 16,42 | 27,29 | 27,84 | 0 | 100 | 2,59 |
| cat_producto | GET /producto/:id | 6,12 | 15,17 | 22,72 | 29,85 | 0 | 100 | 2,59 |
| cat_explorar | GET /producto/explorar?q=monitor&tamano=12&orden=precio-asc | 7,21 | 14,55 | 18,54 | 21,46 | 0 | 100 | 2,59 |
| cat_categorias | GET /producto/categorias | 3,07 | 12,39 | 16,59 | 25,10 | 0 | 100 | 2,59 |
| cat_marcas | GET /producto/marcas | 4,36 | 8,67 | 13,19 | 48,87 | 0 | 100 | 2,59 |
| cat_crear | POST /producto | 13,83 | 50,80 | 79,92 | 84,70 | 0 | 100 | 2,59 |
| cat_actualizar | PUT /producto/:id | 7,90 | 16,72 | 36,51 | 38,69 | 0 | 100 | 2,59 |
| cat_eliminar | DELETE /producto/:id | 10,24 | 22,34 | 70,65 | 100,61 | 0 | 100 | 2,59 |
| cat_precio | GET /producto/:id/precio-usd | 6,03 | 15,60 | 21,15 | 26,07 | 0 | 100 | 2,59 |
| cat_entrega | GET /producto/entrega?diasHabiles=3 | 4,22 | 14,25 | 26,53 | 83,73 | 0 | 100 | 2,59 |
| cat_carrito | GET /carrito | 21,50 | 46,13 | 280,29 | 292,40 | 0 | 100 | 2,59 |
| cat_resumen | GET /carrito/resumen | 18,27 | 42,91 | 67,39 | 107,64 | 0 | 100 | 2,59 |
| cat_agregar | POST /carrito/agregar | 23,21 | 48,76 | 124,62 | 158,00 | 0 | 200 | 5,19 |
| cat_cantidad | PUT /carrito/actualizar/:id?cantidad=2 | 26,33 | 56,29 | 87,58 | 317,50 | 0 | 100 | 2,59 |
| cat_quitar | DELETE /carrito/eliminar/:id | 22,06 | 43,50 | 102,87 | 217,91 | 0 | 100 | 2,59 |
| cat_vaciar | DELETE /carrito/vaciar | 19,50 | 87,52 | 113,85 | 158,14 | 0 | 100 | 2,59 |
| cat_comprar | POST /compras/realizar | 33,22 | 70,93 | 105,44 | 145,72 | 0 | 100 | 2,59 |
| cat_historial | GET /compras/usuario/:id | 17,78 | 35,10 | 56,43 | 136,24 | 0 | 100 | 2,59 |
| cat_compra | GET /compras/:id | 8,06 | 20,72 | 44,15 | 53,44 | 0 | 100 | 2,59 |
| cat_estado | PATCH /compras/:id/estado | 25,29 | 54,89 | 115,08 | 193,78 | 0 | 100 | 2,59 |
| cat_compras | GET /compras | 81,10 | 166,75 | 188,22 | 310,84 | 0 | 100 | 2,59 |
| notif_validar | POST /validar | 4,91 | 17,96 | 29,92 | 40,91 | 0 | 100 | 2,59 |
| notif_bienvenida | POST /previsualizar/bienvenida | 3,31 | 11,05 | 18,57 | 24,25 | 0 | 100 | 2,59 |
| notif_compra | POST /previsualizar/compra | 4,24 | 9,08 | 19,26 | 66,09 | 0 | 100 | 2,59 |
| notif_reintentos | GET /plan-reintentos | 4,35 | 8,49 | 62,72 | 77,55 | 0 | 100 | 2,59 |
| notif_encolar | POST /encolar | 7,48 | 16,04 | 39,49 | 47,15 | 0 | 100 | 2,59 |

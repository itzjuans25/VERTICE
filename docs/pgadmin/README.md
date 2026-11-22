# Abrir PostgreSQL con pgAdmin

1. Ejecuta `iniciar.ps1`. Abre pgAdmin 4 desde Inicio si la ventana ya se cerró.
2. En **Tools → Import/Export Servers → Import**, selecciona [servers.json](servers.json), marca Local y Render y finaliza.
3. Abre **TechStore Local**. Conecta a `127.0.0.1`, puerto `55432`, base `ecommerce`, usuario `ecommerce_dev`. La contraseña local es `POSTGRES_PASSWORD` de tu `.env.native`.
4. Expande **Databases → ecommerce → Schemas → public → Tables**. Abre **Query Tool**, carga [verificacion.sql](verificacion.sql) y ejecuta con F5.
5. Para consultar correo SMTP, usa [Mailpit local](http://localhost:8025). PostgreSQL registra SMS simulados y el estado de avisos pendientes, sin conservar un historial de correo entregado.

## Render

1. En el panel de la base Render abre **Connect → External Database URL**. Copia el host externo, puerto, base y usuario. Mantén privados la URL completa y la contraseña.
2. En **Networking → IP Allow List**, autoriza tu IP pública con sufijo `/32`. Quita la regla cuando ya no necesites acceso externo.
3. Edita **TechStore Render → Properties → Connection** con esos datos. Define **SSL mode = Require** en las opciones avanzadas de conexión. Introduce la contraseña que muestra el panel al conectar.
4. Repite la consulta local usando el servidor Render. Los servicios Render de la misma región usan la conexión interna por separado. [Conexiones de Render PostgreSQL](https://render.com/docs/postgresql-creating-connecting)

![Diagrama de las tablas operativas](../../docs/pgadmin/er-techstore.png)

Las relaciones físicas del diagrama proceden del catálogo de PostgreSQL. `usuario_id` y `id_producto` son referencias lógicas de la aplicación cuando el motor carece de clave foránea. La base previa puede conservar tablas de funciones archivadas.

[Salida de la verificación local](../../resultados/pgadmin-local.txt) · [Consulta del esquema](esquema.sql)

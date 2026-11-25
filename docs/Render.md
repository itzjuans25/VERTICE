# Publicar la demo de VÉRTICE

La tienda React se aloja en Vercel Hobby. Render ejecuta los cuatro JAR, Neon guarda los datos persistentes, CloudAMQP enruta eventos y Brevo entrega correos. La demo local usa PostgreSQL, RabbitMQ y Mailpit. La publicación requiere las cuentas del propietario.

## 1. GitHub y proveedores

1. Sube la rama `subastas` a un repositorio propio en GitHub. Conserva la etiqueta `version-estable` como respaldo.
2. En Neon crea un proyecto PostgreSQL. En **Connect**, desactiva **Connection pooling** y copia el host directo, el nombre de la base y el rol. Guarda la contraseña en el panel, con SSL obligatorio.
3. En CloudAMQP crea una instancia RabbitMQ de demostración y obtiene el URI TLS completo que comienza por `amqps://`, incluido su vhost.
4. En Brevo configura un remitente verificado y obtén el usuario y la clave SMTP en **SMTP & API**. El Blueprint usa `smtp-relay.brevo.com:2525` con STARTTLS. Verifica la entrega después de publicar. [Configuración SMTP de Brevo](https://developers.brevo.com/docs/smtp-integration) · [Puertos restringidos en Render Free](https://render.com/docs/free)

## 2. Backend en Render

En Render abre **New → Blueprint**, autoriza el repositorio, selecciona la rama `subastas` y confirma la configuración. Si Render ya administra un Blueprint anterior, agrega a mano las nuevas variables `sync: false` en **Environment**. La plataforma solo solicita estos valores al crear el Blueprint. [Referencia de Blueprints](https://render.com/docs/blueprint-spec)

| Variable | Dónde va | De dónde sale |
| --- | --- | --- |
| `DB_HOST` | Los 4 | Host directo de Neon, sin `-pooler`. |
| `DB_NAME` | Los 4 | Base del proyecto Neon. |
| `DB_USERNAME` y `DB_PASSWORD` | Los 4 | Rol y contraseña de Neon. |
| `SPRING_RABBITMQ_ADDRESSES` | Los 4 | URI AMQPS de CloudAMQP. |
| `CORS_ALLOWED_ORIGINS` | Los 4 | URL pública de Vercel, sin barra final. Si aún falta Vercel, usa temporalmente `https://localhost.invalid` y corrígelo después. |
| `AUTH_SERVICE_URL` | Catálogo | URL HTTPS pública de Auth, sin barra final. Ajusta tras crear el Blueprint con las URLs definitivas. |
| `MAIL_USERNAME` y `MAIL_PASSWORD` | Notificaciones | Usuario y clave SMTP de Brevo. |
| `MAIL_FROM` | Notificaciones | Correo de un remitente verificado en Brevo. |

`JWT_SECRET` se genera una vez en el grupo `vertice-config`. Cada JAR comparte esa firma y toma `PORT` de Render. Spring resuelve el JDBC desde el host y la base indicados, con `sslmode=require`. Hikari usa hasta 4 conexiones, mínimo 0 inactivas, espera de 30 s y vida máxima de 4 min. El arranque tolera hasta 60 s ante fallos de conexión iniciales. Los servicios se llaman `vertice-auth`, `vertice-catalogo`, `vertice-subastas` y `vertice-notificaciones`. Los cuatro servicios exponen `/health`.

Para tener un administrador, registra tu cuenta desde el frontend y, en la consola SQL de tu propia base, actualiza únicamente su rol usando el ID de esa cuenta. Vuelve a iniciar sesión para que Auth emita el JWT actualizado.

## 3. Frontend en Vercel

1. Abre **Add New Project**, importa el mismo repositorio y elige la rama que publicaste.
2. Define **Root Directory = frontend** y Node.js 22. El proyecto utiliza Create React App. El directorio de salida es `build`.
3. Configura `REACT_APP_AUTH_URL`, `REACT_APP_CATALOGO_URL` , `REACT_APP_SUBASTAS_URL` y `REACT_APP_NOTIFICACIONES_URL` con las URLs HTTPS de Render, sin barra final. Cambiar una de ellas requiere un nuevo despliegue.
4. Añade la URL final de Vercel a `CORS_ALLOWED_ORIGINS` en Render y vuelve a desplegar los servicios si el panel lo solicita. Abre directamente `/productos` y `/subastas` para verificar el fallback de la SPA.
5. Verifica los cuatro `/health`, registro, login, carrito, compra y puja con `scripts/smoke-nube.ps1` o `scripts/smoke-nube.sh`. Usa un producto y un lote de demostración, porque el smoke cambia el stock y el precio. Para ver PostgreSQL usa [pgAdmin con Neon](pgadmin/README.md).

## Límites del alojamiento

Si publicas por Docker Compose con Caddy, la compilación usa rutas relativas `/servicios/subastas` y `/servicios/notificaciones`. Ambas pasan por HTTPS del mismo dominio del frontend. Comprueba `/servicios/subastas/health` y `/servicios/notificaciones/health`; luego inicia sesión y abre la actividad. Si publicas en Vercel con backends Render, configura las cuatro URLs HTTPS de la sección anterior.

- Render Free suspende cada web service tras 15 minutos sin tráfico entrante. Su arranque en frío tarda aproximadamente un minuto. Las 750 horas gratuitas mensuales son compartidas por el workspace. El cron de cierre y el consumo de avisos solo avanzan mientras los procesos estén activos. Precalienta los cuatro `/health` antes de presentar. El entorno Free sirve para una demostración con horario. [Render Free](https://render.com/docs/free)
- Neon Free indica 1 GB de PostgreSQL y 100 CU-h por proyecto. El cómputo suspende al pasar cinco minutos sin actividad. Una tarea periódica puede mantener activa la base mientras Render esté despierto. [Neon Free](https://neon.com/docs/introduction/plans) · [Scale to zero](https://neon.com/docs/introduction/scale-to-zero)
- Vercel Hobby está destinado a uso personal y no comercial. Los archivos estáticos los sirve la plataforma y las solicitudes de la aplicación van a los backends. [Vercel Hobby](https://vercel.com/docs/plans/hobby)

Docker local, con motor activo y `.env` privada preparada

```powershell
docker compose --env-file .env.example config --quiet
docker compose build auth catalogo notificaciones subastas frontend
docker compose up -d --wait
docker compose down
```

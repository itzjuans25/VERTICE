# Publicar TechStore en Render

El arranque local ya permite sustentar todo el recorrido. Usa Render cuando necesites un enlace público. El Blueprint está preparado para el frontend estático, tres servicios Docker y PostgreSQL administrado. [Referencia de Blueprint](https://render.com/docs/blueprint-spec)

1. Sube este proyecto a un repositorio GitHub o GitLab, excluyendo tus archivos de entorno. En CloudAMQP crea RabbitMQ **Little Lemur** y copia la URL AMQPS privada. [Planes CloudAMQP](https://www.cloudamqp.com/plans/)
2. En Brevo verifica un remitente y crea una clave en **SMTP y API → SMTP**. Guarda su login, clave SMTP y remitente para el asistente. El Blueprint usa `smtp-relay.brevo.com:2525` con STARTTLS. [Puertos SMTP de Brevo](https://help.brevo.com/hc/en-us/articles/10905415650322-Which-SMTP-port-should-I-use-Port-587-465-or-2525)
3. En Render pulsa **New → Blueprint → Connect repository**. Elige tu repositorio y rama. Usa `render.yaml`. Si el nombre del servicio ya existe, elige otro y actualiza sus URLs cuando queden asignadas.
4. En cada servicio Java, completa `SPRING_RABBITMQ_ADDRESSES` con la misma URL AMQPS. En `CORS_ALLOWED_ORIGINS` coloca la URL HTTPS pública de **techstore-web** sin barra final. Render genera el `JWT_SECRET` del grupo `techstore-config` para que Auth y Catálogo compartan una clave exclusiva del entorno remoto. Si el asistente aún no muestra las URLs, usa temporalmente las previstas y corrígelas en **Environment** con los valores reales.
5. En Catálogo define `AUTH_SERVICE_URL` con el enlace HTTPS de Auth. En el sitio estático define `REACT_APP_AUTH_URL` y `REACT_APP_CATALOGO_URL` con sus enlaces HTTPS. En Notificaciones completa `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM` con los datos de Brevo.
6. Pulsa **Deploy Blueprint**. Tras corregir URL o CORS, redepliega el sitio estático para incorporar las URLs al JavaScript. Catálogo aplica el esquema e inserta el seed al arrancar. El Blueprint mantiene la base cerrada para conexiones públicas hasta que autorices tu IP.
7. Abre `/health` en las tres URLs Java. Notificaciones comprueba PostgreSQL y RabbitMQ. Abre la tienda, filtra usados y registra una cuenta. Haz un pedido con dirección permitida en Brevo. Revisa su actividad transaccional y tu bandeja. Verifica el stock y el pedido con [pgAdmin](pgadmin/README.md).

El Blueprint inyecta el host interno y las credenciales de PostgreSQL. Desde pgAdmin utiliza el host externo con `sslmode=require` y una regla temporal para tu IP. Los servicios Java usan el puerto `PORT`. Hikari espera la primera conexión a PostgreSQL hasta 60 segundos, con timeout de conexión JDBC de 5 segundos por intento. Notificaciones vuelve a intentar inicializar las colas si RabbitMQ todavía no responde.

Para usar SMS reales, verifica el destinatario y el remitente en Twilio. Agrega `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` y `TWILIO_FROM` en **Environment** de Notificaciones y cambia `SMS_PROVIDER` a `twilio`. Prueba con un destino habilitado. En la configuración inicial, el modo `log` guarda SMS de prueba en PostgreSQL.

## Límites gratuitos comprobados

Render detiene servicios web Free tras 15 minutos sin tráfico y documenta un arranque de alrededor de un minuto. El primer request frío puede superar ampliamente 500 ms. La base Free vence a los 30 días y el workspace comparte 750 horas gratuitas al mes. Render bloquea salida por 25, 465 y 587 en Free. El Blueprint usa 2525 con STARTTLS, admitido por Brevo. Si el entorno permite 587, configura ese puerto. Verifica la conexión real después de desplegar. [Planes gratuitos de Render](https://render.com/docs/free) · [Puertos SMTP de Brevo](https://help.brevo.com/hc/en-us/articles/10905415650322-Which-SMTP-port-should-I-use-Port-587-465-or-2525)

Antes de publicar desde una máquina con Docker, ejecuta `docker compose build` y luego `docker compose up -d --wait` con un `.env` privado. En Windows, instala Docker Desktop con WSL 2, reinicia si Windows lo solicita y comprueba `docker info`. [Guía oficial de instalación](https://docs.docker.com/desktop/setup/install/windows-install/)

Con Docker local, el Compose incluido usa RabbitMQ y Mailpit. Si prefieres operar RabbitMQ propio para Render, aloja el broker Docker fuera de Render con AMQPS, acceso restringido y un vhost dedicado. Configura su URL en la misma variable.

La infraestructura pública y las credenciales se agregan desde tu cuenta. La verificación disponible en este equipo es Windows local.

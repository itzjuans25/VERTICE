# Instrucciones de arranque de TechStore

Desde la carpeta que contiene `docker-compose.yml`, ejecuta en PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\iniciar.ps1
```

El script prepara la base y el catálogo, supervisa el arranque y abre la tienda, Mailpit, Swagger y pgAdmin si está instalado. Usa `-SinNavegador` para omitir las ventanas y `-Recompilar` para forzar la compilación. Para detener todo lo iniciado por el proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\detener.ps1
```

Se conservan la base y los correos en `%LOCALAPPDATA%\EcommerceDev`. Las instancias de PostgreSQL y RabbitMQ instaladas como servicios de Windows son independientes de este proyecto.

| Servicio | Windows | Docker | Comprobación |
| --- | ---: | ---: | --- |
| Tienda React | 3000 | 3000 | [Tienda](http://localhost:3000) |
| Auth | 9090 | 9090 | [Swagger](http://localhost:9090/swagger-ui.html) |
| Catálogo | 9092 | 9092 | [Swagger](http://localhost:9092/swagger-ui.html) |
| Notificaciones | 9093 | 9093 | [Swagger](http://localhost:9093/swagger-ui.html) |
| PostgreSQL | 55432 | 5432 | `pg_isready` |
| RabbitMQ AMQP | 5673 | 5672 | Health de Notificaciones |
| Mailpit SMTP / web | 1025 / 8025 | 1025 / 8025 | [Bandeja](http://localhost:8025) |

En pgAdmin usa `127.0.0.1:55432`, base `ecommerce`, usuario `ecommerce_dev` y `POSTGRES_PASSWORD` de tu `.env.native`.
El arranque carga 45 productos sin duplicarlos. En la tienda puedes explorar sin cuenta. Para comprar, regístrate e inicia sesión.

## Requisitos locales

JDK 17, Node.js 22 con npm, binarios de PostgreSQL, RabbitMQ con Erlang OTP compatible y Mailpit. Maven se descarga con el wrapper la primera vez. Las versiones portables de Node.js y Mailpit también se detectan en `%LOCALAPPDATA%\EcommerceDev`.

## Modo Docker

Instala y abre Docker Desktop con el motor de contenedores Linux. En Windows puede pedir que habilites WSL 2 y reinicies. Si aún no tienes `.env`, copia `.env.example`. Configura contraseñas y `JWT_SECRET`, y define una clave aleatoria en `NOTIFICACIONES_API_KEY` si vas a demostrar `encolar`. [Instalación oficial de Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/)

```powershell
docker compose --env-file .env config --quiet
powershell -ExecutionPolicy Bypass -File .\iniciar.ps1 -Docker
docker compose ps
powershell -ExecutionPolicy Bypass -File .\detener.ps1 -Docker
```

Detén primero el modo Windows antes de iniciar Docker. Si ya tienes PostgreSQL o RabbitMQ en los puertos predeterminados de Docker, cambia `POSTGRES_PORT` y `RABBITMQ_PORT` en `.env`.

## Cuando algo no arranca

1. Revisa `%LOCALAPPDATA%\EcommerceDev` y el archivo `.log` o `.err.log` del componente. Con Docker usa `docker compose ps` y `docker compose logs --tail=100`.
2. Comprueba el puerto con `Get-NetTCPConnection -LocalPort 3000 -State Listen`. Si hay otra aplicación, usa otro puerto. Si cambias los puertos del frontend o Java, revisa CORS y las URLs `REACT_APP_*`.
3. Comprueba `/actuator/health` en los puertos 9090, 9092 y 9093. La primera consulta a Frankfurter puede demorar unos segundos.
4. En Swagger, usa el JWT del login en **Authorize**. Para `encolar`, usa la clave de tu archivo de entorno en el esquema `notificationKey`.

## Crear un administrador para la demostración

Registra primero una cuenta de demostración. En la raíz del proyecto, cambia `demo@example.test` por ese correo y ejecuta:

```powershell
. .\scripts\windows-local.ps1
$c = Read-EcommerceEnv '.env.native'
$psql = Get-EcommerceTool 'psql.exe' @(Get-ChildItem "$env:ProgramFiles/PostgreSQL/*/bin/psql.exe" |
    Select-Object -ExpandProperty FullName) 'PostgreSQL'
$env:PGPASSWORD = $c.POSTGRES_PASSWORD
try {
    "UPDATE usuario SET role = 'ADMIN' WHERE email = :'correo';" | & $psql -X -w -h 127.0.0.1 -p $c.POSTGRES_PORT -U $c.POSTGRES_USER -d $c.POSTGRES_DB -v 'correo=demo@example.test' -v 'ON_ERROR_STOP=1'
} finally {
    Remove-Item Env:PGPASSWORD
}
```

Cuando aparezca `UPDATE 1`, cierra sesión y vuelve a entrar para que el nuevo JWT incluya el rol.

## Reproducir las mediciones

Con el entorno Windows nativo en ejecución y k6 instalado:

```powershell
powershell -ExecutionPolicy Bypass -File .\pruebas\ejecutar-k6.ps1 -Modo carga -Resultado resultados/carga-techstore.json
powershell -ExecutionPolicy Bypass -File .\pruebas\ejecutar-k6.ps1 -Modo estres -VusEstres 6 -Resultado resultados/estres-6vu-techstore.json
powershell -ExecutionPolicy Bypass -File .\pruebas\ejecutar-k6.ps1 -Modo estres -VusEstres 8 -Resultado resultados/estres-techstore.json
```

Los perfiles crean datos sintéticos, incluidos pedidos. Ejecútalos en la base de desarrollo. Los JSON en `resultados` contienen una fila por ruta. Para cobertura ejecuta `mvnw.cmd clean verify` desde cada carpeta Java y abre `target/site/jacoco/index.html`.

Para regenerar los datos del artículo TechStore ejecuta `python pruebas/generar-techstore.py` con Pillow, después de Maven, k6, Newman y la prueba de navegador. La fuente IEEEtran y sus tablas están en `docs/articulo`.

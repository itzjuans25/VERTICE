# Postman en cinco minutos

Importa la colección **TechStore | Demostración completa** y los entornos **Local** y **Render**. Elige **Local** y ejecuta la colección completa desde **Run**. La primera carpeta revisa los servicios. Auth registra el comprador y guarda su token. Catálogo filtra, prepara un producto temporal, compra y muestra el pedido. Notificaciones valida el correo de compra en Mailpit. Las rutas de mantenimiento usan la cuenta Admin.

Para ejecutarla en Windows con una cuenta de administración temporal, inicia el proyecto y usa:

```powershell
.\postman\ejecutar-local.ps1
```

El HTML omite cuerpos, cabeceras y variables sensibles. Si una solicitud supera 500 ms, el informe lo registra como una aserción fallida aunque el código y la estructura sean correctos. En Render cambia las tres URLs con las que entregue el panel. Completa `adminEmail`, `adminPassword` y `notificationKey` con valores privados de Render para ejecutar las rutas protegidas. El paso Mailpit se omite cuando su URL está vacía.

[Reporte HTML local](reporte-local.html)

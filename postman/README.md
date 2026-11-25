# Postman en cinco minutos

Importa la colección **TechStore | Demostración completa** y los entornos **Local** y **Render**. Elige **Local** y ejecuta la colección completa desde **Run**. La primera carpeta revisa los servicios. Auth registra el comprador y guarda su token. Catálogo filtra, prepara un producto temporal, compra y muestra el pedido. Notificaciones valida el correo de compra en Mailpit. Las rutas de mantenimiento usan la cuenta Admin.

Para ejecutarla en Windows con una cuenta de administración temporal, inicia el proyecto y usa:

```powershell
.\postman\ejecutar-local.ps1
```

El HTML omite cuerpos, cabeceras y variables sensibles. El informe registra las respuestas que superan 500 ms como fallos de aserción. En Render ajusta las cuatro URLs del entorno al valor de cada servicio. Completa `adminEmail`, `adminPassword` y `notificationKey` con valores privados para las rutas protegidas. El paso Mailpit se omite cuando su URL está vacía.

[Reporte HTML de la galería curada](reporte-curado.html) · [Resultados de Newman](../resultados/vertice-curado-postman.json)

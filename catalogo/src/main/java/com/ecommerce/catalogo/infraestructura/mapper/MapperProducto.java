package com.ecommerce.catalogo.infraestructura.mapper;

import com.ecommerce.catalogo.domain.model.Producto;
import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto.ProductoData;
import org.springframework.stereotype.Component;

@Component
public class MapperProducto {
    public Producto toProducto(ProductoData data) {
        Producto producto = new Producto(
                data.getId(),
                data.getNombre(),
                data.getDescripcion(),
                data.getPrecio(),
                data.getStock(),
                data.getImagenUrl(), data.getCategoria()
        );
        producto.setCondicion(data.getCondicion());
        producto.setMarca(data.getMarca());
        producto.setGarantiaDias(data.getGarantiaDias());
        producto.setEstadoPublicacion(data.getEstadoPublicacion());
        producto.setGrado(data.getGrado());
        producto.setModelo(data.getModelo());
        producto.setDescripcionLarga(data.getDescripcionLarga());
        producto.setIncluye(data.getIncluye());
        producto.setCompatibilidad(data.getCompatibilidad());
        producto.setNotaEstado(data.getNotaEstado());
        producto.setImagenSecundariaUrl(data.getImagenSecundariaUrl());
        producto.setEspecificaciones(data.getEspecificaciones());
        return producto;
    }


    public ProductoData toData(Producto producto) {
        ProductoData data = new ProductoData(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getImagenUrl(), producto.getCategoria()
        );
        data.setCondicion(producto.getCondicion());
        data.setMarca(producto.getMarca());
        data.setGarantiaDias(producto.getGarantiaDias());
        data.setEstadoPublicacion(producto.getEstadoPublicacion());
        data.setGrado(producto.getGrado());
        data.setModelo(producto.getModelo());
        data.setDescripcionLarga(producto.getDescripcionLarga());
        data.setIncluye(producto.getIncluye());
        data.setCompatibilidad(producto.getCompatibilidad());
        data.setNotaEstado(producto.getNotaEstado());
        data.setImagenSecundariaUrl(producto.getImagenSecundariaUrl());
        data.setEspecificaciones(producto.getEspecificaciones());
        return data;
    }
}

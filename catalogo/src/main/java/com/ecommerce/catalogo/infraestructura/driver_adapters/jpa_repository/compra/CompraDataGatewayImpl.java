package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.compra;

import com.ecommerce.catalogo.domain.model.Compra;
import com.ecommerce.catalogo.domain.port.out.CompraGateway;
import com.ecommerce.catalogo.infraestructura.mapper.MapperCompra;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompraDataGatewayImpl implements CompraGateway {

    private final CompraDataJpaRepository repository;
    private final MapperCompra mapper;
    private final CertificadoDataRepository certificados;

    @Override
    public Compra guardar(Compra compra) {
        CompraData data = mapper.toData(compra);
        return mapper.toDomain(repository.save(data));
    }

    @Override
    public Compra buscarPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain)
                .orElse(null);
    }

    @Override
    public List<Compra> buscarPorUsuarioId(Long usuarioId) {
        return repository.findByUsuarioIdOrderByFechaCompraDesc(usuarioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Compra> obtenerTodas() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean cambiarEstado(Long id, String anterior, String nuevo) {
        return repository.cambiarEstado(id, anterior, nuevo) == 1;
    }

    @Override
    public boolean registrarPagoSimulado(Long id, Long propietario, Instant fecha) {
        return repository.registrarPagoSimulado(id, propietario, fecha) == 1;
    }

    @Override public long unidadesCompradas(Long usuarioId, Long productoId) {
        return repository.unidadesCompradas(usuarioId, productoId);
    }
    @Override public void guardarCertificado(Long compraId, Certificado certificado) {
        certificados.save(new CertificadoData(compraId, certificado));
    }
    @Override public Optional<Certificado> certificadoPorCodigo(String codigo) {
        return certificados.findById(codigo).map(CertificadoData::toDomain);
    }
    @Override public List<Certificado> certificados(Long compraId) {
        return certificados.findAllByCompraIdOrderByProductoId(compraId).stream().map(CertificadoData::toDomain).toList();
    }
}

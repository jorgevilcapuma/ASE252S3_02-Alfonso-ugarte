package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.FaseRequest;
import pe.edu.vallegrande.app.dto.response.FaseResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.FaseProyecto;
import pe.edu.vallegrande.app.repository.FaseProyectoRepository;
import pe.edu.vallegrande.app.service.FaseService;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class FaseServiceImpl implements FaseService {

    private final FaseProyectoRepository fases;

    @Override
    @Transactional(readOnly = true)
    public List<FaseResponse> listar() {
        return fases.findAllByOrderByOrdenAsc().stream().map(FaseResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FaseResponse obtener(Long id) {
        return FaseResponse.de(fase(id));
    }

    @Override
    @Transactional
    public FaseResponse crear(FaseRequest request) {
        if (fases.existsByOrden(request.orden())) {
            throw ApiException.conflicto("Ya existe una fase con el orden " + request.orden() + ".");
        }
        FaseProyecto fase = new FaseProyecto();
        aplicar(fase, request);
        return FaseResponse.de(fases.save(fase));
    }

    @Override
    @Transactional
    public FaseResponse actualizar(Long id, FaseRequest request) {
        FaseProyecto fase = fase(id);
        if (fases.existsByOrdenAndIdNot(request.orden(), id)) {
            throw ApiException.conflicto("Ya existe una fase con el orden " + request.orden() + ".");
        }
        aplicar(fase, request);
        return FaseResponse.de(fase);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        fases.delete(fase(id));
    }

    private static void aplicar(FaseProyecto fase, FaseRequest request) {
        fase.setOrden(request.orden());
        fase.setSemana(Textos.limpiar(request.semana()));
        fase.setTitulo(Textos.limpiar(request.titulo()));
        fase.setActividad(Textos.limpiar(request.actividad()));
        fase.setProducto(Textos.limpiar(request.producto()));
        fase.setEstado(request.estado());
    }

    private FaseProyecto fase(Long id) {
        return fases.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe la fase con id " + id + "."));
    }
}

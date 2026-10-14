package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.CursoRequest;
import pe.edu.vallegrande.app.dto.response.CursoResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.repository.AsignacionRepository;
import pe.edu.vallegrande.app.repository.CursoRepository;
import pe.edu.vallegrande.app.service.CursoService;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursos;
    private final AsignacionRepository asignaciones;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public List<CursoResponse> listar() {
        return cursos.findAllByOrderByNombreAsc().stream().map(CursoResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CursoResponse obtener(Long id) {
        return CursoResponse.de(entidades.curso(id));
    }

    @Override
    @Transactional
    public CursoResponse crear(CursoRequest request) {
        String nombre = Textos.limpiar(request.nombre());
        cursos.findByNombreIgnoreCase(nombre).ifPresent(c -> {
            throw ApiException.conflicto("El curso " + nombre + " ya está registrado.");
        });
        Curso curso = new Curso(nombre);
        curso.setDescripcion(Textos.limpiar(request.descripcion()));
        return CursoResponse.de(cursos.save(curso));
    }

    @Override
    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest request) {
        Curso curso = entidades.curso(id);
        String nombre = Textos.limpiar(request.nombre());
        cursos.findByNombreIgnoreCase(nombre).filter(otro -> !otro.getId().equals(id)).ifPresent(c -> {
            throw ApiException.conflicto("El curso " + nombre + " ya está registrado.");
        });
        curso.setNombre(nombre);
        curso.setDescripcion(Textos.limpiar(request.descripcion()));
        return CursoResponse.de(curso);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Curso curso = entidades.curso(id);
        if (asignaciones.existsByCursoId(id)) {
            throw ApiException.conflicto("No se puede eliminar el curso " + curso.getNombre() + " porque está asignado a secciones.");
        }
        cursos.delete(curso);
    }
}

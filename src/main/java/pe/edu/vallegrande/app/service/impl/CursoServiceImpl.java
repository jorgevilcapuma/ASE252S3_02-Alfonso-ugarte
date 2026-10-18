package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.dto.request.CursoRequest;
import pe.edu.vallegrande.app.dto.response.CursoResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.repository.AsistenciaEstudianteRepository;
import pe.edu.vallegrande.app.repository.CursoRepository;
import pe.edu.vallegrande.app.repository.JustificacionRepository;
import pe.edu.vallegrande.app.service.CursoService;
import pe.edu.vallegrande.app.util.FiltroEstado;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursos;
    private final AsistenciaEstudianteRepository asistencias;
    private final JustificacionRepository justificaciones;
    private final Entidades entidades;

    public CursoServiceImpl(CursoRepository cursos, AsistenciaEstudianteRepository asistencias, JustificacionRepository justificaciones, Entidades entidades) {
        this.cursos = cursos;
        this.asistencias = asistencias;
        this.justificaciones = justificaciones;
        this.entidades = entidades;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CursoResponse> listar(Long gradoSeccionId, Long docenteId, String estado) {
        return cursos.listar(gradoSeccionId, docenteId, FiltroEstado.valor(estado)).stream().map(CursoResponse::de).toList();
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
        if (cursos.existsByNombreIgnoreCaseAndGradoSeccionId(nombre, request.gradoSeccionId())) {
            throw ApiException.conflicto("El curso " + nombre + " ya existe en esa aula.");
        }
        return CursoResponse.de(cursos.save(new Curso(nombre, aula(request.gradoSeccionId()), docente(request.docenteId()))));
    }

    @Override
    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest request) {
        Curso curso = entidades.curso(id);
        if (!curso.esActivo()) {
            throw ApiException.conflicto("El curso está eliminado; restáuralo antes de editarlo.");
        }
        String nombre = Textos.limpiar(request.nombre());
        if (cursos.existsByNombreIgnoreCaseAndGradoSeccionIdAndIdNot(nombre, request.gradoSeccionId(), id)) {
            throw ApiException.conflicto("El curso " + nombre + " ya existe en esa aula.");
        }
        GradoSeccion aula = aula(request.gradoSeccionId());
        if (!aula.getId().equals(curso.getGradoSeccion().getId()) && asistencias.existsByCursoId(id)) {
            throw ApiException.conflicto("No se puede cambiar el aula de un curso que ya tiene asistencia registrada.");
        }
        curso.setNombre(nombre);
        curso.setGradoSeccion(aula);
        curso.setDocente(docente(request.docenteId()));
        return CursoResponse.de(curso);
    }

    @Override
    @Transactional
    public CursoResponse eliminar(Long id) {
        Curso curso = entidades.curso(id);
        if (!curso.esActivo()) {
            throw ApiException.conflicto("El curso ya está eliminado.");
        }
        curso.setEstado(FiltroEstado.INACTIVO);
        return CursoResponse.de(curso);
    }

    @Override
    @Transactional
    public CursoResponse restaurar(Long id) {
        Curso curso = entidades.curso(id);
        if (curso.esActivo()) {
            throw ApiException.conflicto("El curso ya está activo.");
        }
        if (!curso.getGradoSeccion().esActivo()) {
            throw ApiException.conflicto("El aula " + curso.getGradoSeccion().getNombre() + " está eliminada; restáurala primero.");
        }
        if (!curso.getDocente().esActivo()) {
            throw ApiException.conflicto("El docente " + curso.getDocente().getNombreCompleto() + " está inactivo; restáuralo o asigna otro docente.");
        }
        curso.setEstado(FiltroEstado.ACTIVO);
        return CursoResponse.de(curso);
    }

    /** Solo se crean o mueven cursos a aulas activas. */
    private GradoSeccion aula(Long id) {
        GradoSeccion aula = entidades.gradoSeccion(id);
        if (!aula.esActivo()) {
            throw ApiException.conflicto("El aula " + aula.getNombre() + " está eliminada; restáurala antes de asignarle cursos.");
        }
        return aula;
    }

    /** Solo personal activo con cargo Docente puede dictar un curso. */
    private Docente docente(Long id) {
        Docente docente = entidades.docente(id);
        if (!docente.esActivo()) {
            throw ApiException.conflicto("El docente está inactivo; restáuralo antes de asignarle cursos.");
        }
        if (!docente.esDocente()) {
            throw ApiException.validacion("Solo el personal con cargo Docente puede dictar cursos.");
        }
        return docente;
    }
}

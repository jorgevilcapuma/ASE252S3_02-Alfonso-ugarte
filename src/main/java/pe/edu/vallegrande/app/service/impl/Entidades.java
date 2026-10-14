package pe.edu.vallegrande.app.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.repository.AsignacionRepository;
import pe.edu.vallegrande.app.repository.CursoRepository;
import pe.edu.vallegrande.app.repository.DocenteRepository;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.repository.SeccionRepository;
import pe.edu.vallegrande.app.repository.UsuarioRepository;
import pe.edu.vallegrande.app.util.Calendario;

/** Búsquedas por id compartidas por los servicios, con un 404 de mensaje claro. */
@Component
@RequiredArgsConstructor
class Entidades {

    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final SeccionRepository secciones;
    private final AsignacionRepository asignaciones;
    private final DocenteRepository docentes;
    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;
    private final Calendario calendario;

    Estudiante estudiante(Long id) {
        return estudiantes.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el estudiante con id " + id + "."));
    }

    Seccion seccion(Long id) {
        return secciones.buscarConGrado(id).orElseThrow(() -> ApiException.noEncontrado("No existe la sección con id " + id + "."));
    }

    Asignacion asignacion(Long id) {
        return asignaciones.buscarCompleta(id).orElseThrow(() -> ApiException.noEncontrado("No existe el curso con id " + id + "."));
    }

    Docente docente(Long id) {
        return docentes.buscar(id).orElseThrow(() -> ApiException.noEncontrado("No existe el docente con id " + id + "."));
    }

    Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el usuario con id " + id + "."));
    }

    Curso curso(Long id) {
        return cursos.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el curso con id " + id + "."));
    }

    /** Usuario que realiza la operación (null para un alumno que ingresó con DNI). */
    Usuario usuarioOpcional(Long id) {
        return id == null ? null : usuarios.findById(id).orElse(null);
    }

    /** Matrícula activa del año actual o 409 si el estudiante no está matriculado. */
    Matricula matriculaActiva(Long estudianteId) {
        Matricula m = matriculas.buscarDelAnio(estudianteId, calendario.anioActual())
                .orElseThrow(() -> ApiException.conflicto("El estudiante no tiene matrícula en el año " + calendario.anioActual() + "."));
        if (m.getEstado() != EstadoMatricula.ACTIVA) {
            throw ApiException.conflicto("La matrícula del estudiante está retirada.");
        }
        return m;
    }

    /** Alumnos matriculados por sección en el año actual. */
    Map<Long, Long> totalesPorSeccion() {
        Map<Long, Long> totales = new HashMap<>();
        for (Object[] fila : matriculas.contarPorSeccion(calendario.anioActual(), EstadoMatricula.ACTIVA)) {
            totales.put((Long) fila[0], (Long) fila[1]);
        }
        return totales;
    }
}

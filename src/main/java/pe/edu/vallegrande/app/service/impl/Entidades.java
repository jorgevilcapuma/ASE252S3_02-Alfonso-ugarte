package pe.edu.vallegrande.app.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.repository.CursoRepository;
import pe.edu.vallegrande.app.repository.DocenteRepository;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.repository.GradoSeccionRepository;
import pe.edu.vallegrande.app.repository.UsuarioRepository;

/** Búsquedas por id compartidas por los servicios, con un 404 de mensaje claro. */
@Component
class Entidades {

    private final EstudianteRepository estudiantes;
    private final GradoSeccionRepository gradosSecciones;
    private final DocenteRepository docentes;
    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;

    Entidades(EstudianteRepository estudiantes, GradoSeccionRepository gradosSecciones, DocenteRepository docentes, UsuarioRepository usuarios, CursoRepository cursos) {
        this.estudiantes = estudiantes;
        this.gradosSecciones = gradosSecciones;
        this.docentes = docentes;
        this.usuarios = usuarios;
        this.cursos = cursos;
    }

    Estudiante estudiante(Long id) {
        return estudiantes.buscar(id).orElseThrow(() -> ApiException.noEncontrado("No existe el estudiante con id " + id + "."));
    }

    /** Estudiante activo o 409 si está retirado. */
    Estudiante estudianteActivo(Long id) {
        Estudiante e = estudiante(id);
        if (!e.esActivo()) {
            throw ApiException.conflicto("El estudiante está retirado; restáuralo antes de registrar su asistencia.");
        }
        return e;
    }

    GradoSeccion gradoSeccion(Long id) {
        return gradosSecciones.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el grado y sección con id " + id + "."));
    }

    Docente docente(Long id) {
        return docentes.buscar(id).orElseThrow(() -> ApiException.noEncontrado("No existe el docente o personal con id " + id + "."));
    }

    Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el usuario con id " + id + "."));
    }

    Curso curso(Long id) {
        return cursos.buscar(id).orElseThrow(() -> ApiException.noEncontrado("No existe el curso con id " + id + "."));
    }

    /** Usuario que realiza la operación (null para un alumno que ingresó con DNI). */
    Usuario usuarioOpcional(Long id) {
        return id == null ? null : usuarios.findById(id).orElse(null);
    }

    /** Estudiantes activos por aula. */
    Map<Long, Long> totalesPorAula() {
        Map<Long, Long> totales = new HashMap<>();
        for (Object[] fila : estudiantes.contarActivosPorAula()) {
            totales.put((Long) fila[0], (Long) fila[1]);
        }
        return totales;
    }
}

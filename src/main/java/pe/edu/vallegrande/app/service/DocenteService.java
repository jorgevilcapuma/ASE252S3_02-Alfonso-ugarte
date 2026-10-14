package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.DocenteRequest;
import pe.edu.vallegrande.app.dto.request.PerfilDocenteRequest;
import pe.edu.vallegrande.app.dto.response.AlumnoCursoResponse;
import pe.edu.vallegrande.app.dto.response.DocenteResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.AlertaCursoResponse;

public interface DocenteService {

    List<DocenteResponse> listar();

    DocenteResponse obtener(Long id);

    DocenteResponse crear(DocenteRequest request);

    DocenteResponse actualizar(Long id, DocenteRequest request);

    void eliminar(Long id);

    /** "Mi perfil": nombres, apellidos y teléfono. */
    DocenteResponse actualizarPerfil(Long id, PerfilDocenteRequest request);

    /** "Mis estudiantes": alumnos de sus cursos con su porcentaje de asistencia en cada curso. */
    List<AlumnoCursoResponse> estudiantes(Long docenteId, Long cursoId);

    /** Alumnos en riesgo en sus cursos (faltas consecutivas o porcentaje bajo). */
    List<AlertaCursoResponse> alertas(Long docenteId);
}

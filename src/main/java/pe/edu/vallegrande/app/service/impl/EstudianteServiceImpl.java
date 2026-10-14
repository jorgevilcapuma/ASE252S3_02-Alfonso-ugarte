package pe.edu.vallegrande.app.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.ContactoRequest;
import pe.edu.vallegrande.app.dto.request.EstudianteRequest;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.repository.AsistenciaCursoRepository;
import pe.edu.vallegrande.app.repository.AsistenciaDiariaRepository;
import pe.edu.vallegrande.app.repository.AvisoFamiliaRepository;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.repository.JustificacionRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.service.EstudianteService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class EstudianteServiceImpl implements EstudianteService {

    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final AsistenciaDiariaRepository asistenciaDiaria;
    private final AsistenciaCursoRepository asistenciaCurso;
    private final JustificacionRepository justificaciones;
    private final AvisoFamiliaRepository avisos;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse obtener(Long id) {
        return respuesta(entidades.estudiante(id));
    }

    @Override
    @Transactional
    public EstudianteResponse actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = entidades.estudiante(id);
        if (estudiantes.existsByDniAndIdNot(request.dni(), id)) {
            throw ApiException.conflicto("Ya existe otro estudiante con el DNI " + request.dni() + ".");
        }
        estudiante.setNombres(Textos.limpiar(request.nombres()));
        estudiante.setApellidos(Textos.limpiar(request.apellidos()));
        estudiante.setDni(request.dni());
        if (request.genero() != null) {
            estudiante.setGenero(Textos.limpiar(request.genero()));
        }
        estudiante.setTelefono(Textos.limpiar(request.telefono()));
        estudiante.setCorreo(Textos.correo(request.correo()));
        return respuesta(estudiante);
    }

    @Override
    @Transactional
    public EstudianteResponse actualizarContacto(Long id, ContactoRequest request) {
        Estudiante estudiante = entidades.estudiante(id);
        estudiante.setTelefono(Textos.limpiar(request.telefono()));
        estudiante.setCorreo(Textos.correo(request.correo()));
        return respuesta(estudiante);
    }

    /** Solo sin historial: con asistencias o justificaciones se debe retirar la matrícula. */
    @Override
    @Transactional
    public void eliminar(Long id) {
        Estudiante estudiante = entidades.estudiante(id);
        if (asistenciaDiaria.existsByEstudianteId(id) || asistenciaCurso.existsByEstudianteId(id) || justificaciones.existsByEstudianteId(id)) {
            throw ApiException.conflicto("El estudiante tiene historial de asistencia o justificaciones. Retira su matrícula para conservarlo.");
        }
        matriculas.deleteAll(matriculas.findByEstudianteId(id));
        avisos.deleteByEstudianteId(id);
        estudiantes.delete(estudiante);
    }

    private EstudianteResponse respuesta(Estudiante estudiante) {
        Matricula matricula = matriculas.buscarDelAnio(estudiante.getId(), calendario.anioActual()).orElse(null);
        return EstudianteResponse.de(estudiante, matricula);
    }
}

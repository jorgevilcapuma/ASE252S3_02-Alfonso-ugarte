package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.dto.request.JustificacionRequest;
import pe.edu.vallegrande.app.dto.request.RevisionJustificacionRequest;
import pe.edu.vallegrande.app.dto.response.JustificacionResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.AsistenciaEstudiante;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Justificacion;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.repository.AsistenciaEstudianteRepository;
import pe.edu.vallegrande.app.repository.JustificacionRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.JustificacionService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

/**
 * Una justificación se aplica a la asistencia del mismo día: la del curso si indica uno,
 * o la asistencia diaria del aula si no.
 */
@Service
public class JustificacionServiceImpl implements JustificacionService {

    private final JustificacionRepository justificaciones;
    private final AsistenciaEstudianteRepository asistencias;
    private final Calendario calendario;
    private final Entidades entidades;

    public JustificacionServiceImpl(JustificacionRepository justificaciones, AsistenciaEstudianteRepository asistencias, Calendario calendario, Entidades entidades) {
        this.justificaciones = justificaciones;
        this.asistencias = asistencias;
        this.calendario = calendario;
        this.entidades = entidades;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JustificacionResponse> listar(EstadoJustificacion estado, Long estudianteId, Long cursoId, Long docenteId,
                                              LocalDate desde, LocalDate hasta, SesionUsuario sesion) {
        Long alumno = sesion.esAlumno() ? sesion.estudianteId() : estudianteId;
        return justificaciones.buscar(estado, alumno, cursoId, docenteId, desde, hasta).stream()
                .map(JustificacionResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JustificacionResponse obtener(Long id, SesionUsuario sesion) {
        Justificacion j = justificacion(id);
        sesion.verificarEstudiante(j.getEstudiante().getId());
        return JustificacionResponse.de(j);
    }

    @Override
    @Transactional
    public JustificacionResponse crear(JustificacionRequest request, SesionUsuario sesion) {
        Long estudianteId = sesion.esAlumno() ? sesion.estudianteId() : request.estudianteId();
        if (estudianteId == null) {
            throw ApiException.validacion("Selecciona el estudiante.");
        }
        Estudiante estudiante = entidades.estudianteActivo(estudianteId);
        validarFecha(request.fecha());
        Curso curso = cursoDelEstudiante(request.cursoId(), estudiante);
        Long cursoId = curso == null ? null : curso.getId();

        if (justificaciones.existeSolicitud(estudianteId, request.fecha(), cursoId, EstadoJustificacion.PENDIENTE)) {
            throw ApiException.conflicto("Ya existe una solicitud pendiente para esa fecha.");
        }
        if (justificaciones.existeSolicitud(estudianteId, request.fecha(), cursoId, EstadoJustificacion.APROBADA)) {
            throw ApiException.conflicto("Esa inasistencia ya fue justificada.");
        }

        Justificacion j = new Justificacion();
        j.setEstudiante(estudiante);
        j.setCurso(curso);
        j.setFecha(request.fecha());
        j.setMotivo(Textos.limpiar(request.motivo()));
        j.setDetalle(Textos.limpiar(request.detalle()));
        j.setArchivoNombre(Textos.limpiar(request.archivoNombre()));
        j.setEstado(EstadoJustificacion.PENDIENTE);
        j.setEstadoPrevio(estadoActual(estudianteId, cursoId, request.fecha()));
        j.setFechaSolicitud(calendario.ahora());
        return JustificacionResponse.de(justificaciones.save(j));
    }

    @Override
    @Transactional
    public JustificacionResponse actualizar(Long id, JustificacionRequest request, SesionUsuario sesion) {
        Justificacion j = justificacion(id);
        sesion.verificarEstudiante(j.getEstudiante().getId());
        exigirPendiente(j, "editar");
        validarFecha(request.fecha());
        Curso curso = cursoDelEstudiante(request.cursoId(), j.getEstudiante());
        Long cursoId = curso == null ? null : curso.getId();
        boolean cambiaDia = !j.getFecha().equals(request.fecha())
                || !Objects.equals(cursoId, j.getCurso() == null ? null : j.getCurso().getId());
        if (cambiaDia && justificaciones.existeSolicitud(j.getEstudiante().getId(), request.fecha(), cursoId, EstadoJustificacion.PENDIENTE)) {
            throw ApiException.conflicto("Ya existe una solicitud pendiente para esa fecha.");
        }
        if (cambiaDia) {
            j.setEstadoPrevio(estadoActual(j.getEstudiante().getId(), cursoId, request.fecha()));
        }
        j.setFecha(request.fecha());
        j.setCurso(curso);
        j.setMotivo(Textos.limpiar(request.motivo()));
        j.setDetalle(Textos.limpiar(request.detalle()));
        j.setArchivoNombre(Textos.limpiar(request.archivoNombre()));
        return JustificacionResponse.de(j);
    }

    @Override
    @Transactional
    public JustificacionResponse revisar(Long id, RevisionJustificacionRequest request, SesionUsuario sesion) {
        Justificacion j = justificacion(id);
        exigirPendiente(j, "revisar");
        if (request.estado() == EstadoJustificacion.PENDIENTE) {
            throw ApiException.validacion("La revisión debe aprobar u observar la solicitud.");
        }
        String motivoRevision = Textos.limpiar(request.observacion());
        if (request.estado() == EstadoJustificacion.OBSERVADA && (motivoRevision == null || motivoRevision.length() < 5)) {
            throw ApiException.validacion("Para observar la solicitud, escribe el motivo (al menos 5 caracteres).");
        }
        if (sesion.esDocente() && j.getCurso() != null && !j.getCurso().getDocente().getId().equals(sesion.docenteId())) {
            throw ApiException.prohibido("Solo el docente del curso puede revisar esta justificación.");
        }
        Usuario revisor = entidades.usuarioOpcional(sesion.usuarioId());
        j.setEstado(request.estado());
        j.setObservacion(Textos.limpiar(request.observacion()));
        j.setRevisadoPor(revisor);
        j.setFechaRevision(calendario.ahora());

        EstadoAsistencia previo = j.getEstadoPrevio();
        if (request.estado() == EstadoJustificacion.APROBADA) {
            if (previo != EstadoAsistencia.PRESENTE) {
                guardarAsistencia(j, EstadoAsistencia.JUSTIFICADO, revisor);
            }
        } else {
            boolean previoValido = previo != null && previo != EstadoAsistencia.JUSTIFICADO && previo != EstadoAsistencia.PENDIENTE;
            guardarAsistencia(j, previoValido ? previo : EstadoAsistencia.FALTA, revisor);
        }
        return JustificacionResponse.de(j);
    }

    @Override
    @Transactional
    public void eliminar(Long id, SesionUsuario sesion) {
        Justificacion j = justificacion(id);
        sesion.verificarEstudiante(j.getEstudiante().getId());
        exigirPendiente(j, "eliminar");
        justificaciones.delete(j);
    }

    private EstadoAsistencia estadoActual(Long estudianteId, Long cursoId, LocalDate fecha) {
        return asistencias.buscar(estudianteId, cursoId, fecha).map(AsistenciaEstudiante::getEstado).orElse(EstadoAsistencia.PENDIENTE);
    }

    private void guardarAsistencia(Justificacion j, EstadoAsistencia estado, Usuario revisor) {
        Long cursoId = j.getCurso() == null ? null : j.getCurso().getId();
        AsistenciaEstudiante registro = asistencias.buscar(j.getEstudiante().getId(), cursoId, j.getFecha()).orElseGet(() -> {
            AsistenciaEstudiante nuevo = new AsistenciaEstudiante();
            nuevo.setEstudiante(j.getEstudiante());
            nuevo.setCurso(j.getCurso());
            nuevo.setFecha(j.getFecha());
            return nuevo;
        });
        registro.setEstado(estado);
        registro.setRegistradoPor(revisor);
        registro.setActualizadoEn(calendario.ahora());
        asistencias.save(registro);
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(calendario.hoy())) {
            throw ApiException.validacion("No se puede justificar una fecha futura.");
        }
        if (Calendario.esFinDeSemana(fecha)) {
            throw ApiException.validacion("La fecha a justificar debe ser un día de clases (lunes a viernes).");
        }
    }

    private Curso cursoDelEstudiante(Long cursoId, Estudiante estudiante) {
        if (cursoId == null) {
            return null;
        }
        Curso curso = entidades.curso(cursoId);
        if (!curso.getGradoSeccion().getId().equals(estudiante.getGradoSeccion().getId())) {
            throw ApiException.validacion("El curso indicado no corresponde al aula del estudiante.");
        }
        return curso;
    }

    private static void exigirPendiente(Justificacion j, String accion) {
        if (j.getEstado() != EstadoJustificacion.PENDIENTE) {
            throw ApiException.conflicto("La justificación ya fue " + j.getEstado().getValor().toLowerCase() + "; no se puede " + accion + ".");
        }
    }

    private Justificacion justificacion(Long id) {
        return justificaciones.buscarCompleta(id).orElseThrow(() -> ApiException.noEncontrado("No existe la justificación con id " + id + "."));
    }
}

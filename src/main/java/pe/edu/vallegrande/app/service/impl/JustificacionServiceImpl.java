package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.JustificacionRequest;
import pe.edu.vallegrande.app.dto.request.RevisionJustificacionRequest;
import pe.edu.vallegrande.app.dto.response.JustificacionResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.AsistenciaDiaria;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Justificacion;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoAsistenciaCurso;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.repository.AsistenciaCursoRepository;
import pe.edu.vallegrande.app.repository.AsistenciaDiariaRepository;
import pe.edu.vallegrande.app.repository.JustificacionRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.JustificacionService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class JustificacionServiceImpl implements JustificacionService {

    private final JustificacionRepository justificaciones;
    private final AsistenciaDiariaRepository asistenciaDiaria;
    private final AsistenciaCursoRepository asistenciaCurso;
    private final Calendario calendario;
    private final Entidades entidades;

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
        Estudiante estudiante = entidades.estudiante(estudianteId);
        Matricula matricula = entidades.matriculaActiva(estudianteId);
        validarFecha(request.fecha());
        Asignacion asignacion = cursoDelEstudiante(request.cursoId(), matricula);
        Long asignacionId = asignacion == null ? null : asignacion.getId();

        if (justificaciones.existeSolicitud(estudianteId, request.fecha(), asignacionId, EstadoJustificacion.PENDIENTE)) {
            throw ApiException.conflicto("Ya existe una solicitud pendiente para esa fecha.");
        }
        if (justificaciones.existeSolicitud(estudianteId, request.fecha(), asignacionId, EstadoJustificacion.APROBADA)) {
            throw ApiException.conflicto("Esa inasistencia ya fue justificada.");
        }

        Justificacion j = new Justificacion();
        j.setEstudiante(estudiante);
        j.setAsignacion(asignacion);
        j.setFecha(request.fecha());
        j.setMotivo(Textos.limpiar(request.motivo()));
        j.setDetalle(Textos.limpiar(request.detalle()));
        j.setArchivoNombre(Textos.limpiar(request.archivoNombre()));
        j.setEstado(EstadoJustificacion.PENDIENTE);
        j.setEstadoPrevio(asistenciaDiaria.findByEstudianteIdAndFecha(estudianteId, request.fecha())
                .map(AsistenciaDiaria::getEstado).orElse(EstadoAsistencia.PENDIENTE));
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
        Asignacion asignacion = cursoDelEstudiante(request.cursoId(), entidades.matriculaActiva(j.getEstudiante().getId()));
        Long asignacionId = asignacion == null ? null : asignacion.getId();
        boolean cambiaDia = !j.getFecha().equals(request.fecha()) || !java.util.Objects.equals(asignacionId,
                j.getAsignacion() == null ? null : j.getAsignacion().getId());
        if (cambiaDia && justificaciones.existeSolicitud(j.getEstudiante().getId(), request.fecha(), asignacionId, EstadoJustificacion.PENDIENTE)) {
            throw ApiException.conflicto("Ya existe una solicitud pendiente para esa fecha.");
        }
        if (cambiaDia) {
            j.setEstadoPrevio(asistenciaDiaria.findByEstudianteIdAndFecha(j.getEstudiante().getId(), request.fecha())
                    .map(AsistenciaDiaria::getEstado).orElse(EstadoAsistencia.PENDIENTE));
        }
        j.setFecha(request.fecha());
        j.setAsignacion(asignacion);
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
        if (sesion.esDocente() && j.getAsignacion() != null && !j.getAsignacion().getDocente().getId().equals(sesion.docenteId())) {
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
                guardarDia(j, EstadoAsistencia.JUSTIFICADO, revisor);
            }
            actualizarCurso(j, EstadoAsistenciaCurso.JUSTIFICADO);
        } else {
            boolean previoValido = previo != null && previo != EstadoAsistencia.JUSTIFICADO && previo != EstadoAsistencia.PENDIENTE;
            guardarDia(j, previoValido ? previo : EstadoAsistencia.FALTA, revisor);
            actualizarCurso(j, EstadoAsistenciaCurso.FALTA);
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

    private void guardarDia(Justificacion j, EstadoAsistencia estado, Usuario revisor) {
        if (Calendario.esFinDeSemana(j.getFecha())) {
            return;
        }
        AsistenciaDiaria dia = asistenciaDiaria.findByEstudianteIdAndFecha(j.getEstudiante().getId(), j.getFecha()).orElseGet(() -> {
            AsistenciaDiaria nuevo = new AsistenciaDiaria();
            nuevo.setEstudiante(j.getEstudiante());
            nuevo.setFecha(j.getFecha());
            return nuevo;
        });
        dia.setEstado(estado);
        dia.setRegistradoPor(revisor);
        dia.setActualizadoEn(calendario.ahora());
        asistenciaDiaria.save(dia);
    }

    /**
     * Si la justificación es de un curso con sesión registrada: aprobar convierte la falta o tardanza
     * en justificada; observar devuelve a falta un registro que se había justificado.
     */
    private void actualizarCurso(Justificacion j, EstadoAsistenciaCurso nuevo) {
        if (j.getAsignacion() == null) {
            return;
        }
        asistenciaCurso.buscar(j.getEstudiante().getId(), j.getAsignacion().getId(), j.getFecha()).ifPresent(r -> {
            boolean aprobar = nuevo == EstadoAsistenciaCurso.JUSTIFICADO
                    && (r.getEstado() == EstadoAsistenciaCurso.FALTA || r.getEstado() == EstadoAsistenciaCurso.TARDANZA);
            boolean observar = nuevo == EstadoAsistenciaCurso.FALTA && r.getEstado() == EstadoAsistenciaCurso.JUSTIFICADO;
            if (aprobar || observar) {
                r.setEstado(nuevo);
            }
        });
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(calendario.hoy())) {
            throw ApiException.validacion("No se puede justificar una fecha futura.");
        }
        if (Calendario.esFinDeSemana(fecha)) {
            throw ApiException.validacion("La fecha a justificar debe ser un día de clases (lunes a viernes).");
        }
    }

    private Asignacion cursoDelEstudiante(Long cursoId, Matricula matricula) {
        if (cursoId == null) {
            return null;
        }
        Asignacion asignacion = entidades.asignacion(cursoId);
        if (!asignacion.getSeccion().getId().equals(matricula.getSeccion().getId())) {
            throw ApiException.validacion("El curso indicado no corresponde a la sección del estudiante.");
        }
        return asignacion;
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

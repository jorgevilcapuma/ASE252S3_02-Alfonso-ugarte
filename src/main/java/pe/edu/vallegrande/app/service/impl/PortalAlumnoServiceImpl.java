package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.response.AsignacionResponse;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.AvisoAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.DiaAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.InicioAlumnoResponse;
import pe.edu.vallegrande.app.dto.response.PortalAlumnoResponses.ResumenAsistenciaResponse;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.AsistenciaCurso;
import pe.edu.vallegrande.app.model.AsistenciaDiaria;
import pe.edu.vallegrande.app.model.Comunicado;
import pe.edu.vallegrande.app.model.Horario;
import pe.edu.vallegrande.app.model.Justificacion;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.model.enums.Prioridad;
import pe.edu.vallegrande.app.repository.AsignacionRepository;
import pe.edu.vallegrande.app.repository.AsistenciaCursoRepository;
import pe.edu.vallegrande.app.repository.AsistenciaDiariaRepository;
import pe.edu.vallegrande.app.repository.AvisoFamiliaRepository;
import pe.edu.vallegrande.app.repository.ComunicadoRepository;
import pe.edu.vallegrande.app.repository.InstitucionRepository;
import pe.edu.vallegrande.app.repository.JustificacionRepository;
import pe.edu.vallegrande.app.service.PortalAlumnoService;
import pe.edu.vallegrande.app.util.Calendario;

@Service
@RequiredArgsConstructor
public class PortalAlumnoServiceImpl implements PortalAlumnoService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DIAS_POR_DEFECTO = 120;

    private final AsignacionRepository asignaciones;
    private final AsistenciaCursoRepository asistenciaCurso;
    private final AsistenciaDiariaRepository asistenciaDiaria;
    private final JustificacionRepository justificaciones;
    private final AvisoFamiliaRepository avisos;
    private final ComunicadoRepository comunicados;
    private final InstitucionRepository institucion;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse perfil(Long estudianteId) {
        return EstudianteResponse.de(entidades.matriculaActiva(estudianteId));
    }

    @Override
    @Transactional(readOnly = true)
    public InicioAlumnoResponse inicio(Long estudianteId) {
        Matricula matricula = entidades.matriculaActiva(estudianteId);
        long pendientes = justificaciones.buscar(EstadoJustificacion.PENDIENTE, estudianteId, null, null, null, null).size();
        return new InicioAlumnoResponse(EstudianteResponse.de(matricula),
                institucion.findFirstByOrderByIdAsc().map(i -> i.getNombre()).orElse(""),
                calendario.hoy(), estadoDelDia(estudianteId, calendario.hoy()), resumen(estudianteId, null, null, null),
                pendientes, asignaciones.listar(null, matricula.getSeccion().getId()).size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionResponse> cursos(Long estudianteId) {
        Matricula matricula = entidades.matriculaActiva(estudianteId);
        long total = entidades.totalesPorSeccion().getOrDefault(matricula.getSeccion().getId(), 0L);
        return asignaciones.listar(null, matricula.getSeccion().getId()).stream().map(a -> AsignacionResponse.de(a, total)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiaAlumnoResponse> asistencia(Long estudianteId, Long cursoId, LocalDate desde, LocalDate hasta, Integer limite) {
        entidades.matriculaActiva(estudianteId);
        LocalDate fin = hasta == null || hasta.isAfter(calendario.hoy()) ? calendario.hoy() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        List<Justificacion> observadas = justificaciones.buscar(EstadoJustificacion.OBSERVADA, estudianteId, null, null, inicio, fin);

        List<DiaAlumnoResponse> dias = new ArrayList<>();
        Set<LocalDate> conRegistroDeCurso = new HashSet<>();
        for (AsistenciaCurso r : asistenciaCurso.historialAlumno(estudianteId, cursoId, inicio, fin)) {
            LocalDate fecha = r.getSesion().getFecha();
            Asignacion a = r.getSesion().getAsignacion();
            conRegistroDeCurso.add(fecha);
            String observacion = r.getObservacion() != null ? r.getObservacion() : observacion(observadas, fecha, a.getId());
            dias.add(new DiaAlumnoResponse(fecha, r.getEstado().comoDiario(), a.getId(), a.getCurso().getNombre(),
                    "Prof. " + a.getDocente().getNombreCompleto(), hora(a, fecha), observacion));
        }
        if (cursoId == null) {
            // Días con registro diario por aula pero sin registro por curso.
            List<AsistenciaDiaria> diarios = asistenciaDiaria.findByEstudianteIdAndFechaBetweenOrderByFechaDesc(estudianteId, inicio, fin);
            for (AsistenciaDiaria d : diarios) {
                if (!conRegistroDeCurso.contains(d.getFecha())) {
                    dias.add(new DiaAlumnoResponse(d.getFecha(), d.getEstado(), null, null, null, null, observacion(observadas, d.getFecha(), null)));
                }
            }
            LocalDate hoy = calendario.hoy();
            boolean hoyRegistrado = dias.stream().anyMatch(d -> d.fecha().equals(hoy));
            if (!hoyRegistrado && !Calendario.esFinDeSemana(hoy) && !fin.isBefore(hoy)) {
                dias.add(new DiaAlumnoResponse(hoy, EstadoAsistencia.PENDIENTE, null, null, null, null, null));
            }
        }
        dias.sort(Comparator.comparing(DiaAlumnoResponse::fecha).reversed()
                .thenComparing(d -> d.cursoNombre() == null ? "" : d.cursoNombre()));
        int maximo = limite != null ? limite : (desde == null ? 20 : Integer.MAX_VALUE);
        return dias.size() > maximo ? dias.subList(0, Math.max(0, maximo)) : dias;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenAsistenciaResponse resumen(Long estudianteId, Long cursoId, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde == null ? calendario.hoy().minusDays(DIAS_POR_DEFECTO) : desde;
        List<DiaAlumnoResponse> contados = asistencia(estudianteId, cursoId, inicio, hasta, Integer.MAX_VALUE).stream()
                .filter(d -> d.estado() != EstadoAsistencia.PENDIENTE).toList();
        int presentes = contar(contados, EstadoAsistencia.PRESENTE);
        int tardanzas = contar(contados, EstadoAsistencia.TARDANZA);
        int faltas = contar(contados, EstadoAsistencia.FALTA);
        int justificadas = contar(contados, EstadoAsistencia.JUSTIFICADO);
        int total = contados.size();
        return new ResumenAsistenciaResponse(presentes, tardanzas, faltas, justificadas, total,
                total == 0 ? 0 : Math.round((presentes + tardanzas + justificadas) * 100f / total),
                total == 0 ? 0 : Math.round(faltas * 100f / total));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisoAlumnoResponse> alertas(Long estudianteId) {
        entidades.matriculaActiva(estudianteId);
        LocalDate hoy = calendario.hoy();
        List<AvisoAlumnoResponse> lista = new ArrayList<>();
        EstadoAsistencia estadoHoy = estadoDelDia(estudianteId, hoy);
        if (estadoHoy == EstadoAsistencia.FALTA) {
            lista.add(new AvisoAlumnoResponse("falta-hoy", hoy, "Falta registrada",
                    "Revisa el registro de hoy y comunícate con el colegio si necesitas aclararlo.", "asistencia", false));
        } else if (estadoHoy == EstadoAsistencia.TARDANZA) {
            lista.add(new AvisoAlumnoResponse("tardanza-hoy", hoy, "Tardanza registrada",
                    "Se registró una tardanza en tu asistencia de hoy.", "asistencia", false));
        }
        ResumenAsistenciaResponse resumen = resumen(estudianteId, null, null, null);
        if (resumen.total() >= 5 && resumen.porcentajeAsistencia() < 85) {
            lista.add(new AvisoAlumnoResponse("porcentaje-bajo", hoy, "Porcentaje de asistencia bajo",
                    "Tu asistencia está en " + resumen.porcentajeAsistencia() + "%. Revisa tu historial y conversa con el colegio.", "asistencia", false));
        }
        avisos.findByEstudianteIdOrderByEnviadoEnDesc(estudianteId).stream().limit(5).forEach(a ->
                lista.add(new AvisoAlumnoResponse("aviso-" + a.getId(), a.getEnviadoEn().toLocalDate(), "Aviso enviado a tu familia",
                        "El colegio comunicó a tu familia " + a.getFaltas() + " falta(s) acumulada(s).", "asistencia", false)));
        justificaciones.buscar(null, estudianteId, null, null, null, null).stream()
                .filter(j -> j.getEstado() != EstadoJustificacion.PENDIENTE)
                .limit(10)
                .forEach(j -> lista.add(new AvisoAlumnoResponse("justificacion-" + j.getId(), j.getFecha(),
                        "Justificación " + j.getEstado().getValor().toLowerCase(),
                        j.getObservacion() != null ? j.getObservacion()
                                : j.getMotivo() + (j.getArchivoNombre() != null ? " · Sustento: " + j.getArchivoNombre() : ""),
                        "justificacion", false)));
        lista.sort(Comparator.comparing(AvisoAlumnoResponse::fecha).reversed());
        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisoAlumnoResponse> comunicaciones(Long estudianteId) {
        Matricula matricula = entidades.matriculaActiva(estudianteId);
        return comunicados.visiblesParaSeccion(matricula.getSeccion().getId()).stream()
                .map(this::comoAviso)
                .toList();
    }

    private AvisoAlumnoResponse comoAviso(Comunicado c) {
        String curso = c.getAsignacion() == null ? "Todos los cursos" : c.getAsignacion().getCurso().getNombre();
        String titulo = c.getPrioridad() == Prioridad.ALTA ? "Importante: " + c.getTitulo() : c.getTitulo();
        return new AvisoAlumnoResponse("comunicado-" + c.getId(), c.getFechaCreacion(), titulo, c.getContenido() + " · " + curso, "comunicado", false);
    }

    private EstadoAsistencia estadoDelDia(Long estudianteId, LocalDate fecha) {
        return asistenciaDiaria.findByEstudianteIdAndFecha(estudianteId, fecha)
                .map(AsistenciaDiaria::getEstado)
                .orElse(EstadoAsistencia.PENDIENTE);
    }

    private static String hora(Asignacion a, LocalDate fecha) {
        return a.getHorarios().stream()
                .filter(h -> h.getDia().getDiaJava() == fecha.getDayOfWeek())
                .min(Comparator.comparing(Horario::getHoraInicio))
                .map(h -> h.getHoraInicio().format(HORA) + "–" + h.getHoraFin().format(HORA))
                .orElse(null);
    }

    private static String observacion(List<Justificacion> observadas, LocalDate fecha, Long asignacionId) {
        return observadas.stream()
                .filter(j -> j.getFecha().equals(fecha))
                .filter(j -> j.getAsignacion() == null || Objects.equals(j.getAsignacion().getId(), asignacionId))
                .map(Justificacion::getObservacion)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private static int contar(List<DiaAlumnoResponse> dias, EstadoAsistencia estado) {
        return (int) dias.stream().filter(d -> d.estado() == estado).count();
    }
}

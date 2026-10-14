package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.SesionClaseRequest;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.Conteo;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.FilaAsistencia;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.ResumenMensualResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.SesionClaseResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.SesionHistorialResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.AsistenciaCurso;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.SesionClase;
import pe.edu.vallegrande.app.model.enums.EstadoAsistenciaCurso;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.repository.AsistenciaCursoRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.repository.SesionClaseRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.SesionClaseService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class SesionClaseServiceImpl implements SesionClaseService {

    private final SesionClaseRepository sesiones;
    private final AsistenciaCursoRepository registrosCurso;
    private final MatriculaRepository matriculas;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public SesionClaseResponse obtener(Long cursoId, LocalDate fecha, SesionUsuario sesion) {
        Asignacion asignacion = asignacionPermitida(cursoId, sesion);
        LocalDate dia = fecha == null ? calendario.hoy() : fecha;
        return respuesta(asignacion, dia, sesiones.buscar(cursoId, dia).orElse(null));
    }

    @Override
    @Transactional
    public SesionClaseResponse guardar(Long cursoId, LocalDate fecha, SesionClaseRequest request, SesionUsuario sesion) {
        Asignacion asignacion = asignacionPermitida(cursoId, sesion);
        calendario.validarFechaRegistrable(fecha);
        Map<Long, Estudiante> delCurso = new HashMap<>();
        alumnos(asignacion).forEach(m -> delCurso.put(m.getEstudiante().getId(), m.getEstudiante()));
        Set<Long> vistos = new HashSet<>();
        for (SesionClaseRequest.Registro r : request.registros()) {
            if (!vistos.add(r.estudianteId())) {
                throw ApiException.validacion("El alumno con id " + r.estudianteId() + " aparece más de una vez.");
            }
            if (!delCurso.containsKey(r.estudianteId())) {
                throw ApiException.validacion("El alumno con id " + r.estudianteId() + " no está matriculado en la sección de este curso.");
            }
        }

        SesionClase sesionClase = sesiones.buscar(cursoId, fecha).orElseGet(() -> {
            SesionClase nueva = new SesionClase();
            nueva.setAsignacion(asignacion);
            nueva.setFecha(fecha);
            return nueva;
        });
        sesionClase.setRegistradoPor(entidades.usuarioOpcional(sesion.usuarioId()));
        sesionClase.setActualizadoEn(calendario.ahora());

        // Se actualiza en el lugar para no violar uq_asistencia_curso_sesion_estudiante al reemplazar filas.
        Map<Long, AsistenciaCurso> actuales = new HashMap<>();
        sesionClase.getRegistros().forEach(r -> actuales.put(r.getEstudiante().getId(), r));
        sesionClase.getRegistros().removeIf(r -> !vistos.contains(r.getEstudiante().getId()));
        for (SesionClaseRequest.Registro r : request.registros()) {
            AsistenciaCurso fila = actuales.get(r.estudianteId());
            if (fila == null) {
                fila = new AsistenciaCurso();
                fila.setSesion(sesionClase);
                fila.setEstudiante(delCurso.get(r.estudianteId()));
                sesionClase.getRegistros().add(fila);
            }
            fila.setEstado(r.estado());
            fila.setObservacion(Textos.limpiar(r.observacion()));
        }
        sesiones.save(sesionClase);
        return respuesta(asignacion, fecha, sesionClase);
    }

    @Override
    @Transactional
    public void eliminar(Long cursoId, LocalDate fecha, SesionUsuario sesion) {
        asignacionPermitida(cursoId, sesion);
        SesionClase sesionClase = sesiones.buscar(cursoId, fecha)
                .orElseThrow(() -> ApiException.noEncontrado("No hay asistencia registrada para ese curso en " + fecha + "."));
        sesiones.delete(sesionClase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionHistorialResponse> historial(Long docenteId, Long cursoId, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? calendario.hoy() : hasta;
        LocalDate inicio = desde == null ? LocalDate.of(fin.getYear(), 1, 1) : desde;
        return sesiones.historial(docenteId, cursoId, inicio, fin).stream().map(s -> {
            Conteo c = conteo(s.getRegistros().stream().map(AsistenciaCurso::getEstado).toList());
            Asignacion a = s.getAsignacion();
            return new SesionHistorialResponse(s.getId(), s.getFecha(), a.getId(), a.getCurso().getNombre(),
                    a.getSeccion().getGrado().getEtiqueta() + " " + a.getSeccion().getLetra(),
                    c.asistieron(), c.tardanzas(), c.faltas(), c.justificados(), c.total());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenMensualResponse> resumenMensual(Long cursoId, String mes, SesionUsuario sesion) {
        Asignacion asignacion = asignacionPermitida(cursoId, sesion);
        YearMonth periodo = mes == null ? YearMonth.from(calendario.hoy()) : Calendario.mes(mes);
        Map<Long, List<EstadoAsistenciaCurso>> porAlumno = new HashMap<>();
        for (AsistenciaCurso r : registrosCurso.registrosDeCursos(null, cursoId, periodo.atDay(1), periodo.atEndOfMonth())) {
            porAlumno.computeIfAbsent(r.getEstudiante().getId(), k -> new ArrayList<>()).add(r.getEstado());
        }
        return alumnos(asignacion).stream().map(m -> {
            Estudiante e = m.getEstudiante();
            List<EstadoAsistenciaCurso> estados = porAlumno.getOrDefault(e.getId(), List.of());
            Conteo c = conteo(estados);
            int asistencias = c.total() - c.faltas();
            int porcentaje = c.total() == 0 ? 0 : Math.round(asistencias * 100f / c.total());
            return new ResumenMensualResponse(e.getId(), e.getNombres(), e.getApellidos(), e.getDni(), c.total(), asistencias,
                    c.tardanzas(), c.faltas(), porcentaje);
        }).toList();
    }

    /** Un docente solo gestiona sus propios cursos; el administrador, todos. */
    private Asignacion asignacionPermitida(Long cursoId, SesionUsuario sesion) {
        Asignacion asignacion = entidades.asignacion(cursoId);
        if (sesion.esDocente() && !asignacion.getDocente().getId().equals(sesion.docenteId())) {
            throw ApiException.prohibido("Este curso no está asignado a tu cuenta.");
        }
        return asignacion;
    }

    private SesionClaseResponse respuesta(Asignacion asignacion, LocalDate fecha, SesionClase sesionClase) {
        Map<Long, AsistenciaCurso> registrados = new HashMap<>();
        if (sesionClase != null) {
            sesionClase.getRegistros().forEach(r -> registrados.put(r.getEstudiante().getId(), r));
        }
        List<FilaAsistencia> filas = alumnos(asignacion).stream().map(m -> {
            Estudiante e = m.getEstudiante();
            AsistenciaCurso r = registrados.get(e.getId());
            return new FilaAsistencia(e.getId(), e.getNombres(), e.getApellidos(), e.getDni(),
                    r == null ? EstadoAsistenciaCurso.ASISTIO : r.getEstado(), r == null || r.getObservacion() == null ? "" : r.getObservacion());
        }).toList();
        return new SesionClaseResponse(asignacion.getId(), asignacion.getCurso().getNombre(),
                asignacion.getSeccion().getGrado().getEtiqueta() + " " + asignacion.getSeccion().getLetra(), fecha,
                sesionClase != null && sesionClase.getId() != null, filas,
                conteo(filas.stream().map(FilaAsistencia::estado).toList()));
    }

    private List<Matricula> alumnos(Asignacion asignacion) {
        return matriculas.listarPorSeccion(asignacion.getSeccion().getId(), calendario.anioActual(), EstadoMatricula.ACTIVA);
    }

    private static Conteo conteo(List<EstadoAsistenciaCurso> estados) {
        int[] c = new int[4];
        estados.forEach(e -> c[e.ordinal()]++);
        return new Conteo(c[0], c[1], c[2], c[3], estados.size());
    }
}

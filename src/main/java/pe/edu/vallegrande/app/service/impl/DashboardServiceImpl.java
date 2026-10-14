package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AlertasResponse;
import pe.edu.vallegrande.app.dto.response.DashboardResponse;
import pe.edu.vallegrande.app.dto.response.DashboardResponse.NivelResumen;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.repository.AsistenciaDiariaRepository;
import pe.edu.vallegrande.app.repository.JustificacionRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.repository.NivelEducativoRepository;
import pe.edu.vallegrande.app.service.AlertaService;
import pe.edu.vallegrande.app.service.DashboardService;
import pe.edu.vallegrande.app.util.Calendario;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final NivelEducativoRepository niveles;
    private final MatriculaRepository matriculas;
    private final AsistenciaDiariaRepository asistencias;
    private final JustificacionRepository justificaciones;
    private final AlertaService alertaService;
    private final Calendario calendario;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse resumen(LocalDate fecha) {
        LocalDate dia = fecha == null ? calendario.hoy() : fecha;
        int anio = calendario.anioActual();

        Map<String, Long> estudiantesPorNivel = new HashMap<>();
        matriculas.contarPorNivel(anio, EstadoMatricula.ACTIVA).forEach(f -> estudiantesPorNivel.put((String) f[0], (Long) f[1]));
        Map<String, Long> registradosPorNivel = new HashMap<>();
        Map<EstadoAsistencia, Integer> porEstado = new EnumMap<>(EstadoAsistencia.class);
        for (Object[] f : asistencias.contarDelDiaPorNivel(dia, anio, EstadoMatricula.ACTIVA)) {
            long cantidad = (Long) f[2];
            registradosPorNivel.merge((String) f[0], cantidad, Long::sum);
            porEstado.merge((EstadoAsistencia) f[1], (int) cantidad, Integer::sum);
        }

        List<NivelResumen> porNivel = niveles.findAllByOrderByOrdenAsc().stream().map(n -> {
            long total = estudiantesPorNivel.getOrDefault(n.getNombre(), 0L);
            long registrados = registradosPorNivel.getOrDefault(n.getNombre(), 0L);
            return new NivelResumen(n.getNombre(), total, registrados, porcentaje(registrados, total));
        }).toList();
        long totalEstudiantes = porNivel.stream().mapToLong(NivelResumen::estudiantes).sum();
        long totalRegistrados = porNivel.stream().mapToLong(NivelResumen::registrados).sum();
        AlertasResponse alertas = alertaService.alertas();

        return new DashboardResponse(dia, totalEstudiantes, porcentaje(totalRegistrados, totalEstudiantes),
                porEstado.getOrDefault(EstadoAsistencia.PRESENTE, 0), porEstado.getOrDefault(EstadoAsistencia.TARDANZA, 0),
                porEstado.getOrDefault(EstadoAsistencia.FALTA, 0), porEstado.getOrDefault(EstadoAsistencia.JUSTIFICADO, 0),
                justificaciones.countByEstado(EstadoJustificacion.PENDIENTE), alertas.criticas(), alertas.preventivas(),
                porNivel, alertaService.tendencia(7));
    }

    private static int porcentaje(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 100f / total);
    }
}

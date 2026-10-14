package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.config.PropiedadesApp;
import pe.edu.vallegrande.app.dto.request.AvisoFamiliaRequest;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AlertasResponse;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AvisoResponse;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.FaltasDia;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.ParametrosAlerta;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.ResumenAlertaResponse;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.AvisoFamilia;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.repository.AsistenciaDiariaRepository;
import pe.edu.vallegrande.app.repository.AvisoFamiliaRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AlertaService;
import pe.edu.vallegrande.app.util.Calendario;

@Service
@RequiredArgsConstructor
public class AlertaServiceImpl implements AlertaService {

    private final AsistenciaDiariaRepository asistencias;
    private final MatriculaRepository matriculas;
    private final AvisoFamiliaRepository avisos;
    private final PropiedadesApp propiedades;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public AlertasResponse alertas() {
        PropiedadesApp.Alertas reglas = propiedades.alertas();
        LocalDate hoy = calendario.hoy();
        List<LocalDate> ventana = Calendario.diasHabiles(reglas.ventanaDias(), hoy);
        Map<Long, Integer> faltas = faltasPorEstudiante(ventana.get(0), hoy);
        Map<Long, Integer> faltasHoy = faltasPorEstudiante(hoy, hoy);
        Map<Long, LocalDateTime> ultimoAviso = new HashMap<>();
        avisos.ultimosAvisos().forEach(fila -> ultimoAviso.put((Long) fila[0], (LocalDateTime) fila[1]));

        List<ResumenAlertaResponse> lista = new ArrayList<>();
        for (Matricula m : matriculas.listarActivas(calendario.anioActual(), EstadoMatricula.ACTIVA)) {
            Long id = m.getEstudiante().getId();
            int total = faltas.getOrDefault(id, 0);
            boolean faltaHoy = faltasHoy.containsKey(id);
            if (total < reglas.faltasPreventivas() && !faltaHoy) {
                continue;
            }
            String nivel = total >= reglas.faltasCriticas() ? "critica" : total >= reglas.faltasPreventivas() ? "preventiva" : "hoy";
            lista.add(new ResumenAlertaResponse(EstudianteResponse.de(m), total, faltaHoy, nivel, ultimoAviso.get(id)));
        }
        lista.sort(Comparator.comparingInt(ResumenAlertaResponse::faltas).reversed()
                .thenComparing(a -> a.estudiante().nombre()));
        int criticas = (int) lista.stream().filter(a -> "critica".equals(a.nivel())).count();
        int preventivas = (int) lista.stream().filter(a -> "preventiva".equals(a.nivel())).count();
        return new AlertasResponse(
                new ParametrosAlerta(reglas.ventanaDias(), reglas.faltasCriticas(), reglas.faltasPreventivas(), ventana.get(0), hoy),
                criticas, preventivas, faltasHoy.size(), lista);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaltasDia> tendencia(int dias) {
        if (dias < 1 || dias > 60) {
            throw ApiException.validacion("La tendencia admite entre 1 y 60 días hábiles.");
        }
        List<LocalDate> fechas = Calendario.diasHabiles(dias, calendario.hoy());
        Map<LocalDate, Long> conteo = new HashMap<>();
        asistencias.contarPorFecha(EstadoAsistencia.FALTA, fechas.get(0), fechas.get(fechas.size() - 1), calendario.anioActual(), EstadoMatricula.ACTIVA)
                .forEach(fila -> conteo.put((LocalDate) fila[0], (Long) fila[1]));
        return fechas.stream().map(f -> new FaltasDia(f, conteo.getOrDefault(f, 0L))).toList();
    }

    @Override
    @Transactional
    public List<AvisoResponse> avisarFamilias(AvisoFamiliaRequest request, SesionUsuario sesion) {
        LocalDate hoy = calendario.hoy();
        Map<Long, Integer> faltas = faltasPorEstudiante(Calendario.diasHabiles(propiedades.alertas().ventanaDias(), hoy).get(0), hoy);
        Usuario usuario = entidades.usuarioOpcional(sesion.usuarioId());
        LocalDateTime ahora = calendario.ahora();
        List<AvisoResponse> enviados = new ArrayList<>();
        for (Long estudianteId : new LinkedHashSet<>(request.estudianteIds())) {
            if (estudianteId == null) {
                throw ApiException.validacion("La lista de estudiantes contiene un valor vacío.");
            }
            AvisoFamilia aviso = new AvisoFamilia();
            aviso.setEstudiante(entidades.estudiante(estudianteId));
            aviso.setEnviadoEn(ahora);
            aviso.setFaltas(faltas.getOrDefault(estudianteId, 0));
            aviso.setEnviadoPor(usuario);
            enviados.add(respuesta(avisos.save(aviso)));
        }
        return enviados;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisoResponse> avisos(Long estudianteId) {
        entidades.estudiante(estudianteId);
        return avisos.findByEstudianteIdOrderByEnviadoEnDesc(estudianteId).stream().map(this::respuesta).toList();
    }

    private Map<Long, Integer> faltasPorEstudiante(LocalDate desde, LocalDate hasta) {
        Map<Long, Integer> faltas = new HashMap<>();
        asistencias.contarPorEstudiante(EstadoAsistencia.FALTA, desde, hasta, calendario.anioActual(), EstadoMatricula.ACTIVA)
                .forEach(fila -> faltas.put((Long) fila[0], ((Long) fila[1]).intValue()));
        return faltas;
    }

    private AvisoResponse respuesta(AvisoFamilia a) {
        return new AvisoResponse(a.getId(), a.getEstudiante().getId(), a.getEstudiante().getNombreCompleto(), a.getEnviadoEn(), a.getFaltas(),
                a.getEnviadoPor() == null ? null : a.getEnviadoPor().getNombre());
    }
}

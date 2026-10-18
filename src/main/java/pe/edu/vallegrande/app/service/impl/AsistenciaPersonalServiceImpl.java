package pe.edu.vallegrande.app.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.config.PropiedadesApp;
import pe.edu.vallegrande.app.dto.request.MarcacionPersonalRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.AsistenciaPersonalDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.FilaPersonal;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.FilaReporte;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.ReportePersonalResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.ResumenDia;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.AsistenciaPersonal;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.repository.AsistenciaPersonalRepository;
import pe.edu.vallegrande.app.repository.DocenteRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsistenciaPersonalService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class AsistenciaPersonalServiceImpl implements AsistenciaPersonalService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SIN_MARCAR = "sin-marcar";

    private final AsistenciaPersonalRepository marcaciones;
    private final DocenteRepository docentes;
    private final ReglasAsistencia reglas;
    private final Calendario calendario;
    private final Entidades entidades;

    public AsistenciaPersonalServiceImpl(AsistenciaPersonalRepository marcaciones, DocenteRepository docentes, ReglasAsistencia reglas, Calendario calendario, Entidades entidades) {
        this.marcaciones = marcaciones;
        this.docentes = docentes;
        this.reglas = reglas;
        this.calendario = calendario;
        this.entidades = entidades;
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaPersonalDiaResponse delDia(LocalDate fecha) {
        LocalDate dia = fecha == null ? calendario.hoy() : fecha;
        PropiedadesApp.Asistencia vigentes = reglas.vigentes();
        Map<Long, AsistenciaPersonal> delDia = new HashMap<>();
        marcaciones.listar(dia, dia).forEach(a -> delDia.put(a.getDocente().getId(), a));
        List<FilaPersonal> filas = activos().stream().map(p -> fila(p, delDia.get(p.getId()), vigentes)).toList();
        int puntuales = (int) filas.stream().filter(f -> AsistenciaPersonal.PUNTUAL.equals(f.estado())).count();
        int tardanzas = (int) filas.stream().filter(f -> AsistenciaPersonal.TARDANZA.equals(f.estado())).count();
        int conSalida = (int) filas.stream().filter(f -> f.horaSalida() != null).count();
        return new AsistenciaPersonalDiaResponse(dia, vigentes.horaEntrada().format(HORA), vigentes.minutosTolerancia(),
                vigentes.limitePuntualidad().format(HORA),
                new ResumenDia(filas.size(), puntuales, tardanzas, filas.size() - puntuales - tardanzas, conSalida), filas);
    }

    @Override
    @Transactional
    public FilaPersonal marcarEntrada(Long docenteId, MarcacionPersonalRequest request, SesionUsuario sesion) {
        Docente p = activo(docenteId);
        LocalDate fecha = fecha(request);
        LocalTime hora = hora(request, fecha);
        PropiedadesApp.Asistencia vigentes = reglas.vigentes();
        AsistenciaPersonal registro = marcaciones.findByDocenteIdAndFecha(docenteId, fecha).orElseGet(() -> {
            AsistenciaPersonal nuevo = new AsistenciaPersonal();
            nuevo.setDocente(p);
            nuevo.setFecha(fecha);
            return nuevo;
        });
        if (registro.getHoraSalida() != null && !hora.isBefore(registro.getHoraSalida())) {
            throw ApiException.validacion("La entrada debe ser anterior a la salida registrada (" + registro.getHoraSalida().format(HORA) + ").");
        }
        registro.setHoraEntrada(hora);
        registro.setEstado(hora.isAfter(vigentes.limitePuntualidad()) ? AsistenciaPersonal.TARDANZA : AsistenciaPersonal.PUNTUAL);
        guardar(registro, request, sesion);
        return fila(p, registro, vigentes);
    }

    @Override
    @Transactional
    public FilaPersonal marcarSalida(Long docenteId, MarcacionPersonalRequest request, SesionUsuario sesion) {
        Docente p = activo(docenteId);
        LocalDate fecha = fecha(request);
        LocalTime hora = hora(request, fecha);
        AsistenciaPersonal registro = marcaciones.findByDocenteIdAndFecha(docenteId, fecha)
                .orElseThrow(() -> ApiException.conflicto("Registra primero la entrada de " + p.getNombres() + " del " + fecha + "."));
        if (!hora.isAfter(registro.getHoraEntrada())) {
            throw ApiException.validacion("La salida debe ser posterior a la entrada (" + registro.getHoraEntrada().format(HORA) + ").");
        }
        registro.setHoraSalida(hora);
        guardar(registro, request, sesion);
        return fila(p, registro, reglas.vigentes());
    }

    @Override
    @Transactional
    public void eliminar(Long docenteId, LocalDate fecha) {
        AsistenciaPersonal registro = marcaciones.findByDocenteIdAndFecha(docenteId, fecha)
                .orElseThrow(() -> ApiException.noEncontrado("No hay marcación de esa persona el " + fecha + "."));
        marcaciones.delete(registro);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportePersonalResponse reporte(String mes) {
        YearMonth periodo = mes == null ? YearMonth.from(calendario.hoy()) : Calendario.mes(mes);
        LocalDate hoy = calendario.hoy();
        List<LocalDate> habiles = Calendario.diasDelMes(periodo).stream()
                .filter(d -> !Calendario.esFinDeSemana(d) && !d.isAfter(hoy)).toList();
        PropiedadesApp.Asistencia vigentes = reglas.vigentes();
        Map<Long, List<AsistenciaPersonal>> porPersona = new HashMap<>();
        marcaciones.listar(periodo.atDay(1), periodo.atEndOfMonth())
                .forEach(a -> porPersona.computeIfAbsent(a.getDocente().getId(), k -> new ArrayList<>()).add(a));
        List<FilaReporte> filas = activos().stream().map(p -> {
            List<AsistenciaPersonal> registros = porPersona.getOrDefault(p.getId(), List.of());
            int asistidos = registros.size();
            int tardanzas = (int) registros.stream().filter(a -> AsistenciaPersonal.TARDANZA.equals(a.getEstado())).count();
            int minutos = registros.stream().mapToInt(a -> minutosTarde(a, vigentes)).sum();
            int porcentaje = habiles.isEmpty() ? 0 : Math.round(asistidos * 100f / habiles.size());
            return new FilaReporte(p.getId(), nombre(p), p.getCargo(), habiles.size(), asistidos, tardanzas, minutos,
                    Math.max(0, habiles.size() - asistidos), porcentaje);
        }).toList();
        return new ReportePersonalResponse(periodo.toString(), habiles.size(), filas);
    }

    private void guardar(AsistenciaPersonal registro, MarcacionPersonalRequest request, SesionUsuario sesion) {
        if (request.observacion() != null) {
            registro.setObservacion(Textos.limpiar(request.observacion()));
        }
        registro.setRegistradoPor(entidades.usuarioOpcional(sesion.usuarioId()));
        registro.setActualizadoEn(calendario.ahora());
        marcaciones.save(registro);
    }

    private LocalDate fecha(MarcacionPersonalRequest request) {
        LocalDate fecha = request.fecha() == null ? calendario.hoy() : request.fecha();
        calendario.validarFechaRegistrable(fecha);
        return fecha;
    }

    /** Sin hora se marca la actual; corregir un día anterior exige indicar la hora. */
    private LocalTime hora(MarcacionPersonalRequest request, LocalDate fecha) {
        if (request.hora() != null) {
            LocalTime hora = request.hora().withSecond(0).withNano(0);
            if (fecha.equals(calendario.hoy()) && hora.isAfter(calendario.ahora().toLocalTime())) {
                throw ApiException.validacion("No se puede marcar una hora futura.");
            }
            return hora;
        }
        if (!fecha.equals(calendario.hoy())) {
            throw ApiException.validacion("Indica la hora para corregir la marcación de un día anterior.");
        }
        return calendario.ahora().toLocalTime().withSecond(0);
    }

    private Docente activo(Long id) {
        Docente p = entidades.docente(id);
        if (!p.esActivo()) {
            throw ApiException.conflicto("El registro está inactivo; restáuralo antes de registrar su asistencia.");
        }
        return p;
    }

    private List<Docente> activos() {
        return docentes.listar(Docente.ACTIVO, null);
    }

    private static FilaPersonal fila(Docente p, AsistenciaPersonal a, PropiedadesApp.Asistencia vigentes) {
        if (a == null) {
            return new FilaPersonal(p.getId(), nombre(p), p.getDni(), p.getCargo(), p.getArea(), null, null, SIN_MARCAR, 0, null);
        }
        return new FilaPersonal(p.getId(), nombre(p), p.getDni(), p.getCargo(), p.getArea(), a.getHoraEntrada().format(HORA),
                a.getHoraSalida() == null ? null : a.getHoraSalida().format(HORA), a.getEstado(), minutosTarde(a, vigentes), a.getObservacion());
    }

    /** Minutos después de la hora oficial de entrada (solo cuentan en las tardanzas). */
    private static int minutosTarde(AsistenciaPersonal a, PropiedadesApp.Asistencia vigentes) {
        if (!AsistenciaPersonal.TARDANZA.equals(a.getEstado())) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(vigentes.horaEntrada(), a.getHoraEntrada()).toMinutes());
    }

    private static String nombre(Docente p) {
        return p.getApellidos() + ", " + p.getNombres();
    }
}

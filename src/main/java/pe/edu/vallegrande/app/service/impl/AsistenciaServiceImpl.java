package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.AsistenciaLoteRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AsistenciaDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AulaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.ConteoEstados;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.FilaMensual;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.MatrizMensualResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroDia;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroEstudiante;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.AsistenciaDiaria;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.repository.AsistenciaDiariaRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsistenciaService;
import pe.edu.vallegrande.app.util.Calendario;

@Service
@RequiredArgsConstructor
public class AsistenciaServiceImpl implements AsistenciaService {

    private final AsistenciaDiariaRepository asistencias;
    private final MatriculaRepository matriculas;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public AsistenciaDiaResponse delAula(Long seccionId, LocalDate fecha) {
        Seccion seccion = entidades.seccion(seccionId);
        LocalDate dia = fecha == null ? calendario.hoy() : fecha;
        return respuestaDelDia(seccion, dia);
    }

    @Override
    @Transactional(readOnly = true)
    public MatrizMensualResponse mensual(Long seccionId, String mes) {
        Seccion seccion = entidades.seccion(seccionId);
        YearMonth periodo = mes == null ? YearMonth.from(calendario.hoy()) : Calendario.mes(mes);
        List<LocalDate> dias = Calendario.diasDelMes(periodo);
        List<Matricula> alumnos = alumnos(seccionId);
        Map<Long, Map<LocalDate, EstadoAsistencia>> porEstudiante = new HashMap<>();
        if (!alumnos.isEmpty()) {
            for (AsistenciaDiaria a : asistencias.listar(ids(alumnos), dias.get(0), dias.get(dias.size() - 1))) {
                porEstudiante.computeIfAbsent(a.getEstudiante().getId(), k -> new HashMap<>()).put(a.getFecha(), a.getEstado());
            }
        }
        List<FilaMensual> filas = alumnos.stream().map(m -> {
            Estudiante e = m.getEstudiante();
            Map<LocalDate, EstadoAsistencia> registrados = porEstudiante.getOrDefault(e.getId(), Map.of());
            Map<LocalDate, EstadoAsistencia> estados = new LinkedHashMap<>();
            dias.stream().filter(d -> !Calendario.esFinDeSemana(d))
                    .forEach(d -> estados.put(d, registrados.getOrDefault(d, EstadoAsistencia.PENDIENTE)));
            List<EstadoAsistencia> contables = estados.entrySet().stream()
                    .filter(entrada -> !entrada.getKey().isAfter(calendario.hoy()))
                    .map(Map.Entry::getValue)
                    .toList();
            return new FilaMensual(e.getId(), e.getNombreCompleto(), e.getDni(), estados, ConteoEstados.de(contables));
        }).toList();
        return new MatrizMensualResponse(aula(seccion), periodo.toString(), dias,
                dias.stream().filter(Calendario::esFinDeSemana).toList(), filas);
    }

    @Override
    @Transactional
    public AsistenciaDiaResponse registrarLote(Long seccionId, AsistenciaLoteRequest request, SesionUsuario sesion) {
        Seccion seccion = entidades.seccion(seccionId);
        calendario.validarFechaRegistrable(request.fecha());
        Map<Long, Estudiante> delAula = new HashMap<>();
        alumnos(seccionId).forEach(m -> delAula.put(m.getEstudiante().getId(), m.getEstudiante()));

        Set<Long> vistos = new HashSet<>();
        for (AsistenciaLoteRequest.Registro r : request.registros()) {
            if (!vistos.add(r.estudianteId())) {
                throw ApiException.validacion("El estudiante con id " + r.estudianteId() + " aparece más de una vez.");
            }
            if (!delAula.containsKey(r.estudianteId())) {
                throw ApiException.validacion("El estudiante con id " + r.estudianteId() + " no está matriculado en esta sección.");
            }
        }
        Usuario usuario = entidades.usuarioOpcional(sesion.usuarioId());
        Map<Long, AsistenciaDiaria> existentes = new HashMap<>();
        asistencias.listar(vistos, request.fecha(), request.fecha()).forEach(a -> existentes.put(a.getEstudiante().getId(), a));
        for (AsistenciaLoteRequest.Registro r : request.registros()) {
            guardar(delAula.get(r.estudianteId()), request.fecha(), r.estado(), existentes.get(r.estudianteId()), usuario);
        }
        asistencias.flush();
        return respuestaDelDia(seccion, request.fecha());
    }

    @Override
    @Transactional
    public RegistroHistorial registrar(Long estudianteId, LocalDate fecha, EstadoAsistencia estado, SesionUsuario sesion) {
        Estudiante estudiante = entidades.estudiante(estudianteId);
        entidades.matriculaActiva(estudianteId);
        calendario.validarFechaRegistrable(fecha);
        guardar(estudiante, fecha, estado, asistencias.findByEstudianteIdAndFecha(estudianteId, fecha).orElse(null),
                entidades.usuarioOpcional(sesion.usuarioId()));
        return new RegistroHistorial(fecha, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroHistorial> historial(Long estudianteId, LocalDate desde, LocalDate hasta) {
        entidades.estudiante(estudianteId);
        LocalDate fin = hasta == null ? calendario.hoy() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(60) : desde;
        if (inicio.isAfter(fin)) {
            throw ApiException.validacion("La fecha inicial no puede ser posterior a la final.");
        }
        return asistencias.findByEstudianteIdAndFechaBetweenOrderByFechaDesc(estudianteId, inicio, fin).stream()
                .map(a -> new RegistroHistorial(a.getFecha(), a.getEstado()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroDia> rango(LocalDate desde, LocalDate hasta, Long seccionId) {
        if (desde.isAfter(hasta)) {
            throw ApiException.validacion("La fecha inicial no puede ser posterior a la final.");
        }
        if (desde.plusDays(93).isBefore(hasta)) {
            throw ApiException.validacion("Consulta como máximo 93 días de asistencia a la vez.");
        }
        if (seccionId != null) {
            entidades.seccion(seccionId);
        }
        return asistencias.rango(desde, hasta, seccionId, calendario.anioActual(), EstadoMatricula.ACTIVA).stream()
                .map(fila -> new RegistroDia((Long) fila[0], (LocalDate) fila[1], (EstadoAsistencia) fila[2]))
                .toList();
    }

    /** Inserta, actualiza o (con "pendiente") elimina el registro del día. */
    private void guardar(Estudiante estudiante, LocalDate fecha, EstadoAsistencia estado, AsistenciaDiaria existente, Usuario usuario) {
        if (estado == EstadoAsistencia.PENDIENTE) {
            if (existente != null) {
                asistencias.delete(existente);
            }
            return;
        }
        AsistenciaDiaria registro = existente == null ? new AsistenciaDiaria() : existente;
        registro.setEstudiante(estudiante);
        registro.setFecha(fecha);
        registro.setEstado(estado);
        registro.setRegistradoPor(usuario);
        registro.setActualizadoEn(calendario.ahora());
        if (existente == null) {
            asistencias.save(registro);
        }
    }

    private AsistenciaDiaResponse respuestaDelDia(Seccion seccion, LocalDate fecha) {
        List<Matricula> alumnos = alumnos(seccion.getId());
        Map<Long, EstadoAsistencia> estados = new HashMap<>();
        if (!alumnos.isEmpty()) {
            asistencias.listar(ids(alumnos), fecha, fecha).forEach(a -> estados.put(a.getEstudiante().getId(), a.getEstado()));
        }
        List<RegistroEstudiante> registros = alumnos.stream()
                .map(m -> new RegistroEstudiante(m.getEstudiante().getId(), m.getEstudiante().getNombreCompleto(), m.getEstudiante().getDni(),
                        estados.getOrDefault(m.getEstudiante().getId(), EstadoAsistencia.PENDIENTE)))
                .toList();
        return new AsistenciaDiaResponse(aula(seccion), fecha, registros,
                ConteoEstados.de(registros.stream().map(RegistroEstudiante::estado).toList()));
    }

    private List<Matricula> alumnos(Long seccionId) {
        return matriculas.listarPorSeccion(seccionId, calendario.anioActual(), EstadoMatricula.ACTIVA);
    }

    private static Set<Long> ids(List<Matricula> alumnos) {
        Set<Long> ids = new HashSet<>();
        alumnos.forEach(m -> ids.add(m.getEstudiante().getId()));
        return ids;
    }

    static AulaResponse aula(Seccion s) {
        return new AulaResponse(s.getId(), s.getNivelNombre(), s.getGrado().getCodigo(), s.getGrado().getEtiqueta(), s.getLetra(), s.getTurno());
    }
}

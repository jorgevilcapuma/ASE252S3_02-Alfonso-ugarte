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

import pe.edu.vallegrande.app.dto.request.AsistenciaLoteRequest;
import pe.edu.vallegrande.app.dto.request.EstadoAsistenciaRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AsistenciaDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.AulaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.ConteoEstados;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.FilaMensual;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.MatrizMensualResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroEstudiante;
import pe.edu.vallegrande.app.dto.response.AsistenciaResponses.RegistroHistorial;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.AsistenciaEstudiante;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.repository.AsistenciaEstudianteRepository;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.AsistenciaEstudianteService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class AsistenciaEstudianteServiceImpl implements AsistenciaEstudianteService {

    private final AsistenciaEstudianteRepository asistencias;
    private final EstudianteRepository estudiantes;
    private final Calendario calendario;
    private final Entidades entidades;
    private final ReglasAsistencia reglas;

    public AsistenciaEstudianteServiceImpl(AsistenciaEstudianteRepository asistencias, EstudianteRepository estudiantes, Calendario calendario, Entidades entidades, ReglasAsistencia reglas) {
        this.asistencias = asistencias;
        this.estudiantes = estudiantes;
        this.calendario = calendario;
        this.entidades = entidades;
        this.reglas = reglas;
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenciaDiaResponse delAula(Long gradoSeccionId, Long cursoId, LocalDate fecha) {
        GradoSeccion aula = entidades.gradoSeccion(gradoSeccionId);
        Curso curso = cursoDelAula(cursoId, aula);
        return respuestaDelDia(aula, curso, fecha == null ? calendario.hoy() : fecha);
    }

    @Override
    @Transactional(readOnly = true)
    public MatrizMensualResponse mensual(Long gradoSeccionId, Long cursoId, String mes) {
        GradoSeccion aula = entidades.gradoSeccion(gradoSeccionId);
        Curso curso = cursoDelAula(cursoId, aula);
        YearMonth periodo = mes == null ? YearMonth.from(calendario.hoy()) : Calendario.mes(mes);
        List<LocalDate> dias = Calendario.diasDelMes(periodo);
        List<Estudiante> alumnos = alumnos(gradoSeccionId);
        Map<Long, Map<LocalDate, EstadoAsistencia>> porEstudiante = new HashMap<>();
        if (!alumnos.isEmpty()) {
            for (AsistenciaEstudiante a : asistencias.listar(ids(alumnos), cursoId, dias.get(0), dias.get(dias.size() - 1))) {
                porEstudiante.computeIfAbsent(a.getEstudiante().getId(), k -> new HashMap<>()).put(a.getFecha(), a.getEstado());
            }
        }
        List<FilaMensual> filas = alumnos.stream().map(e -> {
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
        return new MatrizMensualResponse(AulaResponse.de(aula), cursoId, curso == null ? null : curso.getNombre(), periodo.toString(),
                dias, dias.stream().filter(Calendario::esFinDeSemana).toList(), filas);
    }

    @Override
    @Transactional
    public AsistenciaDiaResponse registrarLote(Long gradoSeccionId, AsistenciaLoteRequest request, SesionUsuario sesion) {
        GradoSeccion aula = entidades.gradoSeccion(gradoSeccionId);
        Curso curso = cursoDelAula(request.cursoId(), aula);
        validarRegistro(request.fecha(), curso, sesion);
        Map<Long, Estudiante> delAula = new HashMap<>();
        alumnos(gradoSeccionId).forEach(e -> delAula.put(e.getId(), e));

        Set<Long> vistos = new HashSet<>();
        for (AsistenciaLoteRequest.Registro r : request.registros()) {
            if (!vistos.add(r.estudianteId())) {
                throw ApiException.validacion("El estudiante con id " + r.estudianteId() + " aparece más de una vez.");
            }
            if (!delAula.containsKey(r.estudianteId())) {
                throw ApiException.validacion("El estudiante con id " + r.estudianteId() + " no es un estudiante activo de esta aula.");
            }
        }
        Usuario usuario = entidades.usuarioOpcional(sesion.usuarioId());
        Map<Long, AsistenciaEstudiante> existentes = new HashMap<>();
        asistencias.listar(vistos, request.cursoId(), request.fecha(), request.fecha())
                .forEach(a -> existentes.put(a.getEstudiante().getId(), a));
        for (AsistenciaLoteRequest.Registro r : request.registros()) {
            guardar(delAula.get(r.estudianteId()), curso, request.fecha(), r.estado(), r.observacion(),
                    existentes.get(r.estudianteId()), usuario);
        }
        asistencias.flush();
        return respuestaDelDia(aula, curso, request.fecha());
    }

    @Override
    @Transactional
    public RegistroHistorial registrar(Long estudianteId, EstadoAsistenciaRequest request, SesionUsuario sesion) {
        Estudiante estudiante = entidades.estudianteActivo(estudianteId);
        Curso curso = cursoDelAula(request.cursoId(), estudiante.getGradoSeccion());
        validarRegistro(request.fecha(), curso, sesion);
        guardar(estudiante, curso, request.fecha(), request.estado(), request.observacion(),
                asistencias.buscar(estudianteId, request.cursoId(), request.fecha()).orElse(null),
                entidades.usuarioOpcional(sesion.usuarioId()));
        return new RegistroHistorial(request.fecha(), request.cursoId(), curso == null ? null : curso.getNombre(), request.estado(),
                request.estado() == EstadoAsistencia.PENDIENTE ? null : Textos.limpiar(request.observacion()));
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
        return asistencias.historial(estudianteId, inicio, fin).stream()
                .map(a -> new RegistroHistorial(a.getFecha(), a.getCurso() == null ? null : a.getCurso().getId(),
                        a.getCurso() == null ? null : a.getCurso().getNombre(), a.getEstado(), a.getObservacion()))
                .toList();
    }

    /** Fecha hábil no futura, plazo de edición del docente y, por curso, que el docente sea quien lo dicta. */
    private void validarRegistro(LocalDate fecha, Curso curso, SesionUsuario sesion) {
        calendario.validarFechaRegistrable(fecha);
        reglas.validarPlazoDocente(fecha, sesion);
        if (curso != null && sesion.esDocente() && !curso.getDocente().getId().equals(sesion.docenteId())) {
            throw ApiException.prohibido("Solo el docente del curso puede registrar su asistencia.");
        }
    }

    /** Inserta, actualiza o (con "pendiente") elimina el registro. */
    private void guardar(Estudiante estudiante, Curso curso, LocalDate fecha, EstadoAsistencia estado, String observacion,
                         AsistenciaEstudiante existente, Usuario usuario) {
        if (estado == EstadoAsistencia.PENDIENTE) {
            if (existente != null) {
                asistencias.delete(existente);
            }
            return;
        }
        AsistenciaEstudiante registro = existente == null ? new AsistenciaEstudiante() : existente;
        registro.setEstudiante(estudiante);
        registro.setCurso(curso);
        registro.setFecha(fecha);
        registro.setEstado(estado);
        registro.setObservacion(Textos.limpiar(observacion));
        registro.setRegistradoPor(usuario);
        registro.setActualizadoEn(calendario.ahora());
        if (existente == null) {
            asistencias.save(registro);
        }
    }

    private AsistenciaDiaResponse respuestaDelDia(GradoSeccion aula, Curso curso, LocalDate fecha) {
        List<Estudiante> alumnos = alumnos(aula.getId());
        Long cursoId = curso == null ? null : curso.getId();
        Map<Long, AsistenciaEstudiante> registrados = new HashMap<>();
        if (!alumnos.isEmpty()) {
            asistencias.listar(ids(alumnos), cursoId, fecha, fecha).forEach(a -> registrados.put(a.getEstudiante().getId(), a));
        }
        List<RegistroEstudiante> registros = alumnos.stream().map(e -> {
            AsistenciaEstudiante a = registrados.get(e.getId());
            return new RegistroEstudiante(e.getId(), e.getNombreCompleto(), e.getDni(),
                    a == null ? EstadoAsistencia.PENDIENTE : a.getEstado(), a == null ? null : a.getObservacion());
        }).toList();
        return new AsistenciaDiaResponse(AulaResponse.de(aula), cursoId, curso == null ? null : curso.getNombre(), fecha, registros,
                ConteoEstados.de(registros.stream().map(RegistroEstudiante::estado).toList()));
    }

    private Curso cursoDelAula(Long cursoId, GradoSeccion aula) {
        if (cursoId == null) {
            return null;
        }
        Curso curso = entidades.curso(cursoId);
        if (!curso.getGradoSeccion().getId().equals(aula.getId())) {
            throw ApiException.validacion("El curso " + curso.getNombreConAula() + " no corresponde a " + aula.getNombre() + ".");
        }
        if (!curso.esActivo()) {
            throw ApiException.conflicto("El curso " + curso.getNombreConAula() + " está eliminado.");
        }
        return curso;
    }

    private List<Estudiante> alumnos(Long gradoSeccionId) {
        return estudiantes.listar(gradoSeccionId, Estudiante.ACTIVO, null);
    }

    private static Set<Long> ids(List<Estudiante> alumnos) {
        Set<Long> ids = new HashSet<>();
        alumnos.forEach(e -> ids.add(e.getId()));
        return ids;
    }
}

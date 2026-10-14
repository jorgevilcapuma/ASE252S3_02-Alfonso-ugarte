package pe.edu.vallegrande.app.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.DocenteRequest;
import pe.edu.vallegrande.app.dto.request.PerfilDocenteRequest;
import pe.edu.vallegrande.app.dto.response.AlumnoCursoResponse;
import pe.edu.vallegrande.app.dto.response.DocenteResponse;
import pe.edu.vallegrande.app.dto.response.SesionClaseResponses.AlertaCursoResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.AsistenciaCurso;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoAsistenciaCurso;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.repository.AsignacionRepository;
import pe.edu.vallegrande.app.repository.AsistenciaCursoRepository;
import pe.edu.vallegrande.app.repository.DocenteRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.service.DocenteService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class DocenteServiceImpl implements DocenteService {

    private final DocenteRepository docentes;
    private final AsignacionRepository asignaciones;
    private final MatriculaRepository matriculas;
    private final AsistenciaCursoRepository asistenciaCurso;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public List<DocenteResponse> listar() {
        return docentes.listar().stream().map(DocenteResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocenteResponse obtener(Long id) {
        return DocenteResponse.de(entidades.docente(id));
    }

    @Override
    @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        if (docentes.existsByDni(request.dni())) {
            throw ApiException.conflicto("Ya existe un docente con el DNI " + request.dni() + ".");
        }
        Docente docente = new Docente();
        aplicar(docente, request);
        return DocenteResponse.de(docentes.save(docente));
    }

    @Override
    @Transactional
    public DocenteResponse actualizar(Long id, DocenteRequest request) {
        Docente docente = entidades.docente(id);
        if (docentes.existsByDniAndIdNot(request.dni(), id)) {
            throw ApiException.conflicto("Ya existe un docente con el DNI " + request.dni() + ".");
        }
        aplicar(docente, request);
        return DocenteResponse.de(docente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Docente docente = entidades.docente(id);
        if (asignaciones.existsByDocenteId(id)) {
            throw ApiException.conflicto("No se puede eliminar al docente porque tiene cursos asignados. Reasigna sus cursos primero.");
        }
        docentes.delete(docente);
    }

    @Override
    @Transactional
    public DocenteResponse actualizarPerfil(Long id, PerfilDocenteRequest request) {
        Docente docente = entidades.docente(id);
        docente.setNombres(Textos.limpiar(request.nombres()));
        docente.setApellidos(Textos.limpiar(request.apellidos()));
        docente.setTelefono(Textos.limpiar(request.telefono()));
        if (docente.getUsuario() != null) {
            docente.getUsuario().setNombre(docente.getNombreCompleto());
        }
        return DocenteResponse.de(docente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlumnoCursoResponse> estudiantes(Long docenteId, Long cursoId) {
        List<AlumnoCursoResponse> lista = new ArrayList<>();
        recorrer(docenteId, cursoId, (asignacion, estudiante, estados) -> {
            int asistencias = (int) estados.stream().filter(e -> e != EstadoAsistenciaCurso.FALTA).count();
            int porcentaje = estados.isEmpty() ? 100 : Math.round(asistencias * 100f / estados.size());
            lista.add(new AlumnoCursoResponse(estudiante.getId(), estudiante.getDni(), estudiante.getNombres(), estudiante.getApellidos(),
                    estudiante.getGenero(), asignacion.getSeccion().getNivelNombre(),
                    asignacion.getSeccion().getGrado().getEtiqueta() + " " + asignacion.getSeccion().getLetra(),
                    asignacion.getId(), asignacion.getCurso().getNombre(), nombreCurso(asignacion), estados.size(), porcentaje,
                    AlumnoCursoResponse.estadoGeneral(porcentaje)));
        });
        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaCursoResponse> alertas(Long docenteId) {
        List<AlertaCursoResponse> alertas = new ArrayList<>();
        Map<String, LocalDate> ultimaFalta = new HashMap<>();
        asistenciaCurso.registrosDeCursos(docenteId, null, inicioDelAnio(), calendario.hoy()).stream()
                .filter(r -> r.getEstado() == EstadoAsistenciaCurso.FALTA)
                .forEach(r -> ultimaFalta.merge(r.getSesion().getAsignacion().getId() + "-" + r.getEstudiante().getId(),
                        r.getSesion().getFecha(), (a, b) -> a.isAfter(b) ? a : b));

        recorrer(docenteId, null, (asignacion, estudiante, estados) -> {
            if (estados.isEmpty()) {
                return;
            }
            int consecutivas = 0;
            for (int i = estados.size() - 1; i >= 0 && estados.get(i) == EstadoAsistenciaCurso.FALTA; i--) {
                consecutivas++;
            }
            int asistencias = (int) estados.stream().filter(e -> e != EstadoAsistenciaCurso.FALTA).count();
            int porcentaje = Math.round(asistencias * 100f / estados.size());
            boolean enRiesgo = consecutivas >= 2 || (estados.size() >= 3 && porcentaje < 80);
            if (!enRiesgo) {
                return;
            }
            String nivel = consecutivas >= 3 || porcentaje < 68 ? "Crítico" : consecutivas == 2 || porcentaje < 75 ? "Alto" : "Medio";
            String clave = asignacion.getId() + "-" + estudiante.getId();
            alertas.add(new AlertaCursoResponse(clave, estudiante.getId(), estudiante.getNombreCompleto(), asignacion.getId(),
                    nombreCurso(asignacion), consecutivas, porcentaje, nivel, ultimaFalta.get(clave)));
        });
        alertas.sort(Comparator.comparing((AlertaCursoResponse a) -> ordenRiesgo(a.nivelRiesgo()))
                .thenComparing(AlertaCursoResponse::porcentajeActual));
        return alertas;
    }

    @FunctionalInterface
    private interface VisitaAlumno {
        void visitar(Asignacion asignacion, Estudiante estudiante, List<EstadoAsistenciaCurso> estadosEnOrden);
    }

    /** Recorre cada alumno matriculado en cada curso del docente con sus estados de asistencia del año (en orden de fecha). */
    private void recorrer(Long docenteId, Long cursoId, VisitaAlumno visita) {
        entidades.docente(docenteId);
        List<Asignacion> cursos = asignaciones.listar(docenteId, null).stream()
                .filter(a -> cursoId == null || a.getId().equals(cursoId))
                .toList();
        if (cursoId != null && cursos.isEmpty()) {
            throw ApiException.noEncontrado("El curso indicado no pertenece al docente.");
        }
        if (cursos.isEmpty()) {
            return;
        }
        Set<Long> seccionIds = cursos.stream().map(a -> a.getSeccion().getId()).collect(Collectors.toSet());
        Map<Long, List<Matricula>> porSeccion = matriculas.listarPorSecciones(seccionIds, calendario.anioActual(), EstadoMatricula.ACTIVA)
                .stream().collect(Collectors.groupingBy(m -> m.getSeccion().getId()));
        Map<String, List<EstadoAsistenciaCurso>> estados = new HashMap<>();
        for (AsistenciaCurso r : asistenciaCurso.registrosDeCursos(docenteId, cursoId, inicioDelAnio(), calendario.hoy())) {
            estados.computeIfAbsent(r.getSesion().getAsignacion().getId() + "-" + r.getEstudiante().getId(), k -> new ArrayList<>())
                    .add(r.getEstado());
        }
        for (Asignacion asignacion : cursos) {
            for (Matricula m : porSeccion.getOrDefault(asignacion.getSeccion().getId(), List.of())) {
                visita.visitar(asignacion, m.getEstudiante(),
                        estados.getOrDefault(asignacion.getId() + "-" + m.getEstudiante().getId(), List.of()));
            }
        }
    }

    private void aplicar(Docente docente, DocenteRequest request) {
        docente.setNombres(Textos.limpiar(request.nombres()));
        docente.setApellidos(Textos.limpiar(request.apellidos()));
        docente.setDni(request.dni());
        docente.setTelefono(Textos.limpiar(request.telefono()));
        docente.setEspecialidad(Textos.limpiar(request.especialidad()));
        docente.setColegiatura(Textos.limpiar(request.colegiatura()));
        docente.setCodigoPlaza(Textos.limpiar(request.codigoPlaza()));
        docente.setCondicion(Textos.limpiar(request.condicion()));
        docente.setNivelEducativo(Textos.limpiar(request.nivelEducativo()));
        docente.setFechaIngreso(request.fechaIngreso());
        if (request.usuarioId() == null) {
            docente.setUsuario(null);
            return;
        }
        Usuario usuario = entidades.usuario(request.usuarioId());
        if (usuario.getPerfil() != Perfil.DOCENTE) {
            throw ApiException.validacion("La cuenta vinculada debe tener perfil docente.");
        }
        docentes.findByUsuarioId(usuario.getId()).filter(otro -> !otro.getId().equals(docente.getId())).ifPresent(otro -> {
            throw ApiException.conflicto("Esa cuenta ya está vinculada a otro docente.");
        });
        docente.setUsuario(usuario);
    }

    static String nombreCurso(Asignacion a) {
        return a.getCurso().getNombre() + " (" + a.getSeccion().getGrado().getEtiqueta() + " " + a.getSeccion().getLetra() + ")";
    }

    private LocalDate inicioDelAnio() {
        return LocalDate.of(calendario.anioActual(), 1, 1);
    }

    private static int ordenRiesgo(String nivel) {
        return switch (nivel) {
            case "Crítico" -> 0;
            case "Alto" -> 1;
            default -> 2;
        };
    }
}

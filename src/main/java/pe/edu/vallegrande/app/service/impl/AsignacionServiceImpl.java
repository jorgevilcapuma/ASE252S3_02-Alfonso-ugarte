package pe.edu.vallegrande.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.AsignacionRequest;
import pe.edu.vallegrande.app.dto.request.AsignacionRequest.HorarioRequest;
import pe.edu.vallegrande.app.dto.response.AsignacionResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.Horario;
import pe.edu.vallegrande.app.repository.AsignacionRepository;
import pe.edu.vallegrande.app.repository.ComunicadoRepository;
import pe.edu.vallegrande.app.repository.SesionClaseRepository;
import pe.edu.vallegrande.app.service.AsignacionService;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class AsignacionServiceImpl implements AsignacionService {

    private final AsignacionRepository asignaciones;
    private final SesionClaseRepository sesiones;
    private final ComunicadoRepository comunicados;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionResponse> listar(Long docenteId, Long seccionId) {
        Map<Long, Long> totales = entidades.totalesPorSeccion();
        return asignaciones.listar(docenteId, seccionId).stream()
                .map(a -> AsignacionResponse.de(a, totales.getOrDefault(a.getSeccion().getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AsignacionResponse obtener(Long id) {
        Asignacion a = entidades.asignacion(id);
        return AsignacionResponse.de(a, entidades.totalesPorSeccion().getOrDefault(a.getSeccion().getId(), 0L));
    }

    @Override
    @Transactional
    public AsignacionResponse crear(AsignacionRequest request) {
        Asignacion asignacion = new Asignacion();
        aplicar(asignacion, request, null);
        asignaciones.save(asignacion);
        return AsignacionResponse.de(asignacion, entidades.totalesPorSeccion().getOrDefault(asignacion.getSeccion().getId(), 0L));
    }

    @Override
    @Transactional
    public AsignacionResponse actualizar(Long id, AsignacionRequest request) {
        Asignacion asignacion = entidades.asignacion(id);
        aplicar(asignacion, request, id);
        return AsignacionResponse.de(asignacion, entidades.totalesPorSeccion().getOrDefault(asignacion.getSeccion().getId(), 0L));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Asignacion asignacion = entidades.asignacion(id);
        if (sesiones.existsByAsignacionId(id)) {
            throw ApiException.conflicto("No se puede eliminar el curso porque ya tiene asistencia registrada.");
        }
        if (comunicados.existsByAsignacionId(id)) {
            throw ApiException.conflicto("No se puede eliminar el curso porque tiene comunicados publicados.");
        }
        asignaciones.delete(asignacion);
    }

    private void aplicar(Asignacion asignacion, AsignacionRequest request, Long idActual) {
        String codigo = Textos.limpiar(request.codigo()).toUpperCase();
        asignaciones.findByCodigoIgnoreCase(codigo).filter(otra -> !otra.getId().equals(idActual)).ifPresent(a -> {
            throw ApiException.conflicto("El código " + codigo + " ya está en uso.");
        });
        asignaciones.findByCursoIdAndSeccionId(request.cursoId(), request.seccionId()).filter(otra -> !otra.getId().equals(idActual))
                .ifPresent(a -> {
                    throw ApiException.conflicto("Ese curso ya está asignado a la sección (código " + a.getCodigo() + ").");
                });

        asignacion.setCodigo(codigo);
        asignacion.setCurso(entidades.curso(request.cursoId()));
        asignacion.setSeccion(entidades.seccion(request.seccionId()));
        asignacion.setDocente(entidades.docente(request.docenteId()));
        asignacion.setAula(Textos.limpiar(request.aula()));

        List<HorarioRequest> bloques = request.horarios() == null ? List.of() : request.horarios();
        validarHorarios(bloques, request, idActual);
        if (idActual != null) {
            // Hibernate inserta antes de borrar huérfanos: sin este flush, repetir un bloque violaría uq_horario_bloque.
            asignacion.getHorarios().clear();
            asignaciones.flush();
        }
        List<Horario> nuevos = new ArrayList<>();
        bloques.forEach(b -> nuevos.add(new Horario(b.dia(), b.inicio(), b.fin())));
        asignacion.reemplazarHorarios(nuevos);
    }

    /** Fin posterior al inicio, sin cruces dentro del curso ni con otros cursos de la sección o del docente. */
    private void validarHorarios(List<HorarioRequest> bloques, AsignacionRequest request, Long idActual) {
        for (int i = 0; i < bloques.size(); i++) {
            HorarioRequest b = bloques.get(i);
            if (!b.fin().isAfter(b.inicio())) {
                throw ApiException.validacion("En " + b.dia().getValor() + " la hora de fin debe ser posterior a la de inicio.");
            }
            for (int j = i + 1; j < bloques.size(); j++) {
                if (seCruzan(b, bloques.get(j))) {
                    throw ApiException.validacion("Los bloques del " + b.dia().getValor() + " se cruzan entre sí.");
                }
            }
        }
        List<Asignacion> relacionadas = new ArrayList<>(asignaciones.listar(null, request.seccionId()));
        relacionadas.addAll(asignaciones.listar(request.docenteId(), null));
        for (Asignacion otra : relacionadas) {
            if (otra.getId().equals(idActual)) {
                continue;
            }
            for (Horario h : otra.getHorarios()) {
                for (HorarioRequest b : bloques) {
                    if (h.getDia() == b.dia() && b.inicio().isBefore(h.getHoraFin()) && h.getHoraInicio().isBefore(b.fin())) {
                        String deQuien = otra.getSeccion().getId().equals(request.seccionId()) ? "de la sección" : "del docente";
                        throw ApiException.conflicto("El horario del " + b.dia().getValor() + " se cruza con " + otra.getCodigo()
                                + " (" + otra.getCurso().getNombre() + ") " + deQuien + ".");
                    }
                }
            }
        }
    }

    private static boolean seCruzan(HorarioRequest a, HorarioRequest b) {
        return a.dia() == b.dia() && a.inicio().isBefore(b.fin()) && b.inicio().isBefore(a.fin());
    }
}

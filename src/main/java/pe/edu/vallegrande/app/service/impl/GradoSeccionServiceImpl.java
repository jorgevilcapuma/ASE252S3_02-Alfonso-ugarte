package pe.edu.vallegrande.app.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.dto.request.GradoSeccionRequest;
import pe.edu.vallegrande.app.dto.response.GradoSeccionResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.enums.Nivel;
import pe.edu.vallegrande.app.model.enums.Turno;
import pe.edu.vallegrande.app.repository.CursoRepository;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.repository.GradoSeccionRepository;
import pe.edu.vallegrande.app.service.GradoSeccionService;
import pe.edu.vallegrande.app.util.FiltroEstado;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class GradoSeccionServiceImpl implements GradoSeccionService {

    private final GradoSeccionRepository gradosSecciones;
    private final EstudianteRepository estudiantes;
    private final CursoRepository cursos;
    private final Entidades entidades;

    public GradoSeccionServiceImpl(GradoSeccionRepository gradosSecciones, EstudianteRepository estudiantes, CursoRepository cursos, Entidades entidades) {
        this.gradosSecciones = gradosSecciones;
        this.estudiantes = estudiantes;
        this.cursos = cursos;
        this.entidades = entidades;
    }

    /**
     * Grados que existen en cada nivel: Inicial "3 años" a "5 años" (acepta "3"), Primaria 1 a 6 y Secundaria 1 a 5
     * (acepta "1°" o "1ro"). Devuelve el grado normalizado como lo guarda la tabla.
     */
    static String gradoValido(Nivel nivel, String grado) {
        String numero = grado == null ? "" : grado.replaceAll("(?i)\\s*(años?|°|º|ro|do|to|er)\\s*$", "").trim();
        int valor;
        try {
            valor = Integer.parseInt(numero);
        } catch (NumberFormatException ex) {
            valor = -1;
        }
        return switch (nivel) {
            case INICIAL -> {
                if (valor < 3 || valor > 5) {
                    throw ApiException.validacion("En Inicial el grado es 3, 4 o 5 años.");
                }
                yield valor + " años";
            }
            case PRIMARIA -> {
                if (valor < 1 || valor > 6) {
                    throw ApiException.validacion("En Primaria el grado va de 1 a 6.");
                }
                yield String.valueOf(valor);
            }
            case SECUNDARIA -> {
                if (valor < 1 || valor > 5) {
                    throw ApiException.validacion("En Secundaria el grado va de 1 a 5.");
                }
                yield String.valueOf(valor);
            }
        };
    }

    /** Regla del colegio usada por el frontend: la sección C funciona en la tarde. */
    public static Turno turnoPorDefecto(String seccion) {
        return "C".equalsIgnoreCase(seccion) ? Turno.TARDE : Turno.MANANA;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradoSeccionResponse> listar(Nivel nivel, String grado, String estado) {
        Map<Long, Long> totales = entidades.totalesPorAula();
        return gradosSecciones.listar(nivel, Textos.limpiar(grado), FiltroEstado.valor(estado)).stream()
                .map(g -> GradoSeccionResponse.de(g, totales.getOrDefault(g.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GradoSeccionResponse obtener(Long id) {
        return GradoSeccionResponse.de(entidades.gradoSeccion(id), entidades.totalesPorAula().getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public GradoSeccionResponse crear(GradoSeccionRequest request) {
        GradoSeccion aula = new GradoSeccion();
        aplicar(aula, request);
        return GradoSeccionResponse.de(gradosSecciones.save(aula), 0L);
    }

    @Override
    @Transactional
    public GradoSeccionResponse actualizar(Long id, GradoSeccionRequest request) {
        GradoSeccion aula = entidades.gradoSeccion(id);
        if (!aula.esActivo()) {
            throw ApiException.conflicto(aula.getNombre() + " está eliminada; restáurala antes de editarla.");
        }
        aplicar(aula, request);
        return GradoSeccionResponse.de(aula, entidades.totalesPorAula().getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public GradoSeccionResponse eliminar(Long id) {
        GradoSeccion aula = entidades.gradoSeccion(id);
        if (!aula.esActivo()) {
            throw ApiException.conflicto(aula.getNombre() + " ya está eliminada.");
        }
        if (estudiantes.existsByGradoSeccionIdAndEstado(id, FiltroEstado.ACTIVO)) {
            throw ApiException.conflicto("No se puede eliminar " + aula.getNombre() + " porque tiene estudiantes activos. Trasládalos primero.");
        }
        if (cursos.existsByGradoSeccionIdAndEstado(id, FiltroEstado.ACTIVO)) {
            throw ApiException.conflicto("No se puede eliminar " + aula.getNombre() + " porque tiene cursos activos. Elimínalos primero.");
        }
        aula.setEstado(FiltroEstado.INACTIVO);
        return GradoSeccionResponse.de(aula, 0L);
    }

    @Override
    @Transactional
    public GradoSeccionResponse restaurar(Long id) {
        GradoSeccion aula = entidades.gradoSeccion(id);
        if (aula.esActivo()) {
            throw ApiException.conflicto(aula.getNombre() + " ya está activa.");
        }
        aula.setEstado(FiltroEstado.ACTIVO);
        return GradoSeccionResponse.de(aula, entidades.totalesPorAula().getOrDefault(id, 0L));
    }

    private void aplicar(GradoSeccion aula, GradoSeccionRequest request) {
        String grado = gradoValido(request.nivel(), Textos.limpiar(request.grado()));
        String seccion = request.seccion().trim().toUpperCase();
        gradosSecciones.findByNivelAndGradoIgnoreCaseAndSeccionIgnoreCase(request.nivel(), grado, seccion)
                .filter(otra -> !otra.getId().equals(aula.getId()))
                .ifPresent(otra -> {
                    throw ApiException.conflicto("Ya existe " + otra.getNombre() + ".");
                });
        aula.setNivel(request.nivel());
        aula.setGrado(grado);
        aula.setSeccion(seccion);
        aula.setTurno(request.turno() == null ? turnoPorDefecto(seccion) : request.turno());
    }
}

package pe.edu.vallegrande.app.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.GradoRequest;
import pe.edu.vallegrande.app.dto.request.NivelRequest;
import pe.edu.vallegrande.app.dto.request.SeccionRequest;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.GradoResponse;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.NivelResponse;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.SeccionResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Grado;
import pe.edu.vallegrande.app.model.NivelEducativo;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.enums.Turno;
import pe.edu.vallegrande.app.repository.AsignacionRepository;
import pe.edu.vallegrande.app.repository.GradoRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.repository.NivelEducativoRepository;
import pe.edu.vallegrande.app.repository.SeccionRepository;
import pe.edu.vallegrande.app.service.EstructuraService;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class EstructuraServiceImpl implements EstructuraService {

    private final NivelEducativoRepository niveles;
    private final GradoRepository grados;
    private final SeccionRepository secciones;
    private final MatriculaRepository matriculas;
    private final AsignacionRepository asignaciones;
    private final Entidades entidades;

    /** Regla del colegio usada por el frontend: la sección C funciona en la tarde. */
    public static Turno turnoPorDefecto(String letra) {
        return "C".equalsIgnoreCase(letra) ? Turno.TARDE : Turno.MANANA;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NivelResponse> estructura() {
        List<Grado> todosLosGrados = grados.listarConNivel();
        List<Seccion> todasLasSecciones = secciones.listarConGrado();
        Map<Long, Long> totales = entidades.totalesPorSeccion();
        return niveles.findAllByOrderByOrdenAsc().stream()
                .map(nivel -> new NivelResponse(nivel.getId(), nivel.getNombre(), nivel.getOrden(),
                        todosLosGrados.stream()
                                .filter(g -> g.getNivel().getId().equals(nivel.getId()))
                                .map(g -> new GradoResponse(g.getId(), nivel.getId(), nivel.getNombre(), g.getCodigo(), g.getEtiqueta(), g.getOrden(),
                                        todasLasSecciones.stream()
                                                .filter(s -> s.getGrado().getId().equals(g.getId()))
                                                .map(s -> SeccionResponse.de(s, totales.getOrDefault(s.getId(), 0L)))
                                                .toList()))
                                .toList()))
                .toList();
    }

    // ---------- Niveles ----------

    @Override
    @Transactional(readOnly = true)
    public List<NivelResponse> listarNiveles() {
        return niveles.findAllByOrderByOrdenAsc().stream().map(NivelResponse::de).toList();
    }

    @Override
    @Transactional
    public NivelResponse crearNivel(NivelRequest request) {
        String nombre = Textos.limpiar(request.nombre());
        if (niveles.existsByNombreIgnoreCase(nombre)) {
            throw ApiException.conflicto("Ya existe el nivel " + nombre + ".");
        }
        return NivelResponse.de(niveles.save(new NivelEducativo(nombre, request.orden())));
    }

    @Override
    @Transactional
    public NivelResponse actualizarNivel(Long id, NivelRequest request) {
        NivelEducativo nivel = nivel(id);
        String nombre = Textos.limpiar(request.nombre());
        if (niveles.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw ApiException.conflicto("Ya existe el nivel " + nombre + ".");
        }
        nivel.setNombre(nombre);
        nivel.setOrden(request.orden());
        return NivelResponse.de(nivel);
    }

    @Override
    @Transactional
    public void eliminarNivel(Long id) {
        NivelEducativo nivel = nivel(id);
        if (grados.existsByNivelId(id)) {
            throw ApiException.conflicto("No se puede eliminar el nivel " + nivel.getNombre() + " porque tiene grados registrados.");
        }
        niveles.delete(nivel);
    }

    // ---------- Grados ----------

    @Override
    @Transactional(readOnly = true)
    public List<GradoResponse> listarGrados(Long nivelId) {
        List<Grado> lista = nivelId == null ? grados.listarConNivel() : grados.listarPorNivel(nivelId);
        return lista.stream().map(GradoResponse::de).toList();
    }

    @Override
    @Transactional
    public GradoResponse crearGrado(GradoRequest request) {
        NivelEducativo nivel = nivel(request.nivelId());
        String codigo = Textos.limpiar(request.codigo());
        grados.findByNivelIdAndCodigoIgnoreCase(nivel.getId(), codigo).ifPresent(g -> {
            throw ApiException.conflicto("El grado " + codigo + " ya existe en " + nivel.getNombre() + ".");
        });
        return GradoResponse.de(grados.save(new Grado(nivel, codigo, Textos.limpiar(request.etiqueta()), request.orden())));
    }

    @Override
    @Transactional
    public GradoResponse actualizarGrado(Long id, GradoRequest request) {
        Grado grado = grado(id);
        NivelEducativo nivel = nivel(request.nivelId());
        String codigo = Textos.limpiar(request.codigo());
        grados.findByNivelIdAndCodigoIgnoreCase(nivel.getId(), codigo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(g -> {
                    throw ApiException.conflicto("El grado " + codigo + " ya existe en " + nivel.getNombre() + ".");
                });
        if (!grado.getNivel().getId().equals(nivel.getId()) && secciones.existsByGradoId(id)) {
            throw ApiException.conflicto("No se puede cambiar de nivel un grado que ya tiene secciones.");
        }
        grado.setNivel(nivel);
        grado.setCodigo(codigo);
        grado.setEtiqueta(Textos.limpiar(request.etiqueta()));
        grado.setOrden(request.orden());
        return GradoResponse.de(grado);
    }

    @Override
    @Transactional
    public void eliminarGrado(Long id) {
        Grado grado = grado(id);
        if (secciones.existsByGradoId(id)) {
            throw ApiException.conflicto("No se puede eliminar el grado " + grado.getEtiqueta() + " porque tiene secciones registradas.");
        }
        grados.delete(grado);
    }

    // ---------- Secciones ----------

    @Override
    @Transactional(readOnly = true)
    public List<SeccionResponse> listarSecciones(String nivel, String grado) {
        Map<Long, Long> totales = entidades.totalesPorSeccion();
        return secciones.listarConGrado().stream()
                .filter(s -> nivel == null || s.getNivelNombre().equalsIgnoreCase(nivel.trim()))
                .filter(s -> grado == null || s.getGrado().getCodigo().equalsIgnoreCase(grado.trim()))
                .map(s -> SeccionResponse.de(s, totales.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeccionResponse obtenerSeccion(Long id) {
        return SeccionResponse.de(entidades.seccion(id), entidades.totalesPorSeccion().getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public SeccionResponse crearSeccion(SeccionRequest request) {
        Grado grado = grado(request.gradoId());
        String letra = request.letra().trim().toUpperCase();
        secciones.findByGradoIdAndLetraIgnoreCase(grado.getId(), letra).ifPresent(s -> {
            throw ApiException.conflicto("La sección " + letra + " ya existe en " + grado.getEtiqueta() + " de " + grado.getNivel().getNombre() + ".");
        });
        Turno turno = request.turno() == null ? turnoPorDefecto(letra) : request.turno();
        return SeccionResponse.de(secciones.save(new Seccion(grado, letra, turno)), 0L);
    }

    @Override
    @Transactional
    public SeccionResponse actualizarSeccion(Long id, SeccionRequest request) {
        Seccion seccion = entidades.seccion(id);
        Grado grado = grado(request.gradoId());
        String letra = request.letra().trim().toUpperCase();
        secciones.findByGradoIdAndLetraIgnoreCase(grado.getId(), letra)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(s -> {
                    throw ApiException.conflicto("La sección " + letra + " ya existe en " + grado.getEtiqueta() + " de " + grado.getNivel().getNombre() + ".");
                });
        seccion.setGrado(grado);
        seccion.setLetra(letra);
        seccion.setTurno(request.turno() == null ? turnoPorDefecto(letra) : request.turno());
        return SeccionResponse.de(seccion, entidades.totalesPorSeccion().getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public void eliminarSeccion(Long id) {
        Seccion seccion = entidades.seccion(id);
        if (matriculas.existsBySeccionId(id)) {
            throw ApiException.conflicto("No se puede eliminar la sección porque tiene estudiantes matriculados.");
        }
        if (asignaciones.existsBySeccionId(id)) {
            throw ApiException.conflicto("No se puede eliminar la sección porque tiene cursos asignados.");
        }
        secciones.delete(seccion);
    }

    private NivelEducativo nivel(Long id) {
        return niveles.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el nivel con id " + id + "."));
    }

    private Grado grado(Long id) {
        return grados.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el grado con id " + id + "."));
    }
}

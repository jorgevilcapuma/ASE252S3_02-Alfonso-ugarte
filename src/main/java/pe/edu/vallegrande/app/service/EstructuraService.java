package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.GradoRequest;
import pe.edu.vallegrande.app.dto.request.NivelRequest;
import pe.edu.vallegrande.app.dto.request.SeccionRequest;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.GradoResponse;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.NivelResponse;
import pe.edu.vallegrande.app.dto.response.EstructuraResponse.SeccionResponse;

/** Tablas maestras de la estructura del colegio: niveles, grados y secciones. */
public interface EstructuraService {

    /** Árbol completo nivel → grados → secciones (con alumnos matriculados por sección). */
    List<NivelResponse> estructura();

    List<NivelResponse> listarNiveles();

    NivelResponse crearNivel(NivelRequest request);

    NivelResponse actualizarNivel(Long id, NivelRequest request);

    void eliminarNivel(Long id);

    List<GradoResponse> listarGrados(Long nivelId);

    GradoResponse crearGrado(GradoRequest request);

    GradoResponse actualizarGrado(Long id, GradoRequest request);

    void eliminarGrado(Long id);

    List<SeccionResponse> listarSecciones(String nivel, String grado);

    SeccionResponse obtenerSeccion(Long id);

    SeccionResponse crearSeccion(SeccionRequest request);

    SeccionResponse actualizarSeccion(Long id, SeccionRequest request);

    void eliminarSeccion(Long id);
}

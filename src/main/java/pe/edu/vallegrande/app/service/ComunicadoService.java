package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.ComunicadoRequest;
import pe.edu.vallegrande.app.dto.response.ComunicadoResponse;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

public interface ComunicadoService {

    /** Administrador: todos. Docente: los suyos y los de sus cursos. Alumno: generales y los de su sección. */
    List<ComunicadoResponse> listar(Long cursoId, SesionUsuario sesion);

    ComunicadoResponse obtener(Long id);

    ComunicadoResponse crear(ComunicadoRequest request, SesionUsuario sesion);

    ComunicadoResponse actualizar(Long id, ComunicadoRequest request, SesionUsuario sesion);

    void eliminar(Long id, SesionUsuario sesion);
}

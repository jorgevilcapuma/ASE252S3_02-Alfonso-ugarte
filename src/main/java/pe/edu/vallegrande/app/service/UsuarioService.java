package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.CambioContrasenaRequest;
import pe.edu.vallegrande.app.dto.request.UsuarioRequest;
import pe.edu.vallegrande.app.dto.response.UsuarioResponse;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

/** Gestión de cuentas institucionales. */
public interface UsuarioService {

    List<UsuarioResponse> listar(Perfil perfil, String estado, String texto);

    UsuarioResponse obtener(Long id);

    UsuarioResponse crear(UsuarioRequest request);

    UsuarioResponse actualizar(Long id, UsuarioRequest request, SesionUsuario sesion);

    UsuarioResponse alternarEstado(Long id, SesionUsuario sesion);

    /** Eliminación lógica: la cuenta queda Inactiva y no puede ingresar. */
    UsuarioResponse desactivar(Long id, SesionUsuario sesion);

    UsuarioResponse restaurar(Long id);

    /** Eliminación física (solo cuentas sin registros asociados). */
    void eliminar(Long id, SesionUsuario sesion);

    void cambiarContrasena(Long id, CambioContrasenaRequest request, SesionUsuario sesion);
}

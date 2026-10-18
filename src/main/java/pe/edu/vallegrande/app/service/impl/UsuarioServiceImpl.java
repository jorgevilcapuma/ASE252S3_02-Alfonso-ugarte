package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.config.PropiedadesApp;
import pe.edu.vallegrande.app.dto.request.CambioContrasenaRequest;
import pe.edu.vallegrande.app.dto.request.UsuarioRequest;
import pe.edu.vallegrande.app.dto.response.UsuarioResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.repository.DocenteRepository;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.repository.UsuarioRepository;
import pe.edu.vallegrande.app.seguridad.HasherContrasena;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.UsuarioService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.FiltroEstado;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarios;
    private final DocenteRepository docentes;
    private final EstudianteRepository estudiantes;
    private final HasherContrasena hasher;
    private final PropiedadesApp propiedades;
    private final Calendario calendario;
    private final Entidades entidades;

    public UsuarioServiceImpl(UsuarioRepository usuarios, DocenteRepository docentes, EstudianteRepository estudiantes, HasherContrasena hasher, PropiedadesApp propiedades, Calendario calendario, Entidades entidades) {
        this.usuarios = usuarios;
        this.docentes = docentes;
        this.estudiantes = estudiantes;
        this.hasher = hasher;
        this.propiedades = propiedades;
        this.calendario = calendario;
        this.entidades = entidades;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Perfil perfil, String estado, String texto) {
        String filtro = Textos.limpiar(texto) == null ? null : "%" + Textos.limpiar(texto).toLowerCase() + "%";
        String valor = FiltroEstado.valor(estado);
        EstadoUsuario estadoUsuario = valor == null ? null : FiltroEstado.ACTIVO.equals(valor) ? EstadoUsuario.ACTIVO : EstadoUsuario.INACTIVO;
        return usuarios.buscar(perfil, estadoUsuario, filtro).stream().map(this::respuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return respuesta(entidades.usuario(id));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String correo = Textos.correo(request.correo());
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflicto("Ese correo ya está registrado.");
        }
        Usuario usuario = new Usuario();
        usuario.setCreado(calendario.hoy());
        String contrasena = request.contrasena() == null || request.contrasena().isBlank()
                ? propiedades.seguridad().contrasenaInicial() : request.contrasena();
        usuario.setContrasenaHash(hasher.generar(contrasena));
        aplicar(usuario, request, correo);
        usuarios.save(usuario);
        vincular(usuario, request);
        return respuesta(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request, SesionUsuario sesion) {
        Usuario usuario = entidades.usuario(id);
        String correo = Textos.correo(request.correo());
        if (usuarios.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw ApiException.conflicto("Ese correo ya está registrado.");
        }
        boolean dejaDeSerAdminActivo = usuario.getPerfil() == Perfil.ADMINISTRADOR && usuario.getEstado() == EstadoUsuario.ACTIVO
                && (request.perfil() != Perfil.ADMINISTRADOR || request.estado() == EstadoUsuario.INACTIVO);
        if (dejaDeSerAdminActivo) {
            protegerAdministrador(usuario, sesion);
        }
        if (usuario.getPerfil() != request.perfil()) {
            desvincular(usuario);
        }
        aplicar(usuario, request, correo);
        if (request.contrasena() != null && !request.contrasena().isBlank()) {
            usuario.setContrasenaHash(hasher.generar(request.contrasena()));
        }
        vincular(usuario, request);
        // La ficha vinculada usa el mismo correo que su cuenta.
        docentes.findByUsuarioId(usuario.getId()).ifPresent(d -> d.setCorreo(usuario.getCorreo()));
        estudiantes.findByUsuarioId(usuario.getId()).ifPresent(e -> e.setCorreo(usuario.getCorreo()));
        return respuesta(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse alternarEstado(Long id, SesionUsuario sesion) {
        Usuario usuario = entidades.usuario(id);
        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
            if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
                protegerAdministrador(usuario, sesion);
            }
            usuario.setEstado(EstadoUsuario.INACTIVO);
        } else {
            usuario.setEstado(EstadoUsuario.ACTIVO);
        }
        return respuesta(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse desactivar(Long id, SesionUsuario sesion) {
        Usuario usuario = entidades.usuario(id);
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw ApiException.conflicto("La cuenta ya está eliminada (inactiva).");
        }
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            protegerAdministrador(usuario, sesion);
        }
        usuario.setEstado(EstadoUsuario.INACTIVO);
        return respuesta(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse restaurar(Long id) {
        Usuario usuario = entidades.usuario(id);
        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
            throw ApiException.conflicto("La cuenta ya está activa.");
        }
        usuario.setEstado(EstadoUsuario.ACTIVO);
        return respuesta(usuario);
    }

    @Override
    @Transactional
    public void eliminar(Long id, SesionUsuario sesion) {
        Usuario usuario = entidades.usuario(id);
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR && usuario.getEstado() == EstadoUsuario.ACTIVO) {
            protegerAdministrador(usuario, sesion);
        }
        desvincular(usuario);
        try {
            usuarios.delete(usuario);
            usuarios.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflicto("La cuenta tiene registros asociados (asistencias o revisiones de justificaciones). Desactívala en lugar de eliminarla.");
        }
    }

    @Override
    @Transactional
    public void cambiarContrasena(Long id, CambioContrasenaRequest request, SesionUsuario sesion) {
        Usuario usuario = entidades.usuario(id);
        boolean propia = id.equals(sesion.usuarioId());
        if (!propia && !sesion.esAdministrador()) {
            throw ApiException.prohibido("Solo puedes cambiar tu propia contraseña.");
        }
        if (propia && !hasher.coincide(request.contrasenaActual(), usuario.getContrasenaHash())) {
            throw ApiException.validacion("La contraseña actual no es correcta.");
        }
        usuario.setContrasenaHash(hasher.generar(request.contrasenaNueva()));
    }

    private void aplicar(Usuario usuario, UsuarioRequest request, String correo) {
        String nombre = Textos.limpiar(request.nombre());
        if (nombre.split(" ").length < 2) {
            throw ApiException.validacion("Ingresa nombres y apellidos.");
        }
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setPerfil(request.perfil());
        usuario.setEstado(request.estado() == null ? EstadoUsuario.ACTIVO : request.estado());
    }

    /** Vincula la cuenta a la ficha del docente o del estudiante indicada. */
    private void vincular(Usuario usuario, UsuarioRequest request) {
        if (request.docenteId() != null) {
            if (usuario.getPerfil() == Perfil.ALUMNO) {
                throw ApiException.validacion("Solo una cuenta de perfil docente o administrador se puede vincular a un docente o personal.");
            }
            Docente docente = entidades.docente(request.docenteId());
            if (docente.getUsuario() != null && !docente.getUsuario().getId().equals(usuario.getId())) {
                throw ApiException.conflicto("Ese docente ya tiene otra cuenta vinculada.");
            }
            docentes.findByUsuarioId(usuario.getId()).filter(otro -> !otro.getId().equals(docente.getId())).ifPresent(otro -> otro.setUsuario(null));
            docente.setUsuario(usuario);
            docente.setCorreo(usuario.getCorreo());
        }
        if (request.estudianteId() != null) {
            if (usuario.getPerfil() != Perfil.ALUMNO) {
                throw ApiException.validacion("Solo una cuenta de perfil alumno se puede vincular a un estudiante.");
            }
            Estudiante estudiante = entidades.estudiante(request.estudianteId());
            if (estudiante.getUsuario() != null && !estudiante.getUsuario().getId().equals(usuario.getId())) {
                throw ApiException.conflicto("Ese estudiante ya tiene otra cuenta vinculada.");
            }
            estudiantes.findByUsuarioId(usuario.getId()).filter(otro -> !otro.getId().equals(estudiante.getId())).ifPresent(otro -> otro.setUsuario(null));
            estudiante.setUsuario(usuario);
            estudiante.setCorreo(usuario.getCorreo());
        }
    }

    private void desvincular(Usuario usuario) {
        if (usuario.getId() == null) {
            return;
        }
        docentes.findByUsuarioId(usuario.getId()).ifPresent(d -> d.setUsuario(null));
        estudiantes.findByUsuarioId(usuario.getId()).ifPresent(e -> e.setUsuario(null));
    }

    /** Evita quedarse sin administradores activos o desactivarse a uno mismo. */
    private void protegerAdministrador(Usuario usuario, SesionUsuario sesion) {
        if (usuario.getId().equals(sesion.usuarioId())) {
            throw ApiException.conflicto("No puedes desactivar, eliminar ni quitar el perfil de administrador a tu propia cuenta.");
        }
        if (usuarios.countByPerfilAndEstado(Perfil.ADMINISTRADOR, EstadoUsuario.ACTIVO) <= 1) {
            throw ApiException.conflicto("Debe quedar al menos un administrador activo.");
        }
    }

    private UsuarioResponse respuesta(Usuario usuario) {
        Long docenteId = docentes.findByUsuarioId(usuario.getId()).map(Docente::getId).orElse(null);
        Long estudianteId = estudiantes.findByUsuarioId(usuario.getId()).map(Estudiante::getId).orElse(null);
        return UsuarioResponse.de(usuario, docenteId, estudianteId);
    }
}

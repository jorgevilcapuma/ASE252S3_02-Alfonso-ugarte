package pe.edu.vallegrande.app.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.dto.request.LoginRequest;
import pe.edu.vallegrande.app.dto.response.LoginResponse;
import pe.edu.vallegrande.app.dto.response.SesionResponse;
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
import pe.edu.vallegrande.app.seguridad.TokenService;
import pe.edu.vallegrande.app.service.AuthService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String CORREO = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final UsuarioRepository usuarios;
    private final DocenteRepository docentes;
    private final EstudianteRepository estudiantes;
    private final HasherContrasena hasher;
    private final TokenService tokens;
    private final Calendario calendario;

    public AuthServiceImpl(UsuarioRepository usuarios, DocenteRepository docentes, EstudianteRepository estudiantes, HasherContrasena hasher, TokenService tokens, Calendario calendario) {
        this.usuarios = usuarios;
        this.docentes = docentes;
        this.estudiantes = estudiantes;
        this.hasher = hasher;
        this.tokens = tokens;
        this.calendario = calendario;
    }

    @Override
    @Transactional
    public LoginResponse ingresar(LoginRequest request) {
        // El alumno solo ingresa con su nombre completo y DNI; administrador y docente, con correo y contraseña.
        SesionUsuario sesion = request.perfil() == Perfil.ALUMNO ? ingresarAlumnoConDni(request) : ingresarConCorreo(request);
        return new LoginResponse(tokens.generar(sesion), sesion.expiraEn(), SesionResponse.de(sesion));
    }

    @Override
    public SesionResponse sesion(SesionUsuario sesion) {
        return SesionResponse.de(sesion);
    }

    private SesionUsuario ingresarConCorreo(LoginRequest request) {
        String correo = Textos.correo(request.correo());
        if (correo == null || !correo.matches(CORREO)) {
            throw ApiException.validacion("Ingresa un correo institucional válido.");
        }
        if (request.contrasena() == null || request.contrasena().length() < 6) {
            throw ApiException.validacion("La contraseña debe tener 6 caracteres como mínimo.");
        }
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo)
                .filter(u -> hasher.coincide(request.contrasena(), u.getContrasenaHash()))
                .orElseThrow(() -> ApiException.noAutorizado("Correo o contraseña incorrectos."));
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw ApiException.prohibido("Esta cuenta está desactivada. Comunícate con el administrador.");
        }
        if (usuario.getPerfil() != request.perfil()) {
            throw ApiException.prohibido("Esta cuenta pertenece al perfil " + etiqueta(usuario.getPerfil()) + ".");
        }

        Long docenteId = null;
        Long estudianteId = null;
        String dni = null;
        if (usuario.getPerfil() == Perfil.DOCENTE) {
            Docente docente = docentes.findByUsuarioId(usuario.getId()).orElse(null);
            docenteId = docente == null ? null : docente.getId();
            dni = docente == null ? null : docente.getDni();
        } else if (usuario.getPerfil() == Perfil.ALUMNO) {
            Estudiante estudiante = estudiantes.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> ApiException.prohibido("Tu cuenta no está vinculada a un estudiante. Comunícate con el administrador."));
            verificarActivo(estudiante);
            estudianteId = estudiante.getId();
            dni = estudiante.getDni();
        }
        usuario.setUltimoAcceso(calendario.ahora());
        return new SesionUsuario(usuario.getId(), usuario.getCorreo(), usuario.getNombre(), usuario.getPerfil(),
                estudianteId, docenteId, dni, tokens.calcularExpiracion());
    }

    /** El alumno ingresa con su nombre completo y DNI, igual que en el formulario del portal. */
    private SesionUsuario ingresarAlumnoConDni(LoginRequest request) {
        String nombre = Textos.limpiar(request.nombre());
        String dni = request.dni() == null ? "" : request.dni().trim();
        if (nombre == null || nombre.split(" ").length < 2) {
            throw ApiException.validacion("Ingresa tu nombre completo con al menos un apellido.");
        }
        if (!dni.matches("\\d{8}")) {
            throw ApiException.validacion("El DNI debe tener exactamente 8 dígitos.");
        }
        Estudiante estudiante = estudiantes.findByDni(dni)
                .filter(e -> Textos.clave(e.getNombreCompleto()).equals(Textos.clave(nombre)))
                .orElseThrow(() -> ApiException.noAutorizado("No encontramos un estudiante con ese nombre y DNI. Verifica tus datos."));
        Usuario cuenta = estudiante.getUsuario();
        if (cuenta != null && cuenta.getEstado() == EstadoUsuario.INACTIVO) {
            throw ApiException.prohibido("Esta cuenta está desactivada. Comunícate con el administrador.");
        }
        verificarActivo(estudiante);
        String correo = cuenta != null ? cuenta.getCorreo() : (estudiante.getCorreo() == null ? "" : estudiante.getCorreo());
        return new SesionUsuario(cuenta == null ? null : cuenta.getId(), correo, estudiante.getNombreCompleto(), Perfil.ALUMNO,
                estudiante.getId(), null, estudiante.getDni(), tokens.calcularExpiracion());
    }

    private static void verificarActivo(Estudiante estudiante) {
        if (!estudiante.esActivo()) {
            throw ApiException.prohibido("Tu registro de estudiante no está activo. Comunícate con el colegio.");
        }
    }

    private static String etiqueta(Perfil perfil) {
        return switch (perfil) {
            case ADMINISTRADOR -> "Administrador";
            case DOCENTE -> "Docente";
            case ALUMNO -> "Alumno";
        };
    }
}

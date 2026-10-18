package pe.edu.vallegrande.app.service.impl;

import java.util.Set;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import pe.edu.vallegrande.app.config.PropiedadesApp;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.repository.UsuarioRepository;
import pe.edu.vallegrande.app.seguridad.HasherContrasena;
import pe.edu.vallegrande.app.util.Calendario;

/**
 * Cuenta de acceso de un docente: el correo de su ficha ES el de su cuenta (los alumnos ingresan con nombre y DNI).
 * Registrar a la persona con correo crea (o vincula) su cuenta en el mismo paso; editar la ficha mantiene la cuenta
 * al día; eliminarla o restaurarla desactiva o reactiva la cuenta.
 */
@Component
class CuentasAcceso {

    private final UsuarioRepository usuarios;
    private final HasherContrasena hasher;
    private final PropiedadesApp propiedades;
    private final Calendario calendario;

    CuentasAcceso(UsuarioRepository usuarios, HasherContrasena hasher, PropiedadesApp propiedades, Calendario calendario) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.propiedades = propiedades;
        this.calendario = calendario;
    }

    /**
     * Devuelve la cuenta que debe quedar vinculada:
     * <ul>
     *   <li>ya tenía cuenta: se le actualizan el nombre y, si cambió, el correo;</li>
     *   <li>sin cuenta y con un correo que ya es de una cuenta libre de un perfil admitido: se vincula esa;</li>
     *   <li>sin cuenta y con un correo nuevo: se crea con el perfil indicado y la contraseña inicial.</li>
     * </ul>
     * Sin correo no se crea nada y una cuenta existente se conserva.
     *
     * @param vinculadaAOtra indica si la cuenta con ese id ya pertenece a otra persona
     */
    Usuario sincronizar(Usuario actual, String nombre, String correo, Perfil perfilNuevo, Set<Perfil> admitidos,
                        Predicate<Long> vinculadaAOtra) {
        if (actual != null) {
            if (correo != null && !correo.equalsIgnoreCase(actual.getCorreo())) {
                if (usuarios.existsByCorreoIgnoreCaseAndIdNot(correo, actual.getId())) {
                    throw ApiException.conflicto("El correo " + correo + " ya pertenece a otra cuenta del portal.");
                }
                actual.setCorreo(correo);
            }
            actual.setNombre(nombre);
            return actual;
        }
        if (correo == null) {
            return null;
        }
        Usuario existente = usuarios.findByCorreoIgnoreCase(correo).orElse(null);
        if (existente != null) {
            if (!admitidos.contains(existente.getPerfil())) {
                throw ApiException.conflicto("El correo " + correo + " ya es de una cuenta de perfil " + existente.getPerfil().getValor()
                        + ". Usa otro correo.");
            }
            if (vinculadaAOtra.test(existente.getId())) {
                throw ApiException.conflicto("El correo " + correo + " ya es la cuenta de otra persona. Usa otro correo.");
            }
            return existente;
        }
        Usuario nueva = new Usuario();
        nueva.setNombre(nombre);
        nueva.setCorreo(correo);
        nueva.setPerfil(perfilNuevo);
        nueva.setEstado(EstadoUsuario.ACTIVO);
        nueva.setContrasenaHash(hasher.generar(propiedades.seguridad().contrasenaInicial()));
        nueva.setCreado(calendario.hoy());
        return usuarios.save(nueva);
    }

    /** Eliminar a la persona desactiva su cuenta y restaurarla la reactiva (las cuentas de administrador no se tocan). */
    void activar(Usuario usuario, boolean activa) {
        if (usuario != null && usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            usuario.setEstado(activa ? EstadoUsuario.ACTIVO : EstadoUsuario.INACTIVO);
        }
    }
}

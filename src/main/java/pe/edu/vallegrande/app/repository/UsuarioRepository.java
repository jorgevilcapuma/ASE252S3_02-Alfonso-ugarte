package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    long countByPerfilAndEstado(Perfil perfil, EstadoUsuario estado);

    @Query("""
            select u from Usuario u
            where (:perfil is null or u.perfil = :perfil) and (:estado is null or u.estado = :estado)
              and (:texto is null or lower(u.nombre) like :texto or lower(u.correo) like :texto)
            order by u.creado desc, u.id desc
            """)
    List<Usuario> buscar(Perfil perfil, EstadoUsuario estado, String texto);
}

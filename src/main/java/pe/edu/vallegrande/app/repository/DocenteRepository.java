package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Long> {

    /** Filtros opcionales: estado (A/I) y cargo. */
    @Query("""
            select d from Docente d left join fetch d.usuario
            where (:estado is null or d.estado = :estado) and (:cargo is null or d.cargo = :cargo)
            order by d.apellidos, d.nombres
            """)
    List<Docente> listar(String estado, String cargo);

    @Query("select d from Docente d left join fetch d.usuario where d.id = :id")
    Optional<Docente> buscar(Long id);

    Optional<Docente> findByUsuarioId(Long usuarioId);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);
}

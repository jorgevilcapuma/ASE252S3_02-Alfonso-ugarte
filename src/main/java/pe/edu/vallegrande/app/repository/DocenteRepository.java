package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Long> {

    @Query("select d from Docente d left join fetch d.usuario order by d.apellidos, d.nombres")
    List<Docente> listar();

    @Query("select d from Docente d left join fetch d.usuario where d.id = :id")
    Optional<Docente> buscar(Long id);

    Optional<Docente> findByUsuarioId(Long usuarioId);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);
}

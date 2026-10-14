package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.vallegrande.app.model.Curso;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findAllByOrderByNombreAsc();

    Optional<Curso> findByNombreIgnoreCase(String nombre);
}

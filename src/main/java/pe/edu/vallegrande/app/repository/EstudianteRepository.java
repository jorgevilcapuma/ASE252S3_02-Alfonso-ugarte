package pe.edu.vallegrande.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.vallegrande.app.model.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByDni(String dni);

    Optional<Estudiante> findByUsuarioId(Long usuarioId);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);
}

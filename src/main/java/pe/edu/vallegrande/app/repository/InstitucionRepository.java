package pe.edu.vallegrande.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.vallegrande.app.model.Institucion;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {

    Optional<Institucion> findFirstByOrderByIdAsc();
}

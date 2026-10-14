package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.vallegrande.app.model.NivelEducativo;

public interface NivelEducativoRepository extends JpaRepository<NivelEducativo, Long> {

    List<NivelEducativo> findAllByOrderByOrdenAsc();

    Optional<NivelEducativo> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByNombreIgnoreCase(String nombre);
}

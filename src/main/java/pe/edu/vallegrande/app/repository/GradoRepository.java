package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Grado;

public interface GradoRepository extends JpaRepository<Grado, Long> {

    @Query("select g from Grado g join fetch g.nivel n order by n.orden, g.orden")
    List<Grado> listarConNivel();

    @Query("select g from Grado g join fetch g.nivel n where n.id = :nivelId order by g.orden")
    List<Grado> listarPorNivel(Long nivelId);

    Optional<Grado> findByNivelIdAndCodigoIgnoreCase(Long nivelId, String codigo);

    boolean existsByNivelId(Long nivelId);
}

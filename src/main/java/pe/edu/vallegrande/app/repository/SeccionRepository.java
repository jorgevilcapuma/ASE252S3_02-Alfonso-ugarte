package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Seccion;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    @Query("select s from Seccion s join fetch s.grado g join fetch g.nivel n order by n.orden, g.orden, s.letra")
    List<Seccion> listarConGrado();

    @Query("select s from Seccion s join fetch s.grado g join fetch g.nivel n where s.id = :id")
    Optional<Seccion> buscarConGrado(Long id);

    @Query("""
            select s from Seccion s join fetch s.grado g join fetch g.nivel n
            where lower(n.nombre) = lower(:nivel) and lower(g.codigo) = lower(:grado) and lower(s.letra) = lower(:letra)
            """)
    Optional<Seccion> buscar(String nivel, String grado, String letra);

    Optional<Seccion> findByGradoIdAndLetraIgnoreCase(Long gradoId, String letra);

    boolean existsByGradoId(Long gradoId);
}

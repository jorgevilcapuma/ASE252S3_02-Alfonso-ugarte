package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.enums.Nivel;

public interface GradoSeccionRepository extends JpaRepository<GradoSeccion, Long> {

    /** Aulas ordenadas por nivel, grado y sección (filtros opcionales). */
    @Query("""
            select g from GradoSeccion g
            where (:nivel is null or g.nivel = :nivel) and (:grado is null or g.grado = :grado)
              and (:estado is null or g.estado = :estado)
            order by g.nivel, length(g.grado), g.grado, g.seccion
            """)
    List<GradoSeccion> listar(Nivel nivel, String grado, String estado);

    Optional<GradoSeccion> findByNivelAndGradoIgnoreCaseAndSeccionIgnoreCase(Nivel nivel, String grado, String seccion);
}

package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Curso;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    /** Filtros opcionales: aula, docente y estado (A/I). */
    @Query("""
            select c from Curso c join fetch c.gradoSeccion g join fetch c.docente d
            where (:gradoSeccionId is null or g.id = :gradoSeccionId) and (:docenteId is null or d.id = :docenteId)
              and (:estado is null or c.estado = :estado)
            order by g.nivel, length(g.grado), g.grado, g.seccion, c.nombre
            """)
    List<Curso> listar(Long gradoSeccionId, Long docenteId, String estado);

    @Query("select c from Curso c join fetch c.gradoSeccion join fetch c.docente where c.id = :id")
    Optional<Curso> buscar(Long id);

    boolean existsByNombreIgnoreCaseAndGradoSeccionId(String nombre, Long gradoSeccionId);

    boolean existsByNombreIgnoreCaseAndGradoSeccionIdAndIdNot(String nombre, Long gradoSeccionId, Long id);

    boolean existsByGradoSeccionIdAndEstado(Long gradoSeccionId, String estado);

    boolean existsByDocenteIdAndEstado(Long docenteId, String estado);
}

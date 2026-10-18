package pe.edu.vallegrande.app.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.AsistenciaEstudiante;

/** En todas las consultas cursoId null significa la asistencia diaria del aula (registros sin curso). */
public interface AsistenciaEstudianteRepository extends JpaRepository<AsistenciaEstudiante, Long> {

    @Query("""
            select a from AsistenciaEstudiante a
            where a.estudiante.id = :estudianteId and a.fecha = :fecha
              and ((:cursoId is null and a.curso is null) or a.curso.id = :cursoId)
            """)
    Optional<AsistenciaEstudiante> buscar(Long estudianteId, Long cursoId, LocalDate fecha);

    @Query("""
            select a from AsistenciaEstudiante a
            where a.estudiante.id in :estudianteIds and a.fecha between :desde and :hasta
              and ((:cursoId is null and a.curso is null) or a.curso.id = :cursoId)
            """)
    List<AsistenciaEstudiante> listar(Collection<Long> estudianteIds, Long cursoId, LocalDate desde, LocalDate hasta);

    /** Historial de un estudiante: diario y de todos sus cursos, del más reciente al más antiguo. */
    @Query("""
            select a from AsistenciaEstudiante a left join fetch a.curso
            where a.estudiante.id = :estudianteId and a.fecha between :desde and :hasta
            order by a.fecha desc, a.id
            """)
    List<AsistenciaEstudiante> historial(Long estudianteId, LocalDate desde, LocalDate hasta);

    boolean existsByCursoId(Long cursoId);
}

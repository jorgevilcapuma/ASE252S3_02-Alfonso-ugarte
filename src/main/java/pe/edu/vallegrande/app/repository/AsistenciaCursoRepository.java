package pe.edu.vallegrande.app.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.AsistenciaCurso;

public interface AsistenciaCursoRepository extends JpaRepository<AsistenciaCurso, Long> {

    /** Historial del alumno por curso (portal del alumno). */
    @Query("""
            select r from AsistenciaCurso r join fetch r.sesion s
            join fetch s.asignacion a join fetch a.curso join fetch a.docente
            where r.estudiante.id = :estudianteId and s.fecha between :desde and :hasta
              and (:asignacionId is null or a.id = :asignacionId)
            order by s.fecha desc
            """)
    List<AsistenciaCurso> historialAlumno(Long estudianteId, Long asignacionId, LocalDate desde, LocalDate hasta);

    /** Registros de los cursos (de un docente o de una asignación), ordenados por fecha ascendente. */
    @Query("""
            select r from AsistenciaCurso r join fetch r.sesion s join fetch s.asignacion a join fetch a.curso
            join fetch a.seccion sec join fetch sec.grado g join fetch g.nivel join fetch r.estudiante
            where (:docenteId is null or a.docente.id = :docenteId) and (:asignacionId is null or a.id = :asignacionId)
              and s.fecha between :desde and :hasta
            order by s.fecha asc
            """)
    List<AsistenciaCurso> registrosDeCursos(Long docenteId, Long asignacionId, LocalDate desde, LocalDate hasta);

    @Query("""
            select r from AsistenciaCurso r
            where r.estudiante.id = :estudianteId and r.sesion.asignacion.id = :asignacionId and r.sesion.fecha = :fecha
            """)
    Optional<AsistenciaCurso> buscar(Long estudianteId, Long asignacionId, LocalDate fecha);

    boolean existsByEstudianteId(Long estudianteId);
}

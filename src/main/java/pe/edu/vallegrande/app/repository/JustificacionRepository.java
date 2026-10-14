package pe.edu.vallegrande.app.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Justificacion;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;

public interface JustificacionRepository extends JpaRepository<Justificacion, Long> {

    @Query("""
            select j from Justificacion j join fetch j.estudiante e
            left join fetch j.asignacion a left join fetch a.curso left join fetch j.revisadoPor
            where (:estado is null or j.estado = :estado) and (:estudianteId is null or e.id = :estudianteId)
              and (:asignacionId is null or a.id = :asignacionId) and (:docenteId is null or a.docente.id = :docenteId)
              and (:desde is null or j.fecha >= :desde) and (:hasta is null or j.fecha <= :hasta)
            order by j.fechaSolicitud desc, j.id desc
            """)
    List<Justificacion> buscar(EstadoJustificacion estado, Long estudianteId, Long asignacionId, Long docenteId,
                               LocalDate desde, LocalDate hasta);

    @Query("""
            select j from Justificacion j join fetch j.estudiante e
            left join fetch j.asignacion a left join fetch a.curso left join fetch j.revisadoPor
            where j.id = :id
            """)
    Optional<Justificacion> buscarCompleta(Long id);

    /** Solicitudes del mismo día y del mismo curso (o ambas sin curso) en un estado dado. */
    @Query("""
            select count(j) > 0 from Justificacion j
            where j.estudiante.id = :estudianteId and j.fecha = :fecha and j.estado = :estado
              and ((:asignacionId is null and j.asignacion is null) or j.asignacion.id = :asignacionId)
            """)
    boolean existeSolicitud(Long estudianteId, LocalDate fecha, Long asignacionId, EstadoJustificacion estado);

    long countByEstado(EstadoJustificacion estado);

    boolean existsByEstudianteId(Long estudianteId);
}

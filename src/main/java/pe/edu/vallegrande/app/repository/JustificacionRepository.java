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
            left join fetch j.curso c left join fetch j.revisadoPor
            where (:estado is null or j.estado = :estado) and (:estudianteId is null or e.id = :estudianteId)
              and (:cursoId is null or c.id = :cursoId) and (:docenteId is null or c.docente.id = :docenteId)
              and (:desde is null or j.fecha >= :desde) and (:hasta is null or j.fecha <= :hasta)
            order by j.fechaSolicitud desc, j.id desc
            """)
    List<Justificacion> buscar(EstadoJustificacion estado, Long estudianteId, Long cursoId, Long docenteId,
                               LocalDate desde, LocalDate hasta);

    @Query("""
            select j from Justificacion j join fetch j.estudiante e
            left join fetch j.curso c left join fetch c.docente left join fetch j.revisadoPor
            where j.id = :id
            """)
    Optional<Justificacion> buscarCompleta(Long id);

    /** Solicitudes del mismo día y del mismo curso (o ambas sin curso) en un estado dado. */
    @Query("""
            select count(j) > 0 from Justificacion j
            where j.estudiante.id = :estudianteId and j.fecha = :fecha and j.estado = :estado
              and ((:cursoId is null and j.curso is null) or j.curso.id = :cursoId)
            """)
    boolean existeSolicitud(Long estudianteId, LocalDate fecha, Long cursoId, EstadoJustificacion estado);

    boolean existsByCursoId(Long cursoId);
}

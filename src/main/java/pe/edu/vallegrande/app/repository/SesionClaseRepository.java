package pe.edu.vallegrande.app.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.SesionClase;

public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {

    @Query("select distinct s from SesionClase s left join fetch s.registros r where s.asignacion.id = :asignacionId and s.fecha = :fecha")
    Optional<SesionClase> buscar(Long asignacionId, LocalDate fecha);

    @Query("""
            select distinct s from SesionClase s
            join fetch s.asignacion a join fetch a.curso join fetch a.seccion sec join fetch sec.grado g join fetch g.nivel
            left join fetch s.registros
            where (:docenteId is null or a.docente.id = :docenteId) and (:asignacionId is null or a.id = :asignacionId)
              and s.fecha between :desde and :hasta
            order by s.fecha desc
            """)
    List<SesionClase> historial(Long docenteId, Long asignacionId, LocalDate desde, LocalDate hasta);

    boolean existsByAsignacionId(Long asignacionId);
}

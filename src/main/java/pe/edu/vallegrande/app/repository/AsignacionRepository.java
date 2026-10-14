package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Asignacion;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    String CONSULTA = """
            select distinct a from Asignacion a
            join fetch a.curso c join fetch a.docente d
            join fetch a.seccion s join fetch s.grado g join fetch g.nivel n
            left join fetch a.horarios h
            """;

    @Query(CONSULTA + """
            where (:docenteId is null or d.id = :docenteId) and (:seccionId is null or s.id = :seccionId)
            order by n.orden, g.orden, s.letra, c.nombre
            """)
    List<Asignacion> listar(Long docenteId, Long seccionId);

    @Query(CONSULTA + "where a.id = :id")
    Optional<Asignacion> buscarCompleta(Long id);

    Optional<Asignacion> findByCodigoIgnoreCase(String codigo);

    Optional<Asignacion> findByCursoIdAndSeccionId(Long cursoId, Long seccionId);

    boolean existsByCursoId(Long cursoId);

    boolean existsBySeccionId(Long seccionId);

    boolean existsByDocenteId(Long docenteId);
}

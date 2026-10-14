package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Comunicado;

public interface ComunicadoRepository extends JpaRepository<Comunicado, Long> {

    String CONSULTA = """
            select c from Comunicado c left join fetch c.asignacion a left join fetch a.curso
            left join fetch a.seccion s left join fetch s.grado g left join fetch g.nivel left join fetch c.autor
            """;

    @Query(CONSULTA + """
            where (:asignacionId is null or a.id = :asignacionId)
              and (:docenteId is null or a.docente.id = :docenteId or c.autor.id = :autorId)
            order by c.fechaCreacion desc, c.id desc
            """)
    List<Comunicado> buscar(Long asignacionId, Long docenteId, Long autorId);

    /** Comunicados generales (sin curso) y los de los cursos de la sección del alumno. */
    @Query(CONSULTA + "where a is null or s.id = :seccionId order by c.fechaCreacion desc, c.id desc")
    List<Comunicado> visiblesParaSeccion(Long seccionId);

    @Query(CONSULTA + "where c.id = :id")
    Optional<Comunicado> buscarCompleto(Long id);

    boolean existsByAsignacionId(Long asignacionId);
}

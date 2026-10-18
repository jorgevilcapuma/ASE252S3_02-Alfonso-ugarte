package pe.edu.vallegrande.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    /** Filtros opcionales: aula, estado (A/I) y texto en nombres, apellidos o DNI (en minúsculas, con %). */
    @Query("""
            select e from Estudiante e join fetch e.gradoSeccion
            where (:gradoSeccionId is null or e.gradoSeccion.id = :gradoSeccionId)
              and (:estado is null or e.estado = :estado)
              and (:texto is null or lower(e.nombres) like :texto or lower(e.apellidos) like :texto or e.dni like :texto)
            order by e.apellidos, e.nombres
            """)
    List<Estudiante> listar(Long gradoSeccionId, String estado, String texto);

    @Query("select e from Estudiante e join fetch e.gradoSeccion left join fetch e.usuario where e.id = :id")
    Optional<Estudiante> buscar(Long id);

    Optional<Estudiante> findByDni(String dni);

    Optional<Estudiante> findByUsuarioId(Long usuarioId);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByGradoSeccionIdAndEstado(Long gradoSeccionId, String estado);

    /** [gradoSeccionId, cantidad] de estudiantes activos por aula. */
    @Query("select e.gradoSeccion.id, count(e) from Estudiante e where e.estado = 'A' group by e.gradoSeccion.id")
    List<Object[]> contarActivosPorAula();
}

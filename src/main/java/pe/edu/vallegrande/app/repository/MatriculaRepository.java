package pe.edu.vallegrande.app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    String FILTROS = """
            where m.anioAcademico = :anio and (:estado is null or m.estado = :estado)
              and (:nivel is null or lower(n.nombre) = lower(:nivel)) and (:grado is null or lower(g.codigo) = lower(:grado))
              and (:letra is null or lower(s.letra) = lower(:letra)) and (:seccionId is null or s.id = :seccionId)
              and (:texto is null or lower(concat(e.nombres, ' ', e.apellidos)) like :texto or e.dni like :texto)
            """;

    @Query(value = """
            select m from Matricula m join fetch m.estudiante e
            join fetch m.seccion s join fetch s.grado g join fetch g.nivel n
            """ + FILTROS + " order by n.orden, g.orden, s.letra, e.apellidos, e.nombres",
            countQuery = """
            select count(m) from Matricula m join m.estudiante e join m.seccion s join s.grado g join g.nivel n
            """ + FILTROS)
    Page<Matricula> buscar(int anio, EstadoMatricula estado, String nivel, String grado, String letra, Long seccionId,
                           String texto, Pageable pagina);

    @Query("""
            select m from Matricula m join fetch m.estudiante e
            join fetch m.seccion s join fetch s.grado g join fetch g.nivel n
            where m.seccion.id = :seccionId and m.anioAcademico = :anio and m.estado = :estado
            order by e.apellidos, e.nombres
            """)
    List<Matricula> listarPorSeccion(Long seccionId, int anio, EstadoMatricula estado);

    @Query("""
            select m from Matricula m join fetch m.estudiante e
            join fetch m.seccion s join fetch s.grado g join fetch g.nivel n
            where m.seccion.id in :seccionIds and m.anioAcademico = :anio and m.estado = :estado
            order by e.apellidos, e.nombres
            """)
    List<Matricula> listarPorSecciones(Collection<Long> seccionIds, int anio, EstadoMatricula estado);

    @Query("""
            select m from Matricula m join fetch m.estudiante e
            join fetch m.seccion s join fetch s.grado g join fetch g.nivel n
            where m.anioAcademico = :anio and m.estado = :estado
            """)
    List<Matricula> listarActivas(int anio, EstadoMatricula estado);

    @Query("""
            select m from Matricula m join fetch m.estudiante e
            join fetch m.seccion s join fetch s.grado g join fetch g.nivel n
            where e.id = :estudianteId and m.anioAcademico = :anio
            """)
    Optional<Matricula> buscarDelAnio(Long estudianteId, int anio);

    @Query("""
            select m from Matricula m join fetch m.estudiante e
            join fetch m.seccion s join fetch s.grado g join fetch g.nivel n
            where m.id = :id
            """)
    Optional<Matricula> buscarCompleta(Long id);

    @Query("""
            select n.nombre, count(m) from Matricula m join m.seccion s join s.grado g join g.nivel n
            where m.anioAcademico = :anio and m.estado = :estado group by n.nombre, n.orden order by n.orden
            """)
    List<Object[]> contarPorNivel(int anio, EstadoMatricula estado);

    @Query("""
            select m.seccion.id, count(m) from Matricula m
            where m.anioAcademico = :anio and m.estado = :estado group by m.seccion.id
            """)
    List<Object[]> contarPorSeccion(int anio, EstadoMatricula estado);

    List<Matricula> findByEstudianteId(Long estudianteId);

    boolean existsBySeccionId(Long seccionId);
}

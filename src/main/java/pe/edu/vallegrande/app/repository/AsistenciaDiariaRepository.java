package pe.edu.vallegrande.app.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.AsistenciaDiaria;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;

public interface AsistenciaDiariaRepository extends JpaRepository<AsistenciaDiaria, Long> {

    Optional<AsistenciaDiaria> findByEstudianteIdAndFecha(Long estudianteId, LocalDate fecha);

    @Query("select a from AsistenciaDiaria a where a.estudiante.id in :estudianteIds and a.fecha between :desde and :hasta")
    List<AsistenciaDiaria> listar(Collection<Long> estudianteIds, LocalDate desde, LocalDate hasta);

    List<AsistenciaDiaria> findByEstudianteIdAndFechaBetweenOrderByFechaDesc(Long estudianteId, LocalDate desde, LocalDate hasta);

    /** Cantidad de días con el estado dado por estudiante (solo matrículas activas del año). */
    @Query("""
            select a.estudiante.id, count(a) from AsistenciaDiaria a, Matricula m
            where m.estudiante = a.estudiante and m.anioAcademico = :anio and m.estado = :activa
              and a.estado = :estado and a.fecha between :desde and :hasta
            group by a.estudiante.id
            """)
    List<Object[]> contarPorEstudiante(EstadoAsistencia estado, LocalDate desde, LocalDate hasta, int anio, EstadoMatricula activa);

    @Query("""
            select a.fecha, count(a) from AsistenciaDiaria a, Matricula m
            where m.estudiante = a.estudiante and m.anioAcademico = :anio and m.estado = :activa
              and a.estado = :estado and a.fecha between :desde and :hasta
            group by a.fecha
            """)
    List<Object[]> contarPorFecha(EstadoAsistencia estado, LocalDate desde, LocalDate hasta, int anio, EstadoMatricula activa);

    /** Registros del día agrupados por nivel y estado (dashboard). */
    @Query("""
            select n.nombre, a.estado, count(a) from AsistenciaDiaria a, Matricula m
            join m.seccion s join s.grado g join g.nivel n
            where m.estudiante = a.estudiante and m.anioAcademico = :anio and m.estado = :activa and a.fecha = :fecha
            group by n.nombre, a.estado
            """)
    List<Object[]> contarDelDiaPorNivel(LocalDate fecha, int anio, EstadoMatricula activa);

    /** Registros [estudianteId, fecha, estado] de las matrículas activas (opcionalmente de una sección) en un rango. */
    @Query("""
            select a.estudiante.id, a.fecha, a.estado from AsistenciaDiaria a, Matricula m
            where m.estudiante = a.estudiante and m.anioAcademico = :anio and m.estado = :activa
              and (:seccionId is null or m.seccion.id = :seccionId) and a.fecha between :desde and :hasta
            """)
    List<Object[]> rango(LocalDate desde, LocalDate hasta, Long seccionId, int anio, EstadoMatricula activa);

    boolean existsByEstudianteId(Long estudianteId);

    void deleteByEstudianteId(Long estudianteId);
}

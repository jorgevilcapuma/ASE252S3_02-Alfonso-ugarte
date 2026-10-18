package pe.edu.vallegrande.app.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.AsistenciaPersonal;

public interface AsistenciaPersonalRepository extends JpaRepository<AsistenciaPersonal, Long> {

    Optional<AsistenciaPersonal> findByDocenteIdAndFecha(Long docenteId, LocalDate fecha);

    @Query("select a from AsistenciaPersonal a join fetch a.docente where a.fecha between :desde and :hasta")
    List<AsistenciaPersonal> listar(LocalDate desde, LocalDate hasta);
}

package pe.edu.vallegrande.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.vallegrande.app.model.FaseProyecto;

public interface FaseProyectoRepository extends JpaRepository<FaseProyecto, Long> {

    List<FaseProyecto> findAllByOrderByOrdenAsc();

    boolean existsByOrden(Integer orden);

    boolean existsByOrdenAndIdNot(Integer orden, Long id);
}

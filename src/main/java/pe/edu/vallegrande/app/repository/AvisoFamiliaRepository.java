package pe.edu.vallegrande.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.vallegrande.app.model.AvisoFamilia;

public interface AvisoFamiliaRepository extends JpaRepository<AvisoFamilia, Long> {

    /** Último aviso por estudiante: [estudianteId, fecha y hora]. */
    @Query("select a.estudiante.id, max(a.enviadoEn) from AvisoFamilia a group by a.estudiante.id")
    List<Object[]> ultimosAvisos();

    List<AvisoFamilia> findByEstudianteIdOrderByEnviadoEnDesc(Long estudianteId);

    void deleteByEstudianteId(Long estudianteId);
}

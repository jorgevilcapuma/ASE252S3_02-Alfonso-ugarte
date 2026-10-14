package pe.edu.vallegrande.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.vallegrande.app.model.Personal;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    List<Personal> findAllByOrderByApellidosAscNombresAsc();

    List<Personal> findByEstadoOrderByApellidosAscNombresAsc(String estado);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByCorreoIgnoreCase(String correo);
}

package pe.edu.vallegrande.app.dto.response;

import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Estudiante con su aula del año (equivale a la interfaz Estudiante del frontend). */
public record EstudianteResponse(
        Long id,
        String nombre,
        String nombres,
        String apellidos,
        String dni,
        String genero,
        String nivel,
        String grado,
        String gradoEtiqueta,
        String seccion,
        Long seccionId,
        Turno turno,
        Long matriculaId,
        EstadoMatricula estadoMatricula,
        String telefono,
        String correo) {

    public static EstudianteResponse de(Matricula matricula) {
        return de(matricula.getEstudiante(), matricula);
    }

    public static EstudianteResponse de(Estudiante e, Matricula m) {
        Seccion s = m == null ? null : m.getSeccion();
        return new EstudianteResponse(
                e.getId(), e.getNombreCompleto(), e.getNombres(), e.getApellidos(), e.getDni(), e.getGenero(),
                s == null ? null : s.getNivelNombre(),
                s == null ? null : s.getGrado().getCodigo(),
                s == null ? null : s.getGrado().getEtiqueta(),
                s == null ? null : s.getLetra(),
                s == null ? null : s.getId(),
                s == null ? null : s.getTurno(),
                m == null ? null : m.getId(),
                m == null ? null : m.getEstado(),
                e.getTelefono(), e.getCorreo());
    }
}

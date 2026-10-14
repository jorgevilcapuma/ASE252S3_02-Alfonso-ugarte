package pe.edu.vallegrande.app.dto.response;

/**
 * Alumno de un curso del docente con su porcentaje de asistencia (interfaz Alumno del frontend).
 * estadoGeneral: Normal (&gt;= 80 %), En Riesgo (&gt;= 68 %), Crítico.
 */
public record AlumnoCursoResponse(
        Long id,
        String dni,
        String nombres,
        String apellidos,
        String genero,
        String nivel,
        String gradoSeccion,
        Long cursoId,
        String curso,
        String cursoNombre,
        int sesiones,
        int porcentajeAsistencia,
        String estadoGeneral) {

    public static String estadoGeneral(int porcentaje) {
        return porcentaje >= 80 ? "Normal" : porcentaje >= 68 ? "En Riesgo" : "Crítico";
    }
}

package pe.edu.vallegrande.app.dto.request;

/** Nueva aula del estudiante: {@code seccionId} o nivel + grado + sección. */
public record TrasladoRequest(String nivel, String grado, String seccion, Long seccionId) {
}

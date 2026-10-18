package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.vallegrande.app.model.Docente;

/** Docente o miembro del personal; nombreCompleto en formato "Apellidos, Nombres". */
public record DocenteResponse(
        Long id,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String dni,
        String cargo,
        String especialidad,
        String area,
        String celular,
        String correo,
        String sexo,
        LocalDate fechaIngreso,
        String estado,
        Long usuarioId,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {

    public static DocenteResponse de(Docente d) {
        return new DocenteResponse(d.getId(), d.getNombres(), d.getApellidos(), d.getApellidos() + ", " + d.getNombres(), d.getDni(),
                d.getCargo(), d.getEspecialidad(), d.getArea(), d.getCelular(), d.getCorreo(), d.getSexo(), d.getFechaIngreso(),
                d.getEstado(), d.getUsuario() == null ? null : d.getUsuario().getId(), d.getCreadoEn(), d.getActualizadoEn());
    }
}

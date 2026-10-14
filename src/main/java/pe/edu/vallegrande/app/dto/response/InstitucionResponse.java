package pe.edu.vallegrande.app.dto.response;

import java.time.LocalDateTime;

import pe.edu.vallegrande.app.model.Institucion;

public record InstitucionResponse(
        Long id,
        String nombre,
        String codigoModular,
        String direccion,
        String colorPrincipal,
        String colorSecundario,
        String logo,
        LocalDateTime actualizadoEn) {

    public static InstitucionResponse de(Institucion i) {
        return new InstitucionResponse(i.getId(), i.getNombre(), i.getCodigoModular(), i.getDireccion(),
                i.getColorPrincipal(), i.getColorSecundario(), i.getLogo() == null ? "" : i.getLogo(), i.getActualizadoEn());
    }
}

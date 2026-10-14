package pe.edu.vallegrande.app.dto.response;

import pe.edu.vallegrande.app.model.FaseProyecto;
import pe.edu.vallegrande.app.model.enums.EstadoFase;

public record FaseResponse(Long id, Integer orden, String semana, String titulo, String actividad, String producto, EstadoFase estado) {

    public static FaseResponse de(FaseProyecto f) {
        return new FaseResponse(f.getId(), f.getOrden(), f.getSemana(), f.getTitulo(), f.getActividad(), f.getProducto(), f.getEstado());
    }
}

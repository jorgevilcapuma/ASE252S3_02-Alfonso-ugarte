package pe.edu.vallegrande.app.dto.response;

import java.util.List;

import pe.edu.vallegrande.app.model.Grado;
import pe.edu.vallegrande.app.model.NivelEducativo;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.enums.Turno;

/** Respuestas de las tablas maestras de estructura (niveles, grados, secciones). */
public final class EstructuraResponse {

    private EstructuraResponse() {
    }

    public record NivelResponse(Long id, String nombre, Integer orden, List<GradoResponse> grados) {

        public static NivelResponse de(NivelEducativo n) {
            return new NivelResponse(n.getId(), n.getNombre(), n.getOrden(), null);
        }
    }

    public record GradoResponse(Long id, Long nivelId, String nivel, String codigo, String etiqueta, Integer orden,
                                List<SeccionResponse> secciones) {

        public static GradoResponse de(Grado g) {
            return new GradoResponse(g.getId(), g.getNivel().getId(), g.getNivel().getNombre(), g.getCodigo(), g.getEtiqueta(), g.getOrden(), null);
        }
    }

    public record SeccionResponse(Long id, Long gradoId, String nivel, String grado, String gradoEtiqueta, String letra,
                                  Turno turno, Long totalEstudiantes) {

        public static SeccionResponse de(Seccion s, Long totalEstudiantes) {
            return new SeccionResponse(s.getId(), s.getGrado().getId(), s.getNivelNombre(), s.getGrado().getCodigo(),
                    s.getGrado().getEtiqueta(), s.getLetra(), s.getTurno(), totalEstudiantes);
        }
    }
}

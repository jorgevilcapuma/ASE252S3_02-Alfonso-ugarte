package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.AvisoFamiliaRequest;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AlertasResponse;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.AvisoResponse;
import pe.edu.vallegrande.app.dto.response.AlertaResponses.FaltasDia;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

/** Alertas tempranas: faltas acumuladas en la ventana de días hábiles y avisos a familias. */
public interface AlertaService {

    AlertasResponse alertas();

    /** Faltas sin justificar por día en los últimos {@code dias} días hábiles. */
    List<FaltasDia> tendencia(int dias);

    List<AvisoResponse> avisarFamilias(AvisoFamiliaRequest request, SesionUsuario sesion);

    List<AvisoResponse> avisos(Long estudianteId);
}

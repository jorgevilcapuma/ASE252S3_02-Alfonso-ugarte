package pe.edu.vallegrande.app.service;

import java.time.LocalDate;

import pe.edu.vallegrande.app.dto.request.MarcacionPersonalRequest;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.AsistenciaPersonalDiaResponse;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.FilaPersonal;
import pe.edu.vallegrande.app.dto.response.AsistenciaPersonalResponses.ReportePersonalResponse;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;

public interface AsistenciaPersonalService {

    AsistenciaPersonalDiaResponse delDia(LocalDate fecha);

    FilaPersonal marcarEntrada(Long docenteId, MarcacionPersonalRequest request, SesionUsuario sesion);

    FilaPersonal marcarSalida(Long docenteId, MarcacionPersonalRequest request, SesionUsuario sesion);

    void eliminar(Long docenteId, LocalDate fecha);

    ReportePersonalResponse reporte(String mes);
}

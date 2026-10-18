package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.GradoSeccionRequest;
import pe.edu.vallegrande.app.dto.response.GradoSeccionResponse;
import pe.edu.vallegrande.app.model.enums.Nivel;

public interface GradoSeccionService {

    List<GradoSeccionResponse> listar(Nivel nivel, String grado, String estado);

    GradoSeccionResponse obtener(Long id);

    GradoSeccionResponse crear(GradoSeccionRequest request);

    GradoSeccionResponse actualizar(Long id, GradoSeccionRequest request);

    GradoSeccionResponse eliminar(Long id);

    GradoSeccionResponse restaurar(Long id);
}

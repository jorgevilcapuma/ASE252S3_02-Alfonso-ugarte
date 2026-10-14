package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.dto.request.InstitucionRequest;
import pe.edu.vallegrande.app.dto.response.InstitucionResponse;

public interface InstitucionService {

    InstitucionResponse obtener();

    InstitucionResponse actualizar(InstitucionRequest request);

    InstitucionResponse restablecerIdentidad();
}

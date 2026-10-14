package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.FaseRequest;
import pe.edu.vallegrande.app.dto.response.FaseResponse;

public interface FaseService {

    List<FaseResponse> listar();

    FaseResponse obtener(Long id);

    FaseResponse crear(FaseRequest request);

    FaseResponse actualizar(Long id, FaseRequest request);

    void eliminar(Long id);
}

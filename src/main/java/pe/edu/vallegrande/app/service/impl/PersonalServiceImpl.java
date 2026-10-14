package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.PersonalRequest;
import pe.edu.vallegrande.app.dto.response.PersonalResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Personal;
import pe.edu.vallegrande.app.repository.PersonalRepository;
import pe.edu.vallegrande.app.service.PersonalService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class PersonalServiceImpl implements PersonalService {

    public static final String ACTIVO = "A";
    public static final String INACTIVO = "I";

    private final PersonalRepository repositorio;
    private final Calendario calendario;

    @Override
    @Transactional(readOnly = true)
    public List<PersonalResponse> listar() {
        return repositorio.findAllByOrderByApellidosAscNombresAsc().stream().map(PersonalResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalResponse> listarPorEstado(String estado) {
        String valor = estado == null ? "" : estado.trim().toUpperCase();
        return switch (valor) {
            case "T" -> listar();
            case ACTIVO, INACTIVO -> repositorio.findByEstadoOrderByApellidosAscNombresAsc(valor).stream().map(PersonalResponse::de).toList();
            default -> throw ApiException.validacion("El estado debe ser A (activos), I (inactivos) o T (todos).");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalResponse obtener(Long id) {
        return PersonalResponse.de(buscar(id));
    }

    @Override
    @Transactional
    public PersonalResponse crear(PersonalRequest request) {
        if (repositorio.existsByDni(request.dni())) {
            throw ApiException.conflicto("Ya existe personal registrado con el DNI " + request.dni() + ".");
        }
        String correo = Textos.correo(request.correo());
        if (correo != null && repositorio.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflicto("El correo " + correo + " ya está registrado.");
        }
        Personal personal = new Personal();
        aplicar(personal, request);
        personal.setEstado(ACTIVO);
        personal.setCreatedAt(calendario.ahora());
        return PersonalResponse.de(repositorio.save(personal));
    }

    @Override
    @Transactional
    public PersonalResponse editar(Long id, PersonalRequest request) {
        Personal personal = buscar(id);
        if (INACTIVO.equals(personal.getEstado())) {
            throw ApiException.conflicto("El personal está eliminado; restáuralo antes de editarlo.");
        }
        if (repositorio.existsByDniAndIdNot(request.dni(), id)) {
            throw ApiException.conflicto("Ya existe personal registrado con el DNI " + request.dni() + ".");
        }
        String correo = Textos.correo(request.correo());
        if (correo != null && repositorio.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw ApiException.conflicto("El correo " + correo + " ya está registrado.");
        }
        aplicar(personal, request);
        personal.setUpdatedAt(calendario.ahora());
        return PersonalResponse.de(personal);
    }

    @Override
    @Transactional
    public PersonalResponse eliminar(Long id) {
        Personal personal = buscar(id);
        if (INACTIVO.equals(personal.getEstado())) {
            throw ApiException.conflicto("El personal ya está eliminado.");
        }
        personal.setEstado(INACTIVO);
        personal.setDeletedAt(calendario.ahora());
        return PersonalResponse.de(personal);
    }

    @Override
    @Transactional
    public PersonalResponse restaurar(Long id) {
        Personal personal = buscar(id);
        if (ACTIVO.equals(personal.getEstado())) {
            throw ApiException.conflicto("El personal ya está activo.");
        }
        personal.setEstado(ACTIVO);
        personal.setRestoredAt(calendario.ahora());
        return PersonalResponse.de(personal);
    }

    private void aplicar(Personal personal, PersonalRequest request) {
        personal.setNombres(Textos.limpiar(request.nombres()));
        personal.setApellidos(Textos.limpiar(request.apellidos()));
        personal.setDni(request.dni());
        String cargo = request.cargo().trim().toLowerCase();
        personal.setCargo(Character.toUpperCase(cargo.charAt(0)) + cargo.substring(1));
        personal.setArea(Textos.limpiar(request.area()));
        personal.setCelular(Textos.limpiar(request.celular()));
        personal.setCorreo(Textos.correo(request.correo()));
        personal.setSexo(request.sexo().toUpperCase());
        personal.setFechaIngreso(request.fechaIngreso());
    }

    private Personal buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe personal con id " + id + "."));
    }
}

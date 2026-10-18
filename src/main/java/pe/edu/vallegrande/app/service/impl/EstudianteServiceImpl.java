package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.dto.request.ContactoRequest;
import pe.edu.vallegrande.app.dto.request.EstudianteRequest;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.service.EstudianteService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class EstudianteServiceImpl implements EstudianteService {

    private final EstudianteRepository estudiantes;
    private final Calendario calendario;
    private final Entidades entidades;
    private final CuentasAcceso cuentas;

    public EstudianteServiceImpl(EstudianteRepository estudiantes, Calendario calendario, Entidades entidades, CuentasAcceso cuentas) {
        this.estudiantes = estudiantes;
        this.cuentas = cuentas;
        this.calendario = calendario;
        this.entidades = entidades;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteResponse> listar(Long gradoSeccionId, String estado, String texto) {
        String valor = estado == null ? Estudiante.ACTIVO : estado.trim().toUpperCase();
        if (!List.of("A", "I", "T").contains(valor)) {
            throw ApiException.validacion("El estado debe ser A (activos), I (retirados) o T (todos).");
        }
        String filtro = Textos.limpiar(texto) == null ? null : "%" + Textos.limpiar(texto).toLowerCase() + "%";
        return estudiantes.listar(gradoSeccionId, "T".equals(valor) ? null : valor, filtro).stream()
                .map(EstudianteResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse obtener(Long id) {
        return EstudianteResponse.de(entidades.estudiante(id));
    }

    @Override
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        if (estudiantes.existsByDni(request.dni())) {
            throw ApiException.conflicto("Ya existe un estudiante con el DNI " + request.dni() + ".");
        }
        Estudiante estudiante = new Estudiante();
        aplicar(estudiante, request);
        estudiante.setEstado(Estudiante.ACTIVO);
        estudiante.setCreado(calendario.hoy());
        return EstudianteResponse.de(estudiantes.save(estudiante));
    }

    @Override
    @Transactional
    public EstudianteResponse actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = entidades.estudiante(id);
        if (estudiantes.existsByDniAndIdNot(request.dni(), id)) {
            throw ApiException.conflicto("Ya existe otro estudiante con el DNI " + request.dni() + ".");
        }
        aplicar(estudiante, request);
        return EstudianteResponse.de(estudiante);
    }

    @Override
    @Transactional
    public EstudianteResponse actualizarContacto(Long id, ContactoRequest request) {
        Estudiante estudiante = entidades.estudiante(id);
        estudiante.setTelefono(Textos.limpiar(request.telefono()));
        estudiante.setCorreo(Textos.correo(request.correo()));
        return EstudianteResponse.de(estudiante);
    }

    @Override
    @Transactional
    public EstudianteResponse eliminar(Long id) {
        Estudiante estudiante = entidades.estudiante(id);
        if (!estudiante.esActivo()) {
            throw ApiException.conflicto("El estudiante ya está retirado.");
        }
        estudiante.setEstado(Estudiante.INACTIVO);
        cuentas.activar(estudiante.getUsuario(), false);
        return EstudianteResponse.de(estudiante);
    }

    @Override
    @Transactional
    public EstudianteResponse restaurar(Long id) {
        Estudiante estudiante = entidades.estudiante(id);
        if (estudiante.esActivo()) {
            throw ApiException.conflicto("El estudiante ya está activo.");
        }
        estudiante.setEstado(Estudiante.ACTIVO);
        cuentas.activar(estudiante.getUsuario(), true);
        return EstudianteResponse.de(estudiante);
    }

    private void aplicar(Estudiante estudiante, EstudianteRequest request) {
        estudiante.setNombres(Textos.limpiar(request.nombres()));
        estudiante.setApellidos(Textos.limpiar(request.apellidos()));
        estudiante.setDni(request.dni());
        GradoSeccion aula = entidades.gradoSeccion(request.gradoSeccionId());
        if (!aula.esActivo()) {
            throw ApiException.conflicto("El aula " + aula.getNombre() + " está eliminada; restáurala o elige otra.");
        }
        estudiante.setGradoSeccion(aula);
        estudiante.setGenero(Textos.limpiar(request.genero()));
        estudiante.setTelefono(Textos.limpiar(request.telefono()));
        estudiante.setCorreo(Textos.correo(request.correo()));
        if (request.usuarioId() != null) {
            vincularCuenta(estudiante, request.usuarioId());
        }
    }

    /** La cuenta debe ser de perfil alumno y no estar vinculada a otro estudiante. */
    private void vincularCuenta(Estudiante estudiante, Long usuarioId) {
        Usuario usuario = entidades.usuario(usuarioId);
        if (usuario.getPerfil() != Perfil.ALUMNO) {
            throw ApiException.validacion("La cuenta vinculada debe tener perfil alumno.");
        }
        estudiantes.findByUsuarioId(usuarioId).filter(otro -> !otro.getId().equals(estudiante.getId())).ifPresent(otro -> {
            throw ApiException.conflicto("Esa cuenta ya está vinculada al estudiante " + otro.getNombreCompleto() + ".");
        });
        estudiante.setUsuario(usuario);
    }
}

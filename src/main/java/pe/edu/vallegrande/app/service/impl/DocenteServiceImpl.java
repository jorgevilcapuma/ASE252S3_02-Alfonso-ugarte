package pe.edu.vallegrande.app.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.dto.request.DocenteRequest;
import pe.edu.vallegrande.app.dto.response.DocenteResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.repository.CursoRepository;
import pe.edu.vallegrande.app.repository.DocenteRepository;
import pe.edu.vallegrande.app.service.DocenteService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.FiltroEstado;
import pe.edu.vallegrande.app.util.Textos;

@Service
public class DocenteServiceImpl implements DocenteService {

    private final DocenteRepository docentes;
    private final CursoRepository cursos;
    private final Calendario calendario;
    private final Entidades entidades;
    private final CuentasAcceso cuentas;

    public DocenteServiceImpl(DocenteRepository docentes, CursoRepository cursos, Calendario calendario, Entidades entidades,
                              CuentasAcceso cuentas) {
        this.docentes = docentes;
        this.cursos = cursos;
        this.calendario = calendario;
        this.entidades = entidades;
        this.cuentas = cuentas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocenteResponse> listar(String estado, String cargo) {
        String valor = estado == null ? "T" : estado.trim().toUpperCase();
        if (!List.of("A", "I", "T").contains(valor)) {
            throw ApiException.validacion("El estado debe ser A (activos), I (inactivos) o T (todos).");
        }
        return docentes.listar("T".equals(valor) ? null : valor, cargo == null ? null : cargo(cargo)).stream()
                .map(DocenteResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocenteResponse obtener(Long id) {
        return DocenteResponse.de(entidades.docente(id));
    }

    @Override
    @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        if (docentes.existsByDni(request.dni())) {
            throw ApiException.conflicto("Ya existe personal registrado con el DNI " + request.dni() + ".");
        }
        String correo = Textos.correo(request.correo());
        if (correo != null && docentes.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflicto("El correo " + correo + " ya está registrado.");
        }
        Docente docente = new Docente();
        aplicar(docente, request);
        docente.setEstado(Docente.ACTIVO);
        docente.setCreadoEn(calendario.ahora());
        return DocenteResponse.de(docentes.save(docente));
    }

    @Override
    @Transactional
    public DocenteResponse editar(Long id, DocenteRequest request) {
        Docente docente = entidades.docente(id);
        if (!docente.esActivo()) {
            throw ApiException.conflicto("El registro está eliminado; restáuralo antes de editarlo.");
        }
        if (docentes.existsByDniAndIdNot(request.dni(), id)) {
            throw ApiException.conflicto("Ya existe personal registrado con el DNI " + request.dni() + ".");
        }
        String correo = Textos.correo(request.correo());
        if (correo != null && docentes.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw ApiException.conflicto("El correo " + correo + " ya está registrado.");
        }
        if (docente.esDocente() && !Docente.CARGO_DOCENTE.equals(cargo(request.cargo())) && cursos.existsByDocenteIdAndEstado(id, FiltroEstado.ACTIVO)) {
            throw ApiException.conflicto("No se puede cambiar el cargo porque tiene cursos asignados. Reasigna sus cursos primero.");
        }
        aplicar(docente, request);
        docente.setActualizadoEn(calendario.ahora());
        return DocenteResponse.de(docente);
    }

    @Override
    @Transactional
    public DocenteResponse eliminar(Long id) {
        Docente docente = entidades.docente(id);
        if (!docente.esActivo()) {
            throw ApiException.conflicto("El registro ya está eliminado.");
        }
        if (cursos.existsByDocenteIdAndEstado(id, FiltroEstado.ACTIVO)) {
            throw ApiException.conflicto("No se puede eliminar porque tiene cursos asignados. Reasigna sus cursos primero.");
        }
        docente.setEstado(Docente.INACTIVO);
        docente.setActualizadoEn(calendario.ahora());
        cuentas.activar(docente.getUsuario(), false);
        return DocenteResponse.de(docente);
    }

    @Override
    @Transactional
    public DocenteResponse restaurar(Long id) {
        Docente docente = entidades.docente(id);
        if (docente.esActivo()) {
            throw ApiException.conflicto("El registro ya está activo.");
        }
        docente.setEstado(Docente.ACTIVO);
        docente.setActualizadoEn(calendario.ahora());
        cuentas.activar(docente.getUsuario(), true);
        return DocenteResponse.de(docente);
    }

    private void aplicar(Docente docente, DocenteRequest request) {
        docente.setNombres(Textos.limpiar(request.nombres()));
        docente.setApellidos(Textos.limpiar(request.apellidos()));
        docente.setDni(request.dni());
        docente.setCargo(cargo(request.cargo()));
        docente.setEspecialidad(Textos.limpiar(request.especialidad()));
        docente.setArea(Textos.limpiar(request.area()));
        docente.setCelular(Textos.limpiar(request.celular()));
        docente.setCorreo(Textos.correo(request.correo()));
        docente.setSexo(request.sexo().toUpperCase());
        docente.setFechaIngreso(request.fechaIngreso());
        if (request.usuarioId() != null) {
            // Vincular una cuenta existente: su correo es el de ingreso y pasa a la ficha.
            vincularCuenta(docente, request.usuarioId());
            docente.setCorreo(docente.getUsuario().getCorreo());
        }
        sincronizarCuenta(docente);
    }

    /**
     * El correo de la ficha es el de su cuenta: con cargo Docente y correo, la cuenta (perfil docente, contraseña inicial)
     * se crea o se vincula en el mismo registro. Si ya tiene cuenta, se le actualizan nombre y correo; sin correo en la
     * ficha se conserva el de la cuenta. Otros cargos solo mantienen al día la cuenta que ya tuvieran.
     */
    private void sincronizarCuenta(Docente docente) {
        if (docente.getUsuario() == null && !docente.esDocente()) {
            return;
        }
        Usuario cuenta = cuentas.sincronizar(docente.getUsuario(), docente.getNombres() + " " + docente.getApellidos(), docente.getCorreo(),
                Perfil.DOCENTE, Set.of(Perfil.DOCENTE, Perfil.ADMINISTRADOR),
                usuarioId -> docentes.findByUsuarioId(usuarioId).filter(otro -> !Objects.equals(otro.getId(), docente.getId())).isPresent());
        docente.setUsuario(cuenta);
        if (cuenta != null) {
            docente.setCorreo(cuenta.getCorreo());
        }
    }

    /** Vínculo explícito (API): la cuenta debe ser de perfil docente o administrador y no estar vinculada a otra persona. */
    private void vincularCuenta(Docente docente, Long usuarioId) {
        Usuario usuario = entidades.usuario(usuarioId);
        if (usuario.getPerfil() == Perfil.ALUMNO) {
            throw ApiException.validacion("La cuenta vinculada debe tener perfil docente o administrador.");
        }
        docentes.findByUsuarioId(usuario.getId()).filter(otro -> !otro.getId().equals(docente.getId())).ifPresent(otro -> {
            throw ApiException.conflicto("Esa cuenta ya está vinculada a otra persona.");
        });
        docente.setUsuario(usuario);
    }

    /** "ADMINISTRATIVO" → "Administrativo". */
    private static String cargo(String texto) {
        String cargo = texto.trim().toLowerCase();
        return Character.toUpperCase(cargo.charAt(0)) + cargo.substring(1);
    }
}

package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.ComunicadoRequest;
import pe.edu.vallegrande.app.dto.response.ComunicadoResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.Comunicado;
import pe.edu.vallegrande.app.repository.ComunicadoRepository;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.service.ComunicadoService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class ComunicadoServiceImpl implements ComunicadoService {

    private final ComunicadoRepository comunicados;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listar(Long cursoId, SesionUsuario sesion) {
        List<Comunicado> lista;
        if (sesion.esAlumno()) {
            Long seccionId = entidades.matriculaActiva(sesion.estudianteId()).getSeccion().getId();
            lista = comunicados.visiblesParaSeccion(seccionId).stream()
                    .filter(c -> cursoId == null || (c.getAsignacion() != null && c.getAsignacion().getId().equals(cursoId)))
                    .toList();
        } else if (sesion.esDocente()) {
            lista = comunicados.buscar(cursoId, sesion.docenteFiltro(), sesion.usuarioId());
        } else {
            lista = comunicados.buscar(cursoId, null, null);
        }
        return lista.stream().map(ComunicadoResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComunicadoResponse obtener(Long id) {
        return ComunicadoResponse.de(comunicado(id));
    }

    @Override
    @Transactional
    public ComunicadoResponse crear(ComunicadoRequest request, SesionUsuario sesion) {
        Comunicado c = new Comunicado();
        c.setAutor(entidades.usuarioOpcional(sesion.usuarioId()));
        c.setFechaCreacion(calendario.hoy());
        aplicar(c, request, sesion);
        return ComunicadoResponse.de(comunicados.save(c));
    }

    @Override
    @Transactional
    public ComunicadoResponse actualizar(Long id, ComunicadoRequest request, SesionUsuario sesion) {
        Comunicado c = comunicado(id);
        verificarAutor(c, sesion);
        aplicar(c, request, sesion);
        return ComunicadoResponse.de(c);
    }

    @Override
    @Transactional
    public void eliminar(Long id, SesionUsuario sesion) {
        Comunicado c = comunicado(id);
        verificarAutor(c, sesion);
        comunicados.delete(c);
    }

    private void aplicar(Comunicado c, ComunicadoRequest request, SesionUsuario sesion) {
        Asignacion asignacion = request.cursoId() == null ? null : entidades.asignacion(request.cursoId());
        if (asignacion != null && sesion.esDocente() && !asignacion.getDocente().getId().equals(sesion.docenteId())) {
            throw ApiException.prohibido("Solo puedes publicar comunicados en tus cursos.");
        }
        c.setTitulo(Textos.limpiar(request.titulo()));
        c.setContenido(request.contenido().trim());
        c.setPrioridad(request.prioridad());
        c.setAsignacion(asignacion);
    }

    private static void verificarAutor(Comunicado c, SesionUsuario sesion) {
        if (sesion.esAdministrador()) {
            return;
        }
        if (c.getAutor() == null || !c.getAutor().getId().equals(sesion.usuarioId())) {
            throw ApiException.prohibido("Solo el autor o un administrador puede modificar este comunicado.");
        }
    }

    private Comunicado comunicado(Long id) {
        return comunicados.buscarCompleto(id).orElseThrow(() -> ApiException.noEncontrado("No existe el comunicado con id " + id + "."));
    }
}

package pe.edu.vallegrande.app.service.impl;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.MatriculaRequest;
import pe.edu.vallegrande.app.dto.request.TrasladoRequest;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.dto.response.MensajeResponse;
import pe.edu.vallegrande.app.dto.response.PaginaResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.repository.AsistenciaCursoRepository;
import pe.edu.vallegrande.app.repository.AsistenciaDiariaRepository;
import pe.edu.vallegrande.app.repository.AvisoFamiliaRepository;
import pe.edu.vallegrande.app.repository.EstudianteRepository;
import pe.edu.vallegrande.app.repository.JustificacionRepository;
import pe.edu.vallegrande.app.repository.MatriculaRepository;
import pe.edu.vallegrande.app.repository.SeccionRepository;
import pe.edu.vallegrande.app.service.MatriculaService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class MatriculaServiceImpl implements MatriculaService {

    private static final int TAMANIO_MAXIMO = 1000;

    private final MatriculaRepository matriculas;
    private final EstudianteRepository estudiantes;
    private final SeccionRepository secciones;
    private final AsistenciaDiariaRepository asistenciaDiaria;
    private final AsistenciaCursoRepository asistenciaCurso;
    private final JustificacionRepository justificaciones;
    private final AvisoFamiliaRepository avisos;
    private final Calendario calendario;
    private final Entidades entidades;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<EstudianteResponse> listar(String nivel, String grado, String seccion, Long seccionId, String texto,
                                                     EstadoMatricula estado, int pagina, int tamanio) {
        if (pagina < 1 || tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw ApiException.validacion("La página empieza en 1 y el tamaño debe estar entre 1 y " + TAMANIO_MAXIMO + ".");
        }
        String filtro = Textos.limpiar(texto) == null ? null : "%" + Textos.limpiar(texto).toLowerCase() + "%";
        return PaginaResponse.de(matriculas.buscar(calendario.anioActual(), estado, Textos.limpiar(nivel), Textos.limpiar(grado),
                Textos.limpiar(seccion), seccionId, filtro, PageRequest.of(pagina - 1, tamanio)), EstudianteResponse::de);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NivelResumen> resumen() {
        return matriculas.contarPorNivel(calendario.anioActual(), EstadoMatricula.ACTIVA).stream()
                .map(fila -> new NivelResumen((String) fila[0], (Long) fila[1]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse obtener(Long matriculaId) {
        return EstudianteResponse.de(matricula(matriculaId));
    }

    @Override
    @Transactional
    public EstudianteResponse matricular(MatriculaRequest request) {
        String[] nombre = nombreYApellidos(request);
        Seccion seccion = resolverSeccion(request.seccionId(), request.nivel(), request.grado(), request.seccion());

        Estudiante estudiante = estudiantes.findByDni(request.dni()).orElse(null);
        if (estudiante == null) {
            estudiante = new Estudiante();
            estudiante.setDni(request.dni());
            estudiante.setNombres(nombre[0]);
            estudiante.setApellidos(nombre[1]);
            estudiante.setGenero(Textos.limpiar(request.genero()));
            estudiante.setTelefono(Textos.limpiar(request.telefono()));
            estudiante.setCorreo(Textos.correo(request.correo()));
            estudiante.setCreado(calendario.hoy());
            estudiantes.save(estudiante);
        } else {
            if (!Textos.clave(estudiante.getNombreCompleto()).equals(Textos.clave(nombre[0] + " " + nombre[1]))) {
                throw ApiException.conflicto("Ese DNI ya está registrado con otro nombre (" + estudiante.getNombreCompleto() + "). Verifica los datos.");
            }
            Matricula existente = matriculas.buscarDelAnio(estudiante.getId(), calendario.anioActual()).orElse(null);
            if (existente != null) {
                throw ApiException.conflicto(existente.getEstado() == EstadoMatricula.ACTIVA
                        ? "Ya existe un estudiante matriculado con ese DNI."
                        : "El estudiante tiene una matrícula retirada este año; reincorpóralo en lugar de matricularlo de nuevo.");
            }
        }

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioAcademico(calendario.anioActual());
        matricula.setFechaMatricula(calendario.hoy());
        matricula.setEstado(EstadoMatricula.ACTIVA);
        return EstudianteResponse.de(matriculas.save(matricula));
    }

    @Override
    @Transactional
    public EstudianteResponse trasladar(Long matriculaId, TrasladoRequest request) {
        Matricula matricula = matricula(matriculaId);
        if (matricula.getEstado() != EstadoMatricula.ACTIVA) {
            throw ApiException.conflicto("No se puede trasladar una matrícula retirada.");
        }
        Seccion destino = resolverSeccion(request.seccionId(), request.nivel(), request.grado(), request.seccion());
        if (destino.getId().equals(matricula.getSeccion().getId())) {
            throw ApiException.validacion("El estudiante ya pertenece a esa sección.");
        }
        matricula.setSeccion(destino);
        return EstudianteResponse.de(matricula);
    }

    @Override
    @Transactional
    public EstudianteResponse retirar(Long matriculaId) {
        Matricula matricula = matricula(matriculaId);
        if (matricula.getEstado() == EstadoMatricula.RETIRADA) {
            throw ApiException.conflicto("La matrícula ya está retirada.");
        }
        matricula.setEstado(EstadoMatricula.RETIRADA);
        matricula.setFechaRetiro(calendario.hoy());
        return EstudianteResponse.de(matricula);
    }

    @Override
    @Transactional
    public EstudianteResponse reincorporar(Long matriculaId) {
        Matricula matricula = matricula(matriculaId);
        if (matricula.getEstado() == EstadoMatricula.ACTIVA) {
            throw ApiException.conflicto("La matrícula ya está activa.");
        }
        matricula.setEstado(EstadoMatricula.ACTIVA);
        matricula.setFechaRetiro(null);
        return EstudianteResponse.de(matricula);
    }

    @Override
    @Transactional
    public MensajeResponse anular(Long matriculaId) {
        Matricula matricula = matricula(matriculaId);
        Estudiante estudiante = matricula.getEstudiante();
        Long id = estudiante.getId();
        boolean tieneHistorial = asistenciaDiaria.existsByEstudianteId(id) || asistenciaCurso.existsByEstudianteId(id)
                || justificaciones.existsByEstudianteId(id);
        if (tieneHistorial) {
            if (matricula.getEstado() == EstadoMatricula.ACTIVA) {
                matricula.setEstado(EstadoMatricula.RETIRADA);
                matricula.setFechaRetiro(calendario.hoy());
            }
            return new MensajeResponse("El estudiante tiene asistencia registrada: la matrícula se marcó como retirada para conservar su historial.");
        }
        matriculas.delete(matricula);
        if (matriculas.findByEstudianteId(id).stream().allMatch(m -> m.getId().equals(matricula.getId()))) {
            avisos.deleteByEstudianteId(id);
            estudiantes.delete(estudiante);
        }
        return new MensajeResponse("Matrícula anulada.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteResponse> estudiantesDeSeccion(Long seccionId) {
        entidades.seccion(seccionId);
        return matriculas.listarPorSeccion(seccionId, calendario.anioActual(), EstadoMatricula.ACTIVA).stream()
                .map(EstudianteResponse::de).toList();
    }

    /** Sección por id o por nivel + grado + letra, como la envía el formulario del frontend. */
    private Seccion resolverSeccion(Long seccionId, String nivel, String grado, String letra) {
        if (seccionId != null) {
            return entidades.seccion(seccionId);
        }
        if (Textos.limpiar(nivel) == null || Textos.limpiar(grado) == null || Textos.limpiar(letra) == null) {
            throw ApiException.validacion("Selecciona el nivel, el grado y la sección.");
        }
        return secciones.buscar(nivel.trim(), grado.trim(), letra.trim())
                .orElseThrow(() -> ApiException.validacion("Selecciona un grado y una sección válidos para el nivel " + nivel.trim() + "."));
    }

    private static String[] nombreYApellidos(MatriculaRequest request) {
        String nombres = Textos.limpiar(request.nombres());
        String apellidos = Textos.limpiar(request.apellidos());
        if (nombres != null && apellidos != null) {
            return new String[]{nombres, apellidos};
        }
        String[] partes = Textos.separarNombreCompleto(request.nombre());
        if (partes == null) {
            throw ApiException.validacion("Ingresa nombres y apellidos.");
        }
        return partes;
    }

    private Matricula matricula(Long id) {
        return matriculas.buscarCompleta(id).orElseThrow(() -> ApiException.noEncontrado("No existe la matrícula con id " + id + "."));
    }
}

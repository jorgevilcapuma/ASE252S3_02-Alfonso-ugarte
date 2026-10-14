package pe.edu.vallegrande.app.service;

import java.util.List;

import pe.edu.vallegrande.app.dto.request.MatriculaRequest;
import pe.edu.vallegrande.app.dto.request.TrasladoRequest;
import pe.edu.vallegrande.app.dto.response.EstudianteResponse;
import pe.edu.vallegrande.app.dto.response.MensajeResponse;
import pe.edu.vallegrande.app.dto.response.PaginaResponse;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;

/** Matrículas del año: alta, traslado de aula, retiro y anulación. */
public interface MatriculaService {

    PaginaResponse<EstudianteResponse> listar(String nivel, String grado, String seccion, Long seccionId, String texto,
                                              EstadoMatricula estado, int pagina, int tamanio);

    /** Estudiantes matriculados por nivel (tarjetas del módulo Matrículas). */
    List<NivelResumen> resumen();

    EstudianteResponse obtener(Long matriculaId);

    EstudianteResponse matricular(MatriculaRequest request);

    EstudianteResponse trasladar(Long matriculaId, TrasladoRequest request);

    EstudianteResponse retirar(Long matriculaId);

    EstudianteResponse reincorporar(Long matriculaId);

    /** Elimina la matrícula si no tiene historial; si lo tiene, la retira para conservarlo. */
    MensajeResponse anular(Long matriculaId);

    List<EstudianteResponse> estudiantesDeSeccion(Long seccionId);

    record NivelResumen(String nivel, long total) {
    }
}

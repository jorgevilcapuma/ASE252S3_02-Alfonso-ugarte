package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Carga de demostración con historial (5 días hábiles): asistencia diaria, marcaciones del personal y una justificación pendiente. */
@TestPropertySource(properties = "app.datos-demo.dias-historial=5")
class DatosDemoTest extends PruebaApi {

    @Test
    void generaHistorialYJustificacionPendiente() throws Exception {
        String admin = admin();
        long juan = juanPerez();
        // Días hábiles anteriores al martes 13: 6, 7, 8, 9 y 12 de octubre; el último falta.
        get_("/api/estudiantes/" + juan + "/asistencia", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].fecha").value(LUNES.toString()))
                .andExpect(jsonPath("$[0].estado").value("falta"));

        get_("/api/justificaciones?estado=Pendiente", admin)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].motivo").value("Cita médica comunicada por la familia."))
                .andExpect(jsonPath("$[0].estadoPrevio").value("falta"));

        long aula = gradoSeccionId("Primaria", "1", "A", admin);
        get_("/api/asistencia-estudiantes/grados-secciones/" + aula + "?fecha=" + LUNES, admin)
                .andExpect(jsonPath("$.resumen.pendiente").value(0));
        get_("/api/asistencia-personal/reporte?mes=2026-10", admin)
                .andExpect(jsonPath("$.filas.length()").value(13))
                .andExpect(jsonPath("$.filas[0].diasAsistidos", greaterThan(0)));
    }
}

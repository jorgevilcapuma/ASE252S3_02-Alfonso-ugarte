package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;

/** Carga de demostración con historial (5 días hábiles): sesiones por curso, justificaciones y alertas coherentes. */
@TestPropertySource(properties = "app.datos-demo.dias-historial=5")
class DatosDemoTest extends PruebaApi {

    @Test
    void generaHistorialSesionesYJustificacionesPendientes() throws Exception {
        String admin = admin();
        JsonNode luis = ingresar("docente", LUIS, CLAVE).get("sesion");
        // MAT-1A se dicta lunes, martes, jueves y viernes: 6, 8, 9 y 12 de octubre.
        get_("/api/docentes/" + luis.get("docenteId").asLong() + "/historial", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].fecha").value(LUNES.toString()))
                .andExpect(jsonPath("$[0].totalAlumnos").value(13));

        get_("/api/justificaciones?estado=Pendiente", admin)
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[*].motivo", hasItem("Cita médica comunicada por la familia.")));

        long juan = ingresarAlumno("Juan Pérez Ugarte", "72819201").get("sesion").get("estudianteId").asLong();
        get_("/api/estudiantes/" + juan + "/asistencia", admin)
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].fecha").value(LUNES.toString()))
                .andExpect(jsonPath("$[0].estado").value("falta"));

        long aula = seccionId("Primaria", "1", "A", admin);
        get_("/api/asistencia/secciones/" + aula + "?fecha=" + LUNES, admin)
                .andExpect(jsonPath("$.resumen.pendiente").value(0));
        get_("/api/dashboard", admin).andExpect(jsonPath("$.tendencia[5].faltas", greaterThan(0)))
                .andExpect(jsonPath("$.justificacionesPendientes", greaterThan(0)));
    }
}

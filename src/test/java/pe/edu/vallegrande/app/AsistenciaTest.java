package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Matriz de asistencia diaria por aula (P/T/F/J), vista mensual y dashboard. */
class AsistenciaTest extends PruebaApi {

    private static Map<String, Object> registro(JsonNode estudiante, String estado) {
        return Map.of("estudianteId", estudiante.get("id").asLong(), "estado", estado);
    }

    @Test
    void registraElDiaDelAulaYActualizaSinDuplicar() throws Exception {
        String docente = docente(ELENA);
        long aula = seccionId("Primaria", "1", "A", docente);
        JsonNode alumnos = estudiantesDe(aula, docente);

        get_("/api/asistencia/secciones/" + aula, docente).andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(HOY.toString()))
                .andExpect(jsonPath("$.aula.gradoEtiqueta").value("1ro grado"))
                .andExpect(jsonPath("$.registros.length()").value(13))
                .andExpect(jsonPath("$.resumen.pendiente").value(13));

        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", HOY.toString(), "registros", List.of(
                registro(alumnos.get(0), "presente"), registro(alumnos.get(1), "tardanza"), registro(alumnos.get(2), "falta"))), docente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.presente").value(1))
                .andExpect(jsonPath("$.resumen.tardanza").value(1))
                .andExpect(jsonPath("$.resumen.falta").value(1))
                .andExpect(jsonPath("$.resumen.pendiente").value(10));

        // Volver a guardar el mismo día actualiza; "pendiente" borra el registro.
        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", HOY.toString(), "registros", List.of(
                registro(alumnos.get(2), "justificado"), registro(alumnos.get(0), "pendiente"))), docente)
                .andExpect(jsonPath("$.resumen.presente").value(0))
                .andExpect(jsonPath("$.resumen.justificado").value(1))
                .andExpect(jsonPath("$.resumen.falta").value(0))
                .andExpect(jsonPath("$.resumen.pendiente").value(11));
    }

    @Test
    void rechazaFechasFuturasFinesDeSemanaYAlumnosDeOtraAula() throws Exception {
        String admin = admin();
        long aula = seccionId("Primaria", "1", "A", admin);
        JsonNode alumno = estudiantesDe(aula, admin).get(0);
        JsonNode otro = estudiantesDe(seccionId("Primaria", "1", "B", admin), admin).get(0);

        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", HOY.plusDays(1).toString(), "registros", List.of(registro(alumno, "presente"))), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("No se puede registrar asistencia de una fecha futura."));
        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", SABADO.toString(), "registros", List.of(registro(alumno, "presente"))), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("No se registra asistencia en sábado ni domingo."));
        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", HOY.toString(), "registros", List.of(registro(otro, "presente"))), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje", containsString("no está matriculado en esta sección")));
        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", HOY.toString(), "registros", List.of(registro(alumno, "presente"), registro(alumno, "falta"))), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje", containsString("aparece más de una vez")));
        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", HOY.toString(), "registros", List.of(registro(alumno, "ausente"))), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Valor no válido para estado de asistencia: 'ausente'. Valores permitidos: presente, tardanza, falta, justificado, pendiente."));
        put_("/api/asistencia/secciones/" + aula, Map.of("registros", List.of()), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.length()").value(2));
    }

    @Test
    void vistaMensualConFinesDeSemanaYResumenHastaHoy() throws Exception {
        String admin = admin();
        long aula = seccionId("Primaria", "1", "A", admin);
        JsonNode alumno = estudiantesDe(aula, admin).get(0);
        long id = alumno.get("id").asLong();
        put_("/api/asistencia/estudiantes/" + id + "/" + LocalDate.of(2026, 10, 1), Map.of("estado", "falta"), admin).andExpect(status().isOk());
        put_("/api/asistencia/estudiantes/" + id + "/" + HOY, Map.of("estado", "presente"), admin).andExpect(status().isOk());

        get_("/api/asistencia/secciones/" + aula + "/mensual?mes=2026-10", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-10"))
                .andExpect(jsonPath("$.dias.length()").value(31))
                .andExpect(jsonPath("$.finesDeSemana.length()").value(9))
                .andExpect(jsonPath("$.filas.length()").value(13))
                .andExpect(jsonPath("$.filas[0].estados['2026-10-01']").value("falta"))
                .andExpect(jsonPath("$.filas[0].estados['2026-10-13']").value("presente"))
                .andExpect(jsonPath("$.filas[0].estados['2026-10-30']").value("pendiente"))
                .andExpect(jsonPath("$.filas[0].estados['2026-10-03']").doesNotExist())
                .andExpect(jsonPath("$.filas[0].resumen.falta").value(1))
                .andExpect(jsonPath("$.filas[0].resumen.presente").value(1))
                // Días hábiles del 1 al 13 de octubre: 9, de los cuales 7 sin registrar.
                .andExpect(jsonPath("$.filas[0].resumen.pendiente").value(7));
        get_("/api/asistencia/secciones/" + aula + "/mensual?mes=octubre", admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El mes debe tener el formato AAAA-MM."));
    }

    @Test
    void elDashboardReflejaElRegistroDelDia() throws Exception {
        String admin = admin();
        long aula = seccionId("Primaria", "1", "A", admin);
        JsonNode alumnos = estudiantesDe(aula, admin);
        put_("/api/asistencia/secciones/" + aula, Map.of("fecha", HOY.toString(), "registros", List.of(
                registro(alumnos.get(0), "presente"), registro(alumnos.get(1), "falta"), registro(alumnos.get(2), "tardanza"))), admin);

        get_("/api/dashboard", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(HOY.toString()))
                .andExpect(jsonPath("$.totalEstudiantes").value(505))
                .andExpect(jsonPath("$.faltasHoy").value(1))
                .andExpect(jsonPath("$.tardanzasHoy").value(1))
                .andExpect(jsonPath("$.presentesHoy").value(1))
                .andExpect(jsonPath("$.porNivel[1].nivel").value("Primaria"))
                .andExpect(jsonPath("$.porNivel[1].registrados").value(3))
                .andExpect(jsonPath("$.porcentajeRegistro").value(1))
                .andExpect(jsonPath("$.tendencia.length()").value(7))
                .andExpect(jsonPath("$.tendencia[6].fecha").value(HOY.toString()))
                .andExpect(jsonPath("$.tendencia[6].faltas").value(1));
    }

    @Test
    void consultaPorRangoParaTodasLasAulasOUnaSeccion() throws Exception {
        String admin = admin();
        long aula = seccionId("Primaria", "1", "A", admin);
        JsonNode alumnos = estudiantesDe(aula, admin);
        long otro = estudiantesDe(seccionId("Secundaria", "5", "B", admin), admin).get(0).get("id").asLong();
        put_("/api/asistencia/estudiantes/" + alumnos.get(0).get("id").asLong() + "/" + VIERNES, Map.of("estado", "falta"), admin);
        put_("/api/asistencia/estudiantes/" + alumnos.get(1).get("id").asLong() + "/" + HOY, Map.of("estado", "presente"), admin);
        put_("/api/asistencia/estudiantes/" + otro + "/" + HOY, Map.of("estado", "tardanza"), admin);

        get_("/api/asistencia?desde=" + VIERNES + "&hasta=" + HOY, docente(ELENA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        get_("/api/asistencia?desde=" + VIERNES + "&hasta=" + HOY + "&seccionId=" + aula, admin)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.fecha == '" + VIERNES + "')].estado").value("falta"));
        get_("/api/asistencia?desde=2026-01-01&hasta=" + HOY, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Consulta como máximo 93 días de asistencia a la vez."));
        get_("/api/asistencia?desde=" + HOY, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Falta el parámetro 'hasta'."));
    }

    @Test
    void elGeneroSeRegistraYSeExpone() throws Exception {
        String admin = admin();
        post_("/api/matriculas", Map.of("nombre", "Rita Paz Soto", "dni", "76543299", "genero", "F", "nivel", "Primaria", "grado", "1", "seccion", "B"), admin)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.genero").value("F"));
        post_("/api/matriculas", Map.of("nombre", "Rita Paz Soto", "dni", "76543298", "genero", "X", "nivel", "Primaria", "grado", "1", "seccion", "B"), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0]").value("El género debe ser M o F."));
        long luis = ingresar("docente", LUIS, CLAVE).get("sesion").get("docenteId").asLong();
        get_("/api/docentes/" + luis + "/estudiantes", admin)
                .andExpect(jsonPath("$[?(@.nombres == 'Lucía')].genero").value("F"))
                .andExpect(jsonPath("$[0].nivel").value("Primaria"))
                .andExpect(jsonPath("$[0].gradoSeccion").value("1ro grado A"))
                .andExpect(jsonPath("$[0].curso").value("Matemática"));
    }

    @Test
    void elAlumnoNoAccedeALaMatriz() throws Exception {
        String alumno = ingresarAlumno("Juan Pérez Ugarte", "72819201").get("token").asText();
        get_("/api/asistencia/secciones/1", alumno).andExpect(status().isForbidden());
    }
}

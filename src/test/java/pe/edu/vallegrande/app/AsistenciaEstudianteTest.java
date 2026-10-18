package pe.edu.vallegrande.app;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Asistencia de estudiantes: diaria del aula (sin curso) y por curso. El reloj de las pruebas marca el martes 13/10/2026. */
class AsistenciaEstudianteTest extends PruebaApi {

    /** Todos presentes salvo el primero (falta) y el segundo (tardanza). */
    private Map<String, Object> lote(JsonNode alumnos, LocalDate fecha, Long cursoId) {
        List<Map<String, Object>> registros = new ArrayList<>();
        for (int i = 0; i < alumnos.size(); i++) {
            registros.add(Map.of("estudianteId", alumnos.get(i).get("id").asLong(), "estado", i == 0 ? "falta" : i == 1 ? "tardanza" : "presente"));
        }
        Map<String, Object> cuerpo = new HashMap<>(Map.of("fecha", fecha.toString(), "registros", registros));
        if (cursoId != null) {
            cuerpo.put("cursoId", cursoId);
        }
        return cuerpo;
    }

    @Test
    void registraLaAsistenciaDiariaDelAula() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "1", "A", admin);
        JsonNode alumnos = estudiantesDe(aula, admin);

        get_("/api/asistencia-estudiantes/grados-secciones/" + aula, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(HOY.toString()))
                .andExpect(jsonPath("$.aula.gradoEtiqueta").value("1ro grado"))
                .andExpect(jsonPath("$.resumen.pendiente").value(13));

        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, HOY, null), docente(ELENA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cursoId").doesNotExist())
                .andExpect(jsonPath("$.resumen.presente").value(11))
                .andExpect(jsonPath("$.resumen.falta").value(1))
                .andExpect(jsonPath("$.resumen.tardanza").value(1))
                .andExpect(jsonPath("$.resumen.pendiente").value(0));

        // "pendiente" borra el registro.
        long primero = alumnos.get(0).get("id").asLong();
        put_("/api/asistencia-estudiantes/estudiantes/" + primero, Map.of("fecha", HOY.toString(), "estado", "pendiente"), admin)
                .andExpect(status().isOk());
        get_("/api/asistencia-estudiantes/grados-secciones/" + aula + "?fecha=" + HOY, admin)
                .andExpect(jsonPath("$.resumen.pendiente").value(1))
                .andExpect(jsonPath("$.resumen.falta").value(0));
    }

    @Test
    void laAsistenciaPorCursoEsIndependienteDeLaDiaria() throws Exception {
        String luis = docente(LUIS);
        long aula = gradoSeccionId("Primaria", "1", "A", luis);
        long mat = cursoId("Matemática", aula, luis);
        JsonNode alumnos = estudiantesDe(aula, luis);

        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, LUNES, mat), luis).andExpect(status().isOk())
                .andExpect(jsonPath("$.cursoNombre").value("Matemática"))
                .andExpect(jsonPath("$.resumen.falta").value(1));
        get_("/api/asistencia-estudiantes/grados-secciones/" + aula + "?fecha=" + LUNES, luis)
                .andExpect(jsonPath("$.resumen.pendiente").value(13));

        long primero = alumnos.get(0).get("id").asLong();
        get_("/api/estudiantes/" + primero + "/asistencia", luis).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cursoId").value(mat))
                .andExpect(jsonPath("$[0].estado").value("falta"));

        // Solo el docente del curso registra su asistencia; la diaria la puede registrar cualquier docente.
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, LUNES, mat), docente(ELENA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el docente del curso puede registrar su asistencia."));
    }

    @Test
    void vistaMensualDelAula() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "1", "A", admin);
        JsonNode alumnos = estudiantesDe(aula, admin);
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, LUNES, null), admin).andExpect(status().isOk());
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, HOY, null), admin).andExpect(status().isOk());

        // Octubre 2026: 22 días hábiles, 9 hasta el martes 13 (7 de ellos sin registrar).
        get_("/api/asistencia-estudiantes/grados-secciones/" + aula + "/mensual?mes=2026-10", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-10"))
                .andExpect(jsonPath("$.dias.length()").value(31))
                .andExpect(jsonPath("$.finesDeSemana.length()").value(9))
                .andExpect(jsonPath("$.filas.length()").value(13))
                .andExpect(jsonPath("$.filas[0].estados['2026-10-12']").value("falta"))
                .andExpect(jsonPath("$.filas[0].resumen.falta").value(2))
                .andExpect(jsonPath("$.filas[0].resumen.pendiente").value(7));
    }

    @Test
    void validaFechasAulaYPlazoDelDocente() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "1", "A", admin);
        JsonNode alumnos = estudiantesDe(aula, admin);
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, HOY.plusDays(1), null), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede registrar asistencia de una fecha futura."));
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, SABADO, null), admin)
                .andExpect(status().isBadRequest());

        long otraAula = gradoSeccionId("Primaria", "1", "B", admin);
        put_("/api/asistencia-estudiantes/grados-secciones/" + otraAula, lote(alumnos, HOY, null), admin)
                .andExpect(status().isBadRequest());
        long matSecundaria = cursoId("Matemática", gradoSeccionId("Secundaria", "3", "A", admin), admin);
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, HOY, matSecundaria), admin)
                .andExpect(status().isBadRequest());

        // Plazo de edición del docente: hoy y 5 días hábiles atrás (desde el martes 6 de octubre).
        LocalDate cerrada = LocalDate.of(2026, 10, 5);
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, cerrada, null), docente(ELENA))
                .andExpect(status().isForbidden());
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, LocalDate.of(2026, 10, 6), null), docente(ELENA))
                .andExpect(status().isOk());
        put_("/api/asistencia-estudiantes/grados-secciones/" + aula, lote(alumnos, cerrada, null), admin).andExpect(status().isOk());

        String alumno = ingresarAlumno("Juan Pérez Ugarte", "72819201").get("token").asText();
        get_("/api/asistencia-estudiantes/grados-secciones/" + aula, alumno).andExpect(status().isForbidden());
    }
}

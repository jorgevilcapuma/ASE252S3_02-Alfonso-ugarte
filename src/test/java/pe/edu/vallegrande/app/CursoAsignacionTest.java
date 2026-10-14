package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Catálogo de cursos y cursos por sección (docente, aula, horario). */
class CursoAsignacionTest extends PruebaApi {

    private long docenteId(String nombreCompleto, String admin) throws Exception {
        for (JsonNode d : leer(get_("/api/docentes", admin))) {
            if (d.get("nombreCompleto").asText().equals(nombreCompleto)) {
                return d.get("id").asLong();
            }
        }
        throw new IllegalStateException(nombreCompleto);
    }

    private long cursoId(String nombre, String admin) throws Exception {
        for (JsonNode c : leer(get_("/api/cursos", admin))) {
            if (c.get("nombre").asText().equals(nombre)) {
                return c.get("id").asLong();
            }
        }
        throw new IllegalStateException(nombre);
    }

    private Map<String, Object> asignacion(String codigo, long curso, long seccion, long docente, List<Map<String, String>> horarios) {
        return Map.of("codigo", codigo, "cursoId", curso, "seccionId", seccion, "docenteId", docente, "aula", "Aula 10", "horarios", horarios);
    }

    private static Map<String, String> bloque(String dia, String inicio, String fin) {
        return Map.of("dia", dia, "inicio", inicio, "fin", fin);
    }

    @Test
    void crudDelCatalogoDeCursos() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/cursos", Map.of("nombre", "Robótica", "descripcion", "Taller"), admin)
                .andExpect(status().isCreated())).get("id").asLong();
        post_("/api/cursos", Map.of("nombre", "ROBÓTICA"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El curso ROBÓTICA ya está registrado."));
        put_("/api/cursos/" + id, Map.of("nombre", "Robótica Educativa"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Robótica Educativa"));
        delete_("/api/cursos/" + id, admin).andExpect(status().isNoContent());
        delete_("/api/cursos/" + cursoId("Matemática", admin), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("está asignado a secciones")));
    }

    @Test
    void creaCursoDeSeccionConHorarioEnTexto() throws Exception {
        String admin = admin();
        long curso = leer(post_("/api/cursos", Map.of("nombre", "Robótica"), admin)).get("id").asLong();
        post_("/api/asignaciones", asignacion("rob-1a", curso, seccionId("Secundaria", "1", "A", admin), docenteId("Mario Torres", admin),
                List.of(bloque("Lunes", "08:00", "09:00"), bloque("Miércoles", "08:00", "09:00"))), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("ROB-1A"))
                .andExpect(jsonPath("$.nombre").value("Robótica"))
                .andExpect(jsonPath("$.gradoEtiqueta").value("1ro año"))
                .andExpect(jsonPath("$.docente").value("Prof. Mario Torres"))
                .andExpect(jsonPath("$.horario").value("Lun - Mié 08:00 - 09:00"))
                .andExpect(jsonPath("$.horarios.length()").value(2))
                .andExpect(jsonPath("$.totalAlumnos").value(12));
    }

    @Test
    void rechazaCrucesDeHorarioEnLaSeccionYDelDocente() throws Exception {
        String admin = admin();
        long curso = leer(post_("/api/cursos", Map.of("nombre", "Robótica"), admin)).get("id").asLong();
        long primeroA = seccionId("Primaria", "1", "A", admin);
        // COM-1A ocupa 1° Primaria A el lunes de 08:00 a 09:15.
        post_("/api/asignaciones", asignacion("ROB-1A", curso, primeroA, docenteId("Mario Torres", admin),
                List.of(bloque("Lunes", "08:30", "09:30"))), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El horario del Lunes se cruza con COM-1A (Comunicación) de la sección."));
        // Luis Mendoza dicta MAT-1A el lunes de 09:15 a 10:30.
        post_("/api/asignaciones", asignacion("ROB-2A", curso, seccionId("Secundaria", "2", "A", admin), docenteId("Luis Mendoza Castro", admin),
                List.of(bloque("Lunes", "10:00", "11:00"))), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("del docente")));
        post_("/api/asignaciones", asignacion("ROB-2A", curso, seccionId("Secundaria", "2", "A", admin), docenteId("Mario Torres", admin),
                List.of(bloque("Lunes", "11:00", "10:00"))), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("En Lunes la hora de fin debe ser posterior a la de inicio."));
        post_("/api/asignaciones", asignacion("ROB-2A", curso, seccionId("Secundaria", "2", "A", admin), docenteId("Mario Torres", admin),
                List.of(bloque("Domingo", "08:00", "09:00"))), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("Valores permitidos: Lunes, Martes, Miércoles, Jueves, Viernes")));
    }

    @Test
    void evitaCodigosYCursosDuplicadosEnLaSeccion() throws Exception {
        String admin = admin();
        long mario = docenteId("Mario Torres", admin);
        long primeroA = seccionId("Primaria", "1", "A", admin);
        post_("/api/asignaciones", asignacion("MAT-1A", cursoId("Inglés", admin), seccionId("Primaria", "1", "B", admin),
                mario, List.of()), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El código MAT-1A ya está en uso."));
        post_("/api/asignaciones", asignacion("MAT-1A-BIS", cursoId("Matemática", admin), primeroA, mario, List.of()), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ese curso ya está asignado a la sección (código MAT-1A)."));
    }

    @Test
    void actualizarConLosMismosBloquesNoChocaConSuPropioHorario() throws Exception {
        String admin = admin();
        long id = asignacionId("MAT-1A", admin);
        JsonNode actual = leer(get_("/api/asignaciones/" + id, admin));
        Map<String, Object> cuerpo = asignacion("MAT-1A", actual.get("cursoId").asLong(), actual.get("seccionId").asLong(),
                actual.get("docenteId").asLong(), List.of(bloque("Lunes", "09:15", "10:30"), bloque("Martes", "11:00", "12:30"),
                        bloque("Jueves", "08:00", "09:15"), bloque("Viernes", "11:00", "12:30")));
        put_("/api/asignaciones/" + id, cuerpo, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.aula").value("Aula 10"))
                .andExpect(jsonPath("$.horarios.length()").value(4));
    }

    @Test
    void noSeEliminaUnCursoConAsistenciaRegistrada() throws Exception {
        String luis = docente(LUIS);
        long mat = asignacionId("MAT-1A", luis);
        JsonNode filas = leer(get_("/api/asignaciones/" + mat + "/sesiones/" + LUNES, luis)).get("filas");
        put_("/api/asignaciones/" + mat + "/sesiones/" + LUNES,
                Map.of("registros", List.of(Map.of("estudianteId", filas.get(0).get("alumnoId").asLong(), "estado", "asistio"))), luis)
                .andExpect(status().isOk());
        delete_("/api/asignaciones/" + mat, admin()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar el curso porque ya tiene asistencia registrada."));
    }

    @Test
    void elDocenteSoloVeSusCursos() throws Exception {
        get_("/api/asignaciones", docente(ELENA)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", containsInAnyOrder("COM-4B", "COM-1A", "TUT-1A")));
        get_("/api/asignaciones", admin()).andExpect(jsonPath("$.length()").value(20));
    }
}

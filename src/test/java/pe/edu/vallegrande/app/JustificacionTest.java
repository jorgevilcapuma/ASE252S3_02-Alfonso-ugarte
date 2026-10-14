package pe.edu.vallegrande.app;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Flujo de justificaciones: solicitud, revisión y efecto sobre la asistencia del día y del curso. */
class JustificacionTest extends PruebaApi {

    private long juanId;
    private String alumno;

    private void ingresarComoJuan() throws Exception {
        JsonNode sesion = ingresarAlumno("Juan Pérez Ugarte", "72819201");
        alumno = sesion.get("token").asText();
        juanId = sesion.get("sesion").get("estudianteId").asLong();
    }

    private String estadoDelDia(long estudiante, LocalDate fecha, String token) throws Exception {
        for (JsonNode r : leer(get_("/api/estudiantes/" + estudiante + "/asistencia?desde=" + fecha + "&hasta=" + fecha, token))) {
            return r.get("estado").asText();
        }
        return "pendiente";
    }

    private long solicitar(LocalDate fecha, Long cursoId) throws Exception {
        Map<String, Object> cuerpo = new HashMap<>(Map.of("fecha", fecha.toString(), "motivo", "Cita médica", "detalle", "Constancia adjunta",
                "archivoNombre", "constancia.pdf"));
        if (cursoId != null) {
            cuerpo.put("cursoId", cursoId);
        }
        return leer(post_("/api/justificaciones", cuerpo, alumno).andExpect(status().isCreated())).get("id").asLong();
    }

    @Test
    void aprobarJustificaElDiaYNoPermiteRevisarDosVeces() throws Exception {
        ingresarComoJuan();
        String admin = admin();
        put_("/api/asistencia/estudiantes/" + juanId + "/" + LUNES, Map.of("estado", "falta"), admin).andExpect(status().isOk());

        long id = leer(post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", "  Cita   médica ", "archivoNombre", "constancia.pdf"), alumno)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estudianteId").value(juanId))
                .andExpect(jsonPath("$.estudianteNombre").value("Juan Pérez Ugarte"))
                .andExpect(jsonPath("$.motivo").value("Cita médica"))
                .andExpect(jsonPath("$.estado").value("Pendiente"))
                .andExpect(jsonPath("$.estadoPrevio").value("falta"))).get("id").asLong();

        post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", "Otra vez"), alumno).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una solicitud pendiente para esa fecha."));

        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Aprobada", "observacion", "Constancia recibida"), docente(ELENA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Aprobada"))
                .andExpect(jsonPath("$.observacion").value("Constancia recibida"))
                .andExpect(jsonPath("$.revisadoPor").value("Elena Salazar Ríos"));
        org.assertj.core.api.Assertions.assertThat(estadoDelDia(juanId, LUNES, admin)).isEqualTo("justificado");

        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Observada"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La justificación ya fue aprobada; no se puede revisar."));
        post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", "Otra vez"), alumno).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Esa inasistencia ya fue justificada."));
        delete_("/api/justificaciones/" + id, alumno).andExpect(status().isConflict());
    }

    @Test
    void observarRestauraElEstadoPrevioOMarcaFalta() throws Exception {
        ingresarComoJuan();
        String admin = admin();
        put_("/api/asistencia/estudiantes/" + juanId + "/" + VIERNES, Map.of("estado", "tardanza"), admin);
        long conPrevio = solicitar(VIERNES, null);
        patch_("/api/justificaciones/" + conPrevio + "/revision", Map.of("estado", "Observada", "observacion", "Falta el sustento"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Observada"));
        org.assertj.core.api.Assertions.assertThat(estadoDelDia(juanId, VIERNES, admin)).isEqualTo("tardanza");

        LocalDate sinRegistro = LocalDate.of(2026, 10, 7);
        long sinPrevio = solicitar(sinRegistro, null);
        patch_("/api/justificaciones/" + sinPrevio + "/revision", Map.of("estado", "Observada"), admin).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(estadoDelDia(juanId, sinRegistro, admin)).isEqualTo("falta");

        // Un día en que estuvo presente no cambia al aprobar.
        LocalDate presente = LocalDate.of(2026, 10, 6);
        put_("/api/asistencia/estudiantes/" + juanId + "/" + presente, Map.of("estado", "presente"), admin);
        long aprobada = solicitar(presente, null);
        patch_("/api/justificaciones/" + aprobada + "/revision", Map.of("estado", "Aprobada"), admin).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(estadoDelDia(juanId, presente, admin)).isEqualTo("presente");
    }

    @Test
    void justificacionDeUnCursoLaRevisaSuDocenteYActualizaLaSesion() throws Exception {
        ingresarComoJuan();
        String luis = docente(LUIS);
        long mat = asignacionId("MAT-1A", luis);
        JsonNode filas = leer(get_("/api/asignaciones/" + mat + "/sesiones/" + LUNES, luis)).get("filas");
        List<Map<String, Object>> registros = new java.util.ArrayList<>();
        filas.forEach(f -> registros.add(Map.of("estudianteId", f.get("alumnoId").asLong(),
                "estado", f.get("alumnoId").asLong() == juanId ? "falta" : "asistio")));
        put_("/api/asignaciones/" + mat + "/sesiones/" + LUNES, Map.of("registros", registros), luis).andExpect(status().isOk());

        long id = solicitar(LUNES, mat);
        get_("/api/justificaciones/" + id, alumno).andExpect(jsonPath("$.cursoNombre").value("Matemática"));
        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Aprobada"), docente(ELENA)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el docente del curso puede revisar esta justificación."));
        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Aprobada"), luis).andExpect(status().isOk());

        JsonNode sesion = leer(get_("/api/asignaciones/" + mat + "/sesiones/" + LUNES, luis));
        sesion.get("filas").forEach(f -> {
            if (f.get("alumnoId").asLong() == juanId) {
                org.assertj.core.api.Assertions.assertThat(f.get("estado").asText()).isEqualTo("justificado");
            }
        });
        get_("/api/justificaciones?docenteId=" + ingresar("docente", LUIS, CLAVE).get("sesion").get("docenteId").asLong(), luis)
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void validacionesDeLaSolicitud() throws Exception {
        ingresarComoJuan();
        post_("/api/justificaciones", Map.of("fecha", HOY.plusDays(1).toString(), "motivo", "x"), alumno).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede justificar una fecha futura."));
        post_("/api/justificaciones", Map.of("fecha", SABADO.toString(), "motivo", "x"), alumno).andExpect(status().isBadRequest());
        post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", " "), alumno).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Describe el motivo de la inasistencia."));
        long cursoDeOtraSeccion = asignacionId("MAT-3A", admin());
        post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", "x", "cursoId", cursoDeOtraSeccion), alumno)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El curso indicado no corresponde a la sección del estudiante."));
        post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", "x"), admin()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Selecciona el estudiante."));
        long id = solicitar(LUNES, null);
        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Pendiente"), admin()).andExpect(status().isBadRequest());
        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Aprobada"), alumno).andExpect(status().isForbidden());
    }

    @Test
    void elAlumnoSoloVeYEditaLasSuyas() throws Exception {
        ingresarComoJuan();
        String admin = admin();
        long otroAlumno = estudiantesDe(seccionId("Primaria", "1", "A", admin), admin).get(0).get("id").asLong();
        long ajena = leer(post_("/api/justificaciones", Map.of("estudianteId", otroAlumno, "fecha", LUNES.toString(), "motivo", "Viaje"), admin)
                .andExpect(status().isCreated())).get("id").asLong();
        long propia = solicitar(VIERNES, null);

        get_("/api/justificaciones", alumno).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(propia));
        get_("/api/justificaciones?estudianteId=" + otroAlumno, alumno).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propia));
        get_("/api/justificaciones/" + ajena, alumno).andExpect(status().isForbidden());
        put_("/api/justificaciones/" + ajena, Map.of("fecha", LUNES.toString(), "motivo", "y"), alumno).andExpect(status().isForbidden());
        put_("/api/justificaciones/" + propia, Map.of("fecha", LUNES.toString(), "motivo", "Cambio de fecha"), alumno).andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(LUNES.toString()));
        get_("/api/justificaciones?estado=Pendiente", admin).andExpect(jsonPath("$.length()").value(2));
        delete_("/api/justificaciones/" + propia, alumno).andExpect(status().isNoContent());
        get_("/api/justificaciones", alumno).andExpect(jsonPath("$.length()").value(0));
    }
}

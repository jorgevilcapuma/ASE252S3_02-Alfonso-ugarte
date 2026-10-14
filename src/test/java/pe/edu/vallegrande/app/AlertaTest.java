package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Alertas tempranas: 3+ faltas en 20 días hábiles = crítica, 2 = preventiva, falta hoy = hoy. */
class AlertaTest extends PruebaApi {

    private void falta(long estudiante, LocalDate fecha, String token) throws Exception {
        put_("/api/asistencia/estudiantes/" + estudiante + "/" + fecha, Map.of("estado", "falta"), token).andExpect(status().isOk());
    }

    @Test
    void clasificaPorFaltasEnLaVentanaYRegistraAvisos() throws Exception {
        String admin = admin();
        JsonNode alumnos = estudiantesDe(seccionId("Secundaria", "2", "B", admin), admin);
        long critico = alumnos.get(0).get("id").asLong();
        long preventivo = alumnos.get(1).get("id").asLong();
        long hoy = alumnos.get(2).get("id").asLong();
        for (LocalDate f : List.of(LocalDate.of(2026, 10, 8), VIERNES, LUNES)) {
            falta(critico, f, admin);
        }
        falta(critico, LocalDate.of(2026, 9, 1), admin);   // fuera de la ventana de 20 días hábiles
        falta(preventivo, VIERNES, admin);
        falta(preventivo, LUNES, admin);
        falta(hoy, HOY, admin);

        get_("/api/alertas", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.parametros.ventanaDias").value(20))
                .andExpect(jsonPath("$.parametros.desde").value("2026-09-16"))
                .andExpect(jsonPath("$.criticas").value(1))
                .andExpect(jsonPath("$.preventivas").value(1))
                .andExpect(jsonPath("$.faltasHoy").value(1))
                .andExpect(jsonPath("$.alertas.length()").value(3))
                .andExpect(jsonPath("$.alertas[0].estudiante.id").value(critico))
                .andExpect(jsonPath("$.alertas[0].faltas").value(3))
                .andExpect(jsonPath("$.alertas[0].nivel").value("critica"))
                .andExpect(jsonPath("$.alertas[1].nivel").value("preventiva"))
                .andExpect(jsonPath("$.alertas[2].nivel").value("hoy"))
                .andExpect(jsonPath("$.alertas[2].faltaHoy").value(true))
                .andExpect(jsonPath("$.alertas[0].ultimoAviso").doesNotExist());

        post_("/api/alertas/avisos", Map.of("estudianteIds", List.of(critico, critico)), docente(ELENA)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].faltas").value(3))
                .andExpect(jsonPath("$[0].enviadoPor").value("Elena Salazar Ríos"));
        get_("/api/alertas", admin).andExpect(jsonPath("$.alertas[0].ultimoAviso", notNullValue()));
        get_("/api/estudiantes/" + critico + "/avisos", admin).andExpect(jsonPath("$.length()").value(1));

        get_("/api/alertas/tendencia?dias=7", admin)
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[6].fecha").value(HOY.toString())).andExpect(jsonPath("$[6].faltas").value(1))
                .andExpect(jsonPath("$[5].fecha").value(LUNES.toString())).andExpect(jsonPath("$[5].faltas").value(2));
        get_("/api/alertas/tendencia?dias=0", admin).andExpect(status().isBadRequest());
        post_("/api/alertas/avisos", Map.of("estudianteIds", List.of()), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Selecciona al menos un estudiante para avisar a su familia."));
    }

    @Test
    void unEstudianteRetiradoDejaDeAparecerEnLasAlertas() throws Exception {
        String admin = admin();
        JsonNode alumno = estudiantesDe(seccionId("Secundaria", "2", "B", admin), admin).get(0);
        falta(alumno.get("id").asLong(), HOY, admin);
        get_("/api/alertas", admin).andExpect(jsonPath("$.alertas.length()").value(1));
        patch_("/api/matriculas/" + alumno.get("matriculaId").asLong() + "/retiro", null, admin).andExpect(status().isOk());
        get_("/api/alertas", admin).andExpect(jsonPath("$.alertas.length()").value(0));
    }
}

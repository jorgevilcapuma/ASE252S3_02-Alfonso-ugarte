package pe.edu.vallegrande.app;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Entrada y salida del personal: puntual o tardanza según la hora de entrada (07:45) y la tolerancia (10 min).
 * El reloj de las pruebas marca las 10:00 del martes 13/10/2026.
 */
class AsistenciaPersonalTest extends PruebaApi {

    private static Map<String, Object> marca(String fecha, String hora) {
        Map<String, Object> m = new HashMap<>();
        if (fecha != null) {
            m.put("fecha", fecha);
        }
        if (hora != null) {
            m.put("hora", hora);
        }
        return m;
    }

    private long personal(int indice, String token) throws Exception {
        return leer(get_("/api/docentes?estado=A", token)).get(indice).get("id").asLong();
    }

    @Test
    void marcaEntradaYSalidaYClasificaLaPuntualidad() throws Exception {
        String admin = admin();
        long puntual = personal(0, admin);
        long tarde = personal(1, admin);

        get_("/api/asistencia-personal", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(HOY.toString()))
                .andExpect(jsonPath("$.limitePuntualidad").value("07:55"))
                .andExpect(jsonPath("$.resumen.total").value(13))
                .andExpect(jsonPath("$.resumen.sinMarcar").value(13))
                .andExpect(jsonPath("$.filas[0].estado").value("sin-marcar"));

        put_("/api/asistencia-personal/" + puntual + "/entrada", marca(null, "07:55"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("puntual"))
                .andExpect(jsonPath("$.minutosTarde").value(0));
        put_("/api/asistencia-personal/" + tarde + "/entrada", marca(null, "08:07"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("tardanza"))
                .andExpect(jsonPath("$.minutosTarde").value(22));
        // Sin hora se marca la hora actual del reloj.
        put_("/api/asistencia-personal/" + puntual + "/salida", marca(null, null), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.horaSalida").value("10:00"));

        get_("/api/asistencia-personal?fecha=" + HOY, admin)
                .andExpect(jsonPath("$.resumen.puntuales").value(1))
                .andExpect(jsonPath("$.resumen.tardanzas").value(1))
                .andExpect(jsonPath("$.resumen.sinMarcar").value(11))
                .andExpect(jsonPath("$.resumen.conSalida").value(1));

        // Corregir la entrada recalcula el estado.
        put_("/api/asistencia-personal/" + tarde + "/entrada", marca(null, "07:40"), admin).andExpect(jsonPath("$.estado").value("puntual"));
    }

    @Test
    void validaHorasFechasYPermisos() throws Exception {
        String admin = admin();
        long id = personal(0, admin);
        put_("/api/asistencia-personal/" + id + "/salida", marca(null, "09:00"), admin).andExpect(status().isConflict());
        put_("/api/asistencia-personal/" + id + "/entrada", marca(null, "11:00"), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede marcar una hora futura."));
        put_("/api/asistencia-personal/" + id + "/entrada", marca(LUNES.toString(), null), admin).andExpect(status().isBadRequest());
        put_("/api/asistencia-personal/" + id + "/entrada", marca(SABADO.toString(), "07:30"), admin).andExpect(status().isBadRequest());
        put_("/api/asistencia-personal/" + id + "/entrada", marca(null, "07:30"), admin).andExpect(status().isOk());
        put_("/api/asistencia-personal/" + id + "/salida", marca(null, "07:30"), admin).andExpect(status().isBadRequest());
        put_("/api/asistencia-personal/" + id + "/entrada", marca(null, "07:30"), docente(ELENA)).andExpect(status().isForbidden());

        delete_("/api/asistencia-personal/" + id + "/" + HOY, admin).andExpect(status().isNoContent());
        delete_("/api/asistencia-personal/" + id + "/" + HOY, admin).andExpect(status().isNotFound());
    }

    @Test
    void incluyeAlPersonalAdministrativoYExcluyeAlInactivo() throws Exception {
        String admin = admin();
        long secretario = leer(get_("/api/docentes?cargo=Administrativo", admin)).get(0).get("id").asLong();
        put_("/api/asistencia-personal/" + secretario + "/entrada", marca(null, "07:50"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("Administrativo"))
                .andExpect(jsonPath("$.estado").value("puntual"));

        patch_("/api/docentes/" + secretario + "/eliminar", null, admin).andExpect(status().isOk());
        get_("/api/asistencia-personal", admin).andExpect(jsonPath("$.resumen.total").value(12));
        put_("/api/asistencia-personal/" + secretario + "/salida", marca(null, "09:00"), admin).andExpect(status().isConflict());
    }

    @Test
    void reporteMensualPorPersona() throws Exception {
        String admin = admin();
        long id = personal(0, admin);
        put_("/api/asistencia-personal/" + id + "/entrada", marca(LUNES.toString(), "08:15"), admin).andExpect(status().isOk());
        put_("/api/asistencia-personal/" + id + "/entrada", marca(HOY.toString(), "07:45"), admin).andExpect(status().isOk());

        // Octubre 2026 hasta el martes 13: 9 días hábiles.
        get_("/api/asistencia-personal/reporte?mes=2026-10", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.diasHabiles").value(9))
                .andExpect(jsonPath("$.filas[0].diasAsistidos").value(2))
                .andExpect(jsonPath("$.filas[0].tardanzas").value(1))
                .andExpect(jsonPath("$.filas[0].minutosTardeAcumulados").value(30))
                .andExpect(jsonPath("$.filas[0].diasSinMarcar").value(7))
                .andExpect(jsonPath("$.filas[0].porcentajeAsistencia").value(22));
    }
}

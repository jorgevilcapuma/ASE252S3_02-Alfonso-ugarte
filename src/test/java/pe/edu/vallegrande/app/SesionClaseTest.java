package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Asistencia por curso del docente: sesiones, historial, reportes, "Mis estudiantes" y alertas por curso. */
class SesionClaseTest extends PruebaApi {

    /** Todos "asistio" salvo los indicados (índice de fila → estado). */
    private List<Map<String, Object>> registros(JsonNode filas, Map<Integer, String> excepciones) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            Map<String, Object> r = new HashMap<>();
            r.put("estudianteId", filas.get(i).get("alumnoId").asLong());
            r.put("estado", excepciones.getOrDefault(i, "asistio"));
            r.put("observacion", i == 0 ? "Llegó con justificación verbal" : "");
            lista.add(r);
        }
        return lista;
    }

    @Test
    void tomaYActualizaLaAsistenciaDelCurso() throws Exception {
        String luis = docente(LUIS);
        long mat = asignacionId("MAT-1A", luis);
        String url = "/api/asignaciones/" + mat + "/sesiones/" + HOY;
        JsonNode lista = leer(get_(url, luis).andExpect(status().isOk())
                .andExpect(jsonPath("$.yaRegistrado").value(false))
                .andExpect(jsonPath("$.cursoNombre").value("Matemática"))
                .andExpect(jsonPath("$.gradoSeccion").value("1ro grado A"))
                .andExpect(jsonPath("$.filas.length()").value(13))
                .andExpect(jsonPath("$.filas[0].estado").value("asistio")));

        put_(url, Map.of("registros", registros(lista.get("filas"), Map.of(0, "falta", 1, "tardanza"))), luis)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaRegistrado").value(true))
                .andExpect(jsonPath("$.resumen.asistieron").value(11))
                .andExpect(jsonPath("$.resumen.tardanzas").value(1))
                .andExpect(jsonPath("$.resumen.faltas").value(1))
                .andExpect(jsonPath("$.filas[0].observacion").value("Llegó con justificación verbal"));

        put_(url, Map.of("registros", registros(lista.get("filas"), Map.of(1, "falta"))), luis)
                .andExpect(jsonPath("$.resumen.faltas").value(1)).andExpect(jsonPath("$.filas[0].estado").value("asistio"));
        get_(url, luis).andExpect(jsonPath("$.yaRegistrado").value(true)).andExpect(jsonPath("$.filas[1].estado").value("falta"));

        delete_(url, luis).andExpect(status().isNoContent());
        delete_(url, luis).andExpect(status().isNotFound());
    }

    @Test
    void validaFechaAlumnosYPropiedadDelCurso() throws Exception {
        String luis = docente(LUIS);
        long mat = asignacionId("MAT-1A", luis);
        JsonNode filas = leer(get_("/api/asignaciones/" + mat + "/sesiones/" + HOY, luis)).get("filas");
        Map<String, Object> cuerpo = Map.of("registros", registros(filas, Map.of()));

        put_("/api/asignaciones/" + mat + "/sesiones/" + HOY.plusDays(2), cuerpo, luis).andExpect(status().isBadRequest());
        put_("/api/asignaciones/" + mat + "/sesiones/" + SABADO, cuerpo, luis).andExpect(status().isBadRequest());
        long ajeno = estudiantesDe(seccionId("Primaria", "1", "B", luis), luis).get(0).get("id").asLong();
        put_("/api/asignaciones/" + mat + "/sesiones/" + HOY, Map.of("registros", List.of(Map.of("estudianteId", ajeno, "estado", "asistio"))), luis)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje", containsString("no está matriculado en la sección de este curso")));
        put_("/api/asignaciones/" + mat + "/sesiones/" + HOY, cuerpo, docente(ELENA))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").value("Este curso no está asignado a tu cuenta."));
        put_("/api/asignaciones/" + mat + "/sesiones/2026-13-45", cuerpo, luis).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'fecha' no tiene un formato válido."));
    }

    @Test
    void historialReporteMensualEstudiantesYAlertasDelDocente() throws Exception {
        JsonNode sesion = ingresar("docente", LUIS, CLAVE);
        String luis = sesion.get("token").asText();
        long luisId = sesion.get("sesion").get("docenteId").asLong();
        long mat = asignacionId("MAT-1A", luis);
        JsonNode filas = leer(get_("/api/asignaciones/" + mat + "/sesiones/" + HOY, luis)).get("filas");
        long faltista = filas.get(0).get("alumnoId").asLong();
        // Tres faltas consecutivas del primer alumno (jueves 8, viernes 9 y lunes 12).
        for (LocalDate fecha : List.of(LocalDate.of(2026, 10, 8), VIERNES, LUNES)) {
            put_("/api/asignaciones/" + mat + "/sesiones/" + fecha, Map.of("registros", registros(filas, Map.of(0, "falta", 1, "tardanza"))), luis)
                    .andExpect(status().isOk());
        }

        get_("/api/docentes/" + luisId + "/historial", luis).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fecha").value(LUNES.toString()))
                .andExpect(jsonPath("$[0].asistieron").value(11))
                .andExpect(jsonPath("$[0].faltas").value(1))
                .andExpect(jsonPath("$[0].totalAlumnos").value(13));

        get_("/api/asignaciones/" + mat + "/resumen-mensual?mes=2026-10", luis)
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].sesiones").value(3))
                .andExpect(jsonPath("$[0].faltas").value(3))
                .andExpect(jsonPath("$[0].porcentaje").value(0))
                .andExpect(jsonPath("$[1].tardanzas").value(3))
                .andExpect(jsonPath("$[1].porcentaje").value(100));

        get_("/api/docentes/" + luisId + "/estudiantes", luis)
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].cursoNombre").value("Matemática (1ro grado A)"))
                .andExpect(jsonPath("$[?(@.id == " + faltista + ")].estadoGeneral").value("Crítico"));

        get_("/api/docentes/" + luisId + "/alertas", luis)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alumnoId").value(faltista))
                .andExpect(jsonPath("$[0].inasistenciasConsecutivas").value(3))
                .andExpect(jsonPath("$[0].nivelRiesgo").value("Crítico"))
                .andExpect(jsonPath("$[0].fechaUltimaInasistencia").value(LUNES.toString()));
    }

    @Test
    void unDocenteNoConsultaLosDatosDeOtro() throws Exception {
        long elenaId = ingresar("docente", ELENA, CLAVE).get("sesion").get("docenteId").asLong();
        String luis = docente(LUIS);
        get_("/api/docentes/" + elenaId + "/cursos", luis).andExpect(status().isForbidden());
        get_("/api/docentes/" + elenaId, luis).andExpect(status().isForbidden());
        put_("/api/docentes/" + elenaId + "/perfil", Map.of("nombres", "X", "apellidos", "Y"), luis).andExpect(status().isForbidden());
    }

    @Test
    void elDocenteEditaSuPerfil() throws Exception {
        JsonNode sesion = ingresar("docente", CARLOS, CLAVE);
        String token = sesion.get("token").asText();
        long id = sesion.get("sesion").get("docenteId").asLong();
        get_("/api/docentes/" + id, token).andExpect(jsonPath("$.codigoPlaza").value("PLZ-2026-9921"))
                .andExpect(jsonPath("$.email").value(CARLOS));
        put_("/api/docentes/" + id + "/perfil", Map.of("nombres", "Carlos", "apellidos", "Mendoza Ríos", "telefono", "999 888 777"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Carlos Mendoza Ríos"))
                .andExpect(jsonPath("$.telefono").value("999 888 777"));
        put_("/api/docentes/" + id + "/perfil", Map.of("nombres", "Carlos", "apellidos", "Mendoza", "telefono", "abc"), token)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0]").value("Ingresa un teléfono válido."));
        get_("/api/docentes/" + id + "/cursos", token).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void crudDeDocentesPorElAdministrador() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/docentes", Map.of("nombres", "Pedro", "apellidos", "Ruiz Soto", "dni", "40000001",
                "especialidad", "Arte"), admin).andExpect(status().isCreated())).get("id").asLong();
        post_("/api/docentes", Map.of("nombres", "Otro", "apellidos", "Docente", "dni", "40000001"), admin)
                .andExpect(status().isConflict());
        put_("/api/docentes/" + id, Map.of("nombres", "Pedro", "apellidos", "Ruiz Soto", "dni", "40000001", "condicion", "Contratado"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.condicion").value("Contratado"));
        delete_("/api/docentes/" + id, admin).andExpect(status().isNoContent());
        long luisId = ingresar("docente", LUIS, CLAVE).get("sesion").get("docenteId").asLong();
        delete_("/api/docentes/" + luisId, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("tiene cursos asignados")));
    }
}

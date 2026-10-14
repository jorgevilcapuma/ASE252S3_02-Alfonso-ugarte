package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Pantallas del alumno: inicio, cursos/horario, mi asistencia, alertas, comunicaciones y perfil. */
class PortalAlumnoTest extends PruebaApi {

    private long juanId;
    private String alumno;

    private void ingresarComoJuan() throws Exception {
        JsonNode sesion = ingresarAlumno("Juan Pérez Ugarte", "72819201");
        alumno = sesion.get("token").asText();
        juanId = sesion.get("sesion").get("estudianteId").asLong();
    }

    @Test
    void cursosYHorarioDeSuSeccion() throws Exception {
        ingresarComoJuan();
        get_("/api/alumnos/" + juanId + "/perfil", alumno).andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("Primaria")).andExpect(jsonPath("$.grado").value("1")).andExpect(jsonPath("$.seccion").value("A"));
        get_("/api/alumnos/" + juanId + "/cursos", alumno).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[?(@.codigo == 'MAT-1A')].docente").value("Prof. Luis Mendoza Castro"))
                .andExpect(jsonPath("$[?(@.codigo == 'MAT-1A')].horarios[0].dia").value("Lunes"))
                .andExpect(jsonPath("$[?(@.codigo == 'MAT-1A')].horarios[0].inicio").value("09:15"))
                .andExpect(jsonPath("$[?(@.codigo == 'TUT-1A')].horario").value("Miércoles 12:30 - 14:00"));
    }

    @Test
    void historialPorCursoYPorDiaConResumen() throws Exception {
        ingresarComoJuan();
        String admin = admin();
        String luis = docente(LUIS);
        long mat = asignacionId("MAT-1A", luis);
        JsonNode filas = leer(get_("/api/asignaciones/" + mat + "/sesiones/" + LUNES, luis)).get("filas");
        List<Map<String, Object>> registros = new ArrayList<>();
        filas.forEach(f -> registros.add(Map.of("estudianteId", f.get("alumnoId").asLong(),
                "estado", f.get("alumnoId").asLong() == juanId ? "tardanza" : "asistio")));
        put_("/api/asignaciones/" + mat + "/sesiones/" + LUNES, Map.of("registros", registros), luis).andExpect(status().isOk());
        put_("/api/asistencia/estudiantes/" + juanId + "/" + VIERNES, Map.of("estado", "presente"), admin).andExpect(status().isOk());

        get_("/api/alumnos/" + juanId + "/asistencia", alumno).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fecha").value(HOY.toString()))
                .andExpect(jsonPath("$[0].estado").value("pendiente"))
                .andExpect(jsonPath("$[1].fecha").value(LUNES.toString()))
                .andExpect(jsonPath("$[1].cursoNombre").value("Matemática"))
                .andExpect(jsonPath("$[1].estado").value("tardanza"))
                .andExpect(jsonPath("$[1].hora").value("09:15–10:30"))
                .andExpect(jsonPath("$[1].docente").value("Prof. Luis Mendoza Castro"))
                .andExpect(jsonPath("$[2].fecha").value(VIERNES.toString()))
                .andExpect(jsonPath("$[2].estado").value("presente"));
        get_("/api/alumnos/" + juanId + "/asistencia?cursoId=" + mat, alumno).andExpect(jsonPath("$.length()").value(1));
        get_("/api/alumnos/" + juanId + "/asistencia/resumen", alumno)
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.asistencias").value(1))
                .andExpect(jsonPath("$.tardanzas").value(1))
                .andExpect(jsonPath("$.porcentajeAsistencia").value(100))
                .andExpect(jsonPath("$.porcentajeInasistencia").value(0));
    }

    @Test
    void inicioAlertasYComunicaciones() throws Exception {
        ingresarComoJuan();
        String admin = admin();
        put_("/api/asistencia/estudiantes/" + juanId + "/" + HOY, Map.of("estado", "falta"), admin).andExpect(status().isOk());

        get_("/api/alumnos/" + juanId + "/inicio", alumno).andExpect(status().isOk())
                .andExpect(jsonPath("$.institucion").value("I.E. 20191 Alfonso Ugarte"))
                .andExpect(jsonPath("$.estadoHoy").value("falta"))
                .andExpect(jsonPath("$.cursos").value(9));
        get_("/api/alumnos/" + juanId + "/alertas", alumno)
                .andExpect(jsonPath("$[*].titulo", hasItem("Falta registrada")));

        get_("/api/alumnos/" + juanId + "/comunicaciones", alumno).andExpect(jsonPath("$.length()").value(2));
        String luis = docente(LUIS);
        post_("/api/comunicados", Map.of("titulo", "Práctica calificada", "contenido", "Traer regla y compás.", "prioridad", "Alta",
                "cursoId", asignacionId("MAT-1A", luis)), luis).andExpect(status().isCreated());
        get_("/api/alumnos/" + juanId + "/comunicaciones", alumno).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].titulo").value("Importante: Práctica calificada"))
                .andExpect(jsonPath("$[0].detalle").value("Traer regla y compás. · Matemática"));
        get_("/api/comunicados", alumno).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void actualizaSuContactoYNoVeDatosDeOtros() throws Exception {
        ingresarComoJuan();
        put_("/api/estudiantes/" + juanId + "/contacto", Map.of("telefono", "987 654 321", "correo", "Juan.Perez@Correo.com"), alumno)
                .andExpect(status().isOk()).andExpect(jsonPath("$.correo").value("juan.perez@correo.com"));
        put_("/api/estudiantes/" + juanId + "/contacto", Map.of("correo", "no-es-correo"), alumno).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Ingresa un correo válido."));
        long otro = juanId + 1;
        get_("/api/alumnos/" + otro + "/asistencia", alumno).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puedes consultar tu propia información."));
        get_("/api/estudiantes/" + otro, alumno).andExpect(status().isForbidden());
        put_("/api/estudiantes/" + otro + "/contacto", Map.of("telefono", "999999999"), alumno).andExpect(status().isForbidden());
    }

    @Test
    void alumnoConCuentaIngresaConCorreo() throws Exception {
        JsonNode sesion = ingresar("alumno", "lucia.bustamante@ie20191.edu.pe", CLAVE).get("sesion");
        get_("/api/alumnos/" + sesion.get("estudianteId").asLong() + "/perfil", token("alumno", "lucia.bustamante@ie20191.edu.pe"))
                .andExpect(jsonPath("$.nombre").value("Lucía Bustamante Morales"))
                .andExpect(jsonPath("$.grado").value("1"));
    }
}

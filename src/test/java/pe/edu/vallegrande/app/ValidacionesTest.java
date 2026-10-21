package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Reglas de validación generales: nombres, correos, DNI único entre personas, grados por nivel y revisiones. */
class ValidacionesTest extends PruebaApi {

    private Map<String, Object> estudiante(String nombres, String dni, long aula) {
        Map<String, Object> e = new HashMap<>();
        e.put("nombres", nombres);
        e.put("apellidos", "Torres Vega");
        e.put("dni", dni);
        e.put("gradoSeccionId", aula);
        return e;
    }

    private Map<String, Object> docente(String nombres, String dni) {
        Map<String, Object> d = new HashMap<>();
        d.put("nombres", nombres);
        d.put("apellidos", "Quispe Torres");
        d.put("dni", dni);
        d.put("cargo", "Auxiliar");
        d.put("sexo", "F");
        return d;
    }

    @Test
    void nombresSoloConLetrasYCorreosConDominio() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "2", "A", admin);
        post_("/api/estudiantes", estudiante("Ju4n", "61000001", aula), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("Los nombres y apellidos solo admiten letras, espacios, apóstrofo o guion.")));
        Map<String, Object> conCorreo = estudiante("María José", "61000002", aula);
        conCorreo.put("correo", "maria@colegio");
        post_("/api/estudiantes", conCorreo, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("Ingresa un correo válido, por ejemplo nombre@ie20191.edu.pe.")));
        // Tildes, ñ, apóstrofo y guion son válidos.
        post_("/api/estudiantes", estudiante("María-José D'Ávila Ñaupari", "61000003", aula), admin).andExpect(status().isCreated());
        post_("/api/docentes", docente("R0sa", "45000001"), admin).andExpect(status().isBadRequest());
        post_("/api/usuarios", Map.of("nombre", "Admin 2", "correo", "admin2@ie20191.edu.pe", "perfil", "administrador"), admin)
                .andExpect(status().isBadRequest());
    }

    @Test
    void unDniNoPuedeSerDeUnDocenteYDeUnEstudiante() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "2", "A", admin);
        String dniDocente = leer(get_("/api/docentes", admin)).get(0).get("dni").asText();
        String dniEstudiante = leer(get_("/api/estudiantes?gradoSeccionId=" + aula, admin)).get(0).get("dni").asText();
        post_("/api/estudiantes", estudiante("Ana", dniDocente, aula), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El DNI " + dniDocente + " ya pertenece a un docente o personal del colegio. Verifica el número."));
        post_("/api/docentes", docente("Rosa", dniEstudiante), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El DNI " + dniEstudiante + " ya pertenece a un estudiante. Verifica el número."));
    }

    @Test
    void elGradoDebeExistirEnSuNivel() throws Exception {
        String admin = admin();
        post_("/api/grados-secciones", Map.of("nivel", "Primaria", "grado", "9", "seccion", "Z"), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("En Primaria el grado va de 1 a 6."));
        post_("/api/grados-secciones", Map.of("nivel", "Secundaria", "grado", "6", "seccion", "Z"), admin).andExpect(status().isBadRequest());
        post_("/api/grados-secciones", Map.of("nivel", "Inicial", "grado", "abc", "seccion", "Z"), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("En Inicial el grado es 3, 4 o 5 años."));
        // Se normaliza: "4" en Inicial es "4 años"; "3ro" en Secundaria es "3".
        post_("/api/grados-secciones", Map.of("nivel", "Inicial", "grado", "4", "seccion", "Z"), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.grado").value("4 años"));
        post_("/api/grados-secciones", Map.of("nivel", "Secundaria", "grado", "3ro", "seccion", "Z"), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.grado").value("3"));
    }

    @Test
    void cursosYJustificacionesConTextoSuficiente() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "2", "A", admin);
        long docente = ingresar("docente", LUIS, CLAVE).get("sesion").get("docenteId").asLong();
        post_("/api/cursos", Map.of("nombre", "Ma", "gradoSeccionId", aula, "docenteId", docente), admin).andExpect(status().isBadRequest());

        long juan = juanPerez();
        put_("/api/asistencia-estudiantes/estudiantes/" + juan, Map.of("fecha", LUNES.toString(), "estado", "falta"), admin).andExpect(status().isOk());
        String alumno = ingresarAlumno("Juan Pérez Ugarte", "72819201").get("token").asText();
        post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", "x"), alumno).andExpect(status().isBadRequest());
        long id = leer(post_("/api/justificaciones", Map.of("fecha", LUNES.toString(), "motivo", "Cita médica"), alumno)
                .andExpect(status().isCreated())).get("id").asLong();
        // Observar exige el motivo; aprobar no.
        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Observada"), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Para observar la solicitud, escribe el motivo (al menos 5 caracteres)."));
        patch_("/api/justificaciones/" + id + "/revision", Map.of("estado", "Observada", "observacion", "Falta la constancia médica"), admin)
                .andExpect(status().isOk());
    }
}

package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Comunicados de docentes: por curso o generales, con control de autoría. */
class ComunicadoTest extends PruebaApi {

    @Test
    void elDocentePublicaEnSusCursosYEnGeneral() throws Exception {
        String luis = docente(LUIS);
        long mat = asignacionId("MAT-1A", luis);
        long id = leer(post_("/api/comunicados", Map.of("titulo", "Tarea", "contenido", "Resolver la página 20.", "prioridad", "Media", "cursoId", mat), luis)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cursoNombre").value("Matemática - 1ro grado A"))
                .andExpect(jsonPath("$.fechaCreacion").value(HOY.toString()))
                .andExpect(jsonPath("$.autor").value("Luis Mendoza Castro"))).get("id").asLong();
        post_("/api/comunicados", Map.of("titulo", "Reunión", "contenido", "Reunión de padres.", "prioridad", "Baja"), luis)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cursoNombre").value("Todos los cursos"));

        get_("/api/comunicados", luis).andExpect(jsonPath("$.length()").value(2));
        get_("/api/comunicados", docente(ELENA)).andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        get_("/api/comunicados", admin()).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void controlaPermisosYAutoria() throws Exception {
        String luis = docente(LUIS);
        String elena = docente(ELENA);
        post_("/api/comunicados", Map.of("titulo", "X", "contenido", "Y", "prioridad", "Alta", "cursoId", asignacionId("COM-1A", elena)), luis)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").value("Solo puedes publicar comunicados en tus cursos."));
        long id = leer(post_("/api/comunicados", Map.of("titulo", "Aviso", "contenido", "Texto", "prioridad", "Alta"), luis)).get("id").asLong();
        put_("/api/comunicados/" + id, Map.of("titulo", "Cambio", "contenido", "Texto", "prioridad", "Alta"), elena).andExpect(status().isForbidden());
        put_("/api/comunicados/" + id, Map.of("titulo", "Aviso corregido", "contenido", "Texto", "prioridad", "Baja"), admin())
                .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Aviso corregido"));
        delete_("/api/comunicados/" + id, elena).andExpect(status().isForbidden());
        delete_("/api/comunicados/" + id, luis).andExpect(status().isNoContent());
        get_("/api/comunicados/" + id, luis).andExpect(status().isNotFound());

        post_("/api/comunicados", Map.of("titulo", "X", "contenido", "Y", "prioridad", "Urgente"), luis).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("Valores permitidos: Alta, Media, Baja")));
        String alumno = ingresarAlumno("Juan Pérez Ugarte", "72819201").get("token").asText();
        post_("/api/comunicados", Map.of("titulo", "X", "contenido", "Y", "prioridad", "Alta"), alumno).andExpect(status().isForbidden());
    }
}

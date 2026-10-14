package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Tablas maestras de estructura: niveles, grados y secciones. */
class EstructuraTest extends PruebaApi {

    @Test
    void estructuraSembradaComoEnElFrontend() throws Exception {
        get_("/api/estructura", admin()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Inicial"))
                .andExpect(jsonPath("$[0].grados.length()").value(3))
                .andExpect(jsonPath("$[0].grados[0].codigo").value("3 años"))
                .andExpect(jsonPath("$[1].grados.length()").value(6))
                .andExpect(jsonPath("$[1].grados[0].etiqueta").value("1ro grado"))
                .andExpect(jsonPath("$[2].grados.length()").value(5))
                .andExpect(jsonPath("$[2].grados[2].etiqueta").value("3ro año"))
                .andExpect(jsonPath("$[1].grados[0].secciones.length()").value(3))
                .andExpect(jsonPath("$[1].grados[0].secciones[0].turno").value("Mañana"))
                .andExpect(jsonPath("$[1].grados[0].secciones[2].turno").value("Tarde"))
                // 12 estudiantes por aula + el alumno de la demo de ingreso en 1° Primaria A
                .andExpect(jsonPath("$[1].grados[0].secciones[0].totalEstudiantes").value(13))
                .andExpect(jsonPath("$[1].grados[0].secciones[1].totalEstudiantes").value(12));
    }

    @Test
    void crudDeNivelesConDuplicados() throws Exception {
        String admin = admin();
        JsonNode creado = leer(post_("/api/niveles", Map.of("nombre", "Educación Básica Alternativa", "orden", 4), admin)
                .andExpect(status().isCreated()));
        long id = creado.get("id").asLong();

        post_("/api/niveles", Map.of("nombre", "educación básica alternativa", "orden", 5), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("Ya existe el nivel")));
        put_("/api/niveles/" + id, Map.of("nombre", "EBA", "orden", 4), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("EBA"));
        delete_("/api/niveles/" + id, admin).andExpect(status().isNoContent());
        get_("/api/niveles", admin).andExpect(jsonPath("$.length()").value(3));
        delete_("/api/niveles/" + id, admin).andExpect(status().isNotFound());
    }

    @Test
    void noSeEliminaUnNivelConGradosNiUnaSeccionConMatriculas() throws Exception {
        String admin = admin();
        long primaria = leer(get_("/api/niveles", admin)).get(1).get("id").asLong();
        delete_("/api/niveles/" + primaria, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar el nivel Primaria porque tiene grados registrados."));
        delete_("/api/secciones/" + seccionId("Primaria", "1", "A", admin), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar la sección porque tiene estudiantes matriculados."));
    }

    @Test
    void crudDeGradosYSeccionesConTurnoPorDefecto() throws Exception {
        String admin = admin();
        long secundaria = leer(get_("/api/niveles", admin)).get(2).get("id").asLong();
        long grado = leer(post_("/api/grados", Map.of("nivelId", secundaria, "codigo", "6", "etiqueta", "6to año", "orden", 6), admin)
                .andExpect(status().isCreated())).get("id").asLong();
        post_("/api/grados", Map.of("nivelId", secundaria, "codigo", "6", "etiqueta", "Otro", "orden", 7), admin)
                .andExpect(status().isConflict());

        long seccionA = leer(post_("/api/secciones", Map.of("gradoId", grado, "letra", "a"), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.letra").value("A"))
                .andExpect(jsonPath("$.turno").value("Mañana"))).get("id").asLong();
        long seccionC = leer(post_("/api/secciones", Map.of("gradoId", grado, "letra", "C"), admin)
                .andExpect(jsonPath("$.turno").value("Tarde"))).get("id").asLong();
        post_("/api/secciones", Map.of("gradoId", grado, "letra", "A"), admin).andExpect(status().isConflict());
        put_("/api/secciones/" + seccionC, Map.of("gradoId", grado, "letra", "C", "turno", "Mañana"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.turno").value("Mañana"));

        delete_("/api/grados/" + grado, admin).andExpect(status().isConflict());
        delete_("/api/secciones/" + seccionA, admin).andExpect(status().isNoContent());
        delete_("/api/secciones/" + seccionC, admin).andExpect(status().isNoContent());
        delete_("/api/grados/" + grado, admin).andExpect(status().isNoContent());
    }

    @Test
    void validacionesConMensajesClaros() throws Exception {
        String admin = admin();
        post_("/api/niveles", Map.of(), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("Ingresa el nombre del nivel.")))
                .andExpect(jsonPath("$.errores", hasItem("Indica el orden del nivel.")));
        Map<String, Object> seccion = new HashMap<>();
        seccion.put("gradoId", 1);
        seccion.put("letra", "AB");
        post_("/api/secciones", seccion, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("La sección es una sola letra (A, B, C...)."));
        post_("/api/secciones", Map.of("gradoId", 99999, "letra", "Z"), admin).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe el grado con id 99999."));
        post_("/api/secciones", "{ no es json", admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no tiene el formato JSON esperado."));
    }

    @Test
    void docentePuedeLeerPeroNoModificar() throws Exception {
        String docente = docente(ELENA);
        get_("/api/secciones?nivel=Secundaria", docente).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(15));
        post_("/api/niveles", Map.of("nombre", "X", "orden", 9), docente).andExpect(status().isForbidden());
    }
}

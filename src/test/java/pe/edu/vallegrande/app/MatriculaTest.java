package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Matrículas: alta, traslado, retiro, anulación y consultas. */
class MatriculaTest extends PruebaApi {

    private JsonNode matricular(String nombre, String dni, String nivel, String grado, String seccion, String admin) throws Exception {
        return leer(post_("/api/matriculas", Map.of("nombre", nombre, "dni", dni, "nivel", nivel, "grado", grado, "seccion", seccion), admin)
                .andExpect(status().isCreated()));
    }

    @Test
    void listaPaginadaConFiltrosYResumenPorNivel() throws Exception {
        String admin = admin();
        get_("/api/matriculas?tamanio=20", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(505))
                .andExpect(jsonPath("$.contenido.length()").value(20))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(26))
                .andExpect(jsonPath("$.contenido[0].nivel").value("Inicial"));
        get_("/api/matriculas?nivel=primaria&grado=1&seccion=a", admin).andExpect(jsonPath("$.totalElementos").value(13));
        get_("/api/matriculas?q=72819201", admin).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Juan Pérez Ugarte"));
        get_("/api/matriculas?q=pérez ugarte", admin).andExpect(jsonPath("$.totalElementos").value(1));
        get_("/api/matriculas/resumen", admin)
                .andExpect(jsonPath("$[0].nivel").value("Inicial")).andExpect(jsonPath("$[0].total").value(108))
                .andExpect(jsonPath("$[1].total").value(217))
                .andExpect(jsonPath("$[2].total").value(180));
        get_("/api/matriculas?tamanio=0", admin).andExpect(status().isBadRequest());
    }

    @Test
    void matriculaSeparandoNombresYApellidos() throws Exception {
        String admin = admin();
        JsonNode e = matricular("ana lucía   Torres Vega", "76543210", "Primaria", "2", "B", admin);
        org.assertj.core.api.Assertions.assertThat(e.get("nombres").asText()).isEqualTo("ana lucía");
        org.assertj.core.api.Assertions.assertThat(e.get("apellidos").asText()).isEqualTo("Torres Vega");
        org.assertj.core.api.Assertions.assertThat(e.get("gradoEtiqueta").asText()).isEqualTo("2do grado");
        org.assertj.core.api.Assertions.assertThat(e.get("estadoMatricula").asText()).isEqualTo("Activa");
        get_("/api/secciones/" + e.get("seccionId").asLong(), admin).andExpect(jsonPath("$.totalEstudiantes").value(13));
    }

    @Test
    void evitaDuplicadosYValidaElAula() throws Exception {
        String admin = admin();
        post_("/api/matriculas", Map.of("nombre", "Juan Pérez Ugarte", "dni", "72819201", "nivel", "Primaria", "grado", "1", "seccion", "A"), admin)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("Ya existe un estudiante matriculado con ese DNI."));
        post_("/api/matriculas", Map.of("nombre", "Otro Alumno", "dni", "72819201", "nivel", "Primaria", "grado", "1", "seccion", "A"), admin)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", containsString("ya está registrado con otro nombre")));
        post_("/api/matriculas", Map.of("nombre", "Nuevo Alumno", "dni", "76543211", "nivel", "Inicial", "grado", "6", "seccion", "A"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Selecciona un grado y una sección válidos para el nivel Inicial."));
        post_("/api/matriculas", Map.of("nombre", "Nuevo", "dni", "7654", "nivel", "Primaria", "grado", "1", "seccion", "A"), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0]").value("El DNI debe tener 8 dígitos."));
        post_("/api/matriculas", Map.of("nombre", "Nuevo", "dni", "76543211", "nivel", "Primaria", "grado", "1", "seccion", "A"), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("Ingresa nombres y apellidos."));
        post_("/api/matriculas", Map.of("nombre", "Nuevo Alumno", "dni", "76543211"), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("Selecciona el nivel, el grado y la sección."));
    }

    @Test
    void trasladoConservaElHistorial() throws Exception {
        String admin = admin();
        JsonNode e = matricular("Rosa María Paz Luna", "76543212", "Primaria", "1", "A", admin);
        long estudiante = e.get("id").asLong();
        put_("/api/asistencia/estudiantes/" + estudiante + "/" + LUNES, Map.of("estado", "tardanza"), admin).andExpect(status().isOk());

        put_("/api/matriculas/" + e.get("matriculaId").asLong() + "/traslado", Map.of("nivel", "Secundaria", "grado", "1", "seccion", "C"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("Secundaria"))
                .andExpect(jsonPath("$.seccion").value("C"))
                .andExpect(jsonPath("$.turno").value("Tarde"));
        get_("/api/estudiantes/" + estudiante + "/asistencia", admin)
                .andExpect(jsonPath("$[0].fecha").value(LUNES.toString()))
                .andExpect(jsonPath("$[0].estado").value("tardanza"));
        put_("/api/matriculas/" + e.get("matriculaId").asLong() + "/traslado", Map.of("nivel", "Secundaria", "grado", "1", "seccion", "C"), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("El estudiante ya pertenece a esa sección."));
    }

    @Test
    void retiroYReincorporacion() throws Exception {
        String admin = admin();
        JsonNode e = matricular("Mario Ruiz Díaz", "76543213", "Inicial", "4 años", "B", admin);
        long matricula = e.get("matriculaId").asLong();
        patch_("/api/matriculas/" + matricula + "/retiro", null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoMatricula").value("Retirada"));
        get_("/api/matriculas?q=76543213", admin).andExpect(jsonPath("$.totalElementos").value(0));
        get_("/api/matriculas?q=76543213&estado=Retirada", admin).andExpect(jsonPath("$.totalElementos").value(1));
        patch_("/api/matriculas/" + matricula + "/retiro", null, admin).andExpect(status().isConflict());
        post_("/api/matriculas", Map.of("nombre", "Mario Ruiz Díaz", "dni", "76543213", "nivel", "Inicial", "grado", "4 años", "seccion", "B"), admin)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", containsString("reincorpóralo")));
        patch_("/api/matriculas/" + matricula + "/reincorporacion", null, admin).andExpect(jsonPath("$.estadoMatricula").value("Activa"));
    }

    @Test
    void anularEliminaSinHistorialYRetiraConHistorial() throws Exception {
        String admin = admin();
        JsonNode sinHistorial = matricular("Pía Soto Ramos", "76543214", "Primaria", "3", "A", admin);
        delete_("/api/matriculas/" + sinHistorial.get("matriculaId").asLong(), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Matrícula anulada."));
        get_("/api/estudiantes/" + sinHistorial.get("id").asLong(), admin).andExpect(status().isNotFound());

        JsonNode conHistorial = matricular("Leo Vega Paz", "76543215", "Primaria", "3", "A", admin);
        put_("/api/asistencia/estudiantes/" + conHistorial.get("id").asLong() + "/" + HOY, Map.of("estado", "presente"), admin);
        delete_("/api/matriculas/" + conHistorial.get("matriculaId").asLong(), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", containsString("retirada para conservar su historial")));
        get_("/api/estudiantes/" + conHistorial.get("id").asLong(), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoMatricula").value("Retirada"));
        delete_("/api/estudiantes/" + conHistorial.get("id").asLong(), admin).andExpect(status().isConflict());
    }

    @Test
    void corrigeDatosPersonalesSinDuplicarDni() throws Exception {
        String admin = admin();
        JsonNode e = matricular("Iris Campos León", "76543216", "Primaria", "5", "B", admin);
        put_("/api/estudiantes/" + e.get("id").asLong(), Map.of("nombres", "Iris Andrea", "apellidos", "Campos León", "dni", "76543216"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Iris Andrea Campos León"));
        put_("/api/estudiantes/" + e.get("id").asLong(), Map.of("nombres", "Iris", "apellidos", "Campos", "dni", "72819201"), admin)
                .andExpect(status().isConflict());
    }
}

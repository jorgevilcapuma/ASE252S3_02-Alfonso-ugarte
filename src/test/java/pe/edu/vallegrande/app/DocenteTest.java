package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Tabla docentes: docentes y demás personal, con eliminación y restauración lógicas. */
class DocenteTest extends PruebaApi {

    private Map<String, Object> persona(String dni) {
        Map<String, Object> p = new HashMap<>();
        p.put("nombres", "  Rosa   María ");
        p.put("apellidos", "Quispe Torres");
        p.put("dni", dni);
        p.put("cargo", "auxiliar");
        p.put("area", "Secundaria");
        p.put("celular", "987654321");
        p.put("correo", "Rosa.Quispe@IE20191.edu.pe");
        p.put("sexo", "f");
        p.put("fechaIngreso", "2024-03-01");
        return p;
    }

    @Test
    void listaLosSembradosYFiltraPorEstadoYCargo() throws Exception {
        String admin = admin();
        get_("/api/docentes", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[*].cargo", hasItem("Administrativo")));
        get_("/api/docentes?estado=A", admin).andExpect(jsonPath("$.length()").value(13));
        get_("/api/docentes?estado=i", admin).andExpect(jsonPath("$.length()").value(0));
        get_("/api/docentes?cargo=docente", admin).andExpect(jsonPath("$.length()").value(9));
        get_("/api/docentes?estado=X", admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El estado debe ser A (activos), I (inactivos) o T (todos)."));
    }

    @Test
    void creaObtieneYEdita() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/docentes", persona("45678912"), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombres").value("Rosa María"))
                .andExpect(jsonPath("$.cargo").value("Auxiliar"))
                .andExpect(jsonPath("$.sexo").value("F"))
                .andExpect(jsonPath("$.correo").value("rosa.quispe@ie20191.edu.pe"))
                .andExpect(jsonPath("$.estado").value("A"))
                .andExpect(jsonPath("$.creadoEn", notNullValue()))).get("id").asLong();

        get_("/api/docentes/" + id, admin).andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value("Quispe Torres, Rosa María"));

        Map<String, Object> cambios = persona("45678912");
        cambios.put("cargo", "Docente");
        cambios.put("especialidad", "Matemática");
        put_("/api/docentes/" + id, cambios, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("Docente"))
                .andExpect(jsonPath("$.especialidad").value("Matemática"))
                .andExpect(jsonPath("$.actualizadoEn", notNullValue()));
        get_("/api/docentes/999999", admin).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe el docente o personal con id 999999."));
    }

    @Test
    void eliminaYRestauraLogicamente() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/docentes", persona("45678913"), admin)).get("id").asLong();

        patch_("/api/docentes/" + id + "/eliminar", null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("I"));
        get_("/api/docentes?estado=I", admin).andExpect(jsonPath("$.length()").value(1));
        get_("/api/docentes/" + id, admin).andExpect(status().isOk());   // sigue en la base de datos
        patch_("/api/docentes/" + id + "/eliminar", null, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El registro ya está eliminado."));
        put_("/api/docentes/" + id, persona("45678913"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El registro está eliminado; restáuralo antes de editarlo."));

        patch_("/api/docentes/" + id + "/restaurar", null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("A"));
        patch_("/api/docentes/" + id + "/restaurar", null, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El registro ya está activo."));
    }

    @Test
    void conCursosAsignadosNoSeEliminaNiCambiaDeCargo() throws Exception {
        String admin = admin();
        JsonNode luis = ingresar("docente", LUIS, CLAVE).get("sesion");
        long id = luis.get("docenteId").asLong();
        patch_("/api/docentes/" + id + "/eliminar", null, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar porque tiene cursos asignados. Reasigna sus cursos primero."));

        JsonNode ficha = leer(get_("/api/docentes/" + id, admin));
        Map<String, Object> cambios = new HashMap<>(Map.of("nombres", ficha.get("nombres").asText(), "apellidos", ficha.get("apellidos").asText(),
                "dni", ficha.get("dni").asText(), "cargo", "Administrativo", "sexo", "M"));
        put_("/api/docentes/" + id, cambios, admin).andExpect(status().isConflict());
    }

    @Test
    void validaDatosYEvitaDuplicados() throws Exception {
        String admin = admin();
        post_("/api/docentes", persona("45678914"), admin).andExpect(status().isCreated());
        post_("/api/docentes", persona("45678914"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe personal registrado con el DNI 45678914."));
        post_("/api/docentes", persona("45678915"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo rosa.quispe@ie20191.edu.pe ya está registrado."));

        Map<String, Object> malo = persona("123");
        malo.put("cargo", "Jardinero");
        malo.put("celular", "12345");
        malo.put("sexo", "X");
        malo.put("nombres", "");
        post_("/api/docentes", malo, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("El DNI debe tener exactamente 8 dígitos.")))
                .andExpect(jsonPath("$.errores", hasItem("Selecciona el cargo (Docente, Auxiliar, Administrativo, Directivo o Servicio).")))
                .andExpect(jsonPath("$.errores", hasItem("El celular debe tener 9 dígitos y empezar con 9.")))
                .andExpect(jsonPath("$.errores", hasItem("Indica el sexo (M o F).")))
                .andExpect(jsonPath("$.errores", hasItem("Ingresa los nombres.")));
    }

    @Test
    void elDocenteSoloVeSuFichaYSusCursos() throws Exception {
        String admin = admin();
        JsonNode sesion = ingresar("docente", LUIS, CLAVE);
        String luis = sesion.get("token").asText();
        long id = sesion.get("sesion").get("docenteId").asLong();
        long elena = ingresar("docente", ELENA, CLAVE).get("sesion").get("docenteId").asLong();

        get_("/api/docentes/" + id, luis).andExpect(status().isOk());
        get_("/api/docentes/" + id + "/cursos", luis).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreConAula").value("Matemática (1ro grado A)"));
        get_("/api/docentes/" + elena, luis).andExpect(status().isForbidden());
        get_("/api/docentes", luis).andExpect(status().isForbidden());
        get_("/api/docentes", null).andExpect(status().isUnauthorized());
        get_("/api/docentes/" + elena + "/cursos", admin).andExpect(jsonPath("$.length()").value(3));
    }
}

package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Tabla maestra del personal: listar, por estado, por id, crear, editar, eliminar y restaurar lógicos. */
class PersonalTest extends PruebaApi {

    private Map<String, Object> personal(String dni) {
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
    void listaElPersonalSembradoYFiltraPorEstado() throws Exception {
        String admin = admin();
        get_("/api/personal", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[*].cargo", hasItem("Docente")));
        get_("/api/personal/estado/A", admin).andExpect(jsonPath("$.length()").value(9));
        get_("/api/personal/estado/i", admin).andExpect(jsonPath("$.length()").value(0));
        get_("/api/personal/estado/T", admin).andExpect(jsonPath("$.length()").value(9));
        get_("/api/personal/estado/X", admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El estado debe ser A (activos), I (inactivos) o T (todos)."));
    }

    @Test
    void creaObtieneYEditaPersonal() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/personal", personal("45678912"), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombres").value("Rosa María"))
                .andExpect(jsonPath("$.cargo").value("Auxiliar"))
                .andExpect(jsonPath("$.sexo").value("F"))
                .andExpect(jsonPath("$.correo").value("rosa.quispe@ie20191.edu.pe"))
                .andExpect(jsonPath("$.estado").value("A"))
                .andExpect(jsonPath("$.createdAt", notNullValue()))).get("id").asLong();

        get_("/api/personal/" + id, admin).andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value("Quispe Torres, Rosa María"));

        Map<String, Object> cambios = personal("45678912");
        cambios.put("cargo", "Administrativo");
        cambios.put("area", "Secretaría");
        put_("/api/personal/" + id, cambios, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("Administrativo"))
                .andExpect(jsonPath("$.area").value("Secretaría"))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
        get_("/api/personal/999999", admin).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe personal con id 999999."));
    }

    @Test
    void eliminaYRestauraLogicamente() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/personal", personal("45678913"), admin)).get("id").asLong();

        patch_("/api/personal/eliminar/" + id, null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("I"))
                .andExpect(jsonPath("$.deletedAt", notNullValue()));
        get_("/api/personal/estado/I", admin).andExpect(jsonPath("$.length()").value(1));
        get_("/api/personal/" + id, admin).andExpect(status().isOk());   // sigue en la base de datos
        patch_("/api/personal/eliminar/" + id, null, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El personal ya está eliminado."));
        put_("/api/personal/" + id, personal("45678913"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El personal está eliminado; restáuralo antes de editarlo."));

        patch_("/api/personal/restaurar/" + id, null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("A"))
                .andExpect(jsonPath("$.restoredAt", notNullValue()));
        patch_("/api/personal/restaurar/" + id, null, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El personal ya está activo."));
    }

    @Test
    void validaDatosYEvitaDuplicados() throws Exception {
        String admin = admin();
        post_("/api/personal", personal("45678914"), admin).andExpect(status().isCreated());
        post_("/api/personal", personal("45678914"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe personal registrado con el DNI 45678914."));
        post_("/api/personal", personal("45678915"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo rosa.quispe@ie20191.edu.pe ya está registrado."));

        Map<String, Object> malo = personal("123");
        malo.put("cargo", "Jardinero");
        malo.put("celular", "12345");
        malo.put("sexo", "X");
        malo.put("nombres", "");
        post_("/api/personal", malo, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("El DNI debe tener exactamente 8 dígitos.")))
                .andExpect(jsonPath("$.errores", hasItem("Selecciona el cargo (Docente, Auxiliar, Administrativo, Directivo o Servicio).")))
                .andExpect(jsonPath("$.errores", hasItem("El celular debe tener 9 dígitos y empezar con 9.")))
                .andExpect(jsonPath("$.errores", hasItem("Indica el sexo (M o F).")))
                .andExpect(jsonPath("$.errores", hasItem("Ingresa los nombres.")));
    }

    @Test
    void soloElAdministradorGestionaElPersonal() throws Exception {
        get_("/api/personal", docente(ELENA)).andExpect(status().isForbidden());
        get_("/api/personal", null).andExpect(status().isUnauthorized());
    }
}

package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Configuración institucional (datos e identidad visual) y fases del proyecto. */
class InstitucionFaseTest extends PruebaApi {

    private static final String LOGO = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";

    private Map<String, Object> datos() {
        Map<String, Object> d = new HashMap<>();
        d.put("nombre", "I.E. N° 20191 Alfonso Ugarte");
        d.put("codigoModular", "0285940");
        d.put("direccion", "Av. Principal 123, San Vicente de Cañete");
        d.put("colorPrincipal", "#1A2B3C");
        d.put("colorSecundario", "#fdc425");
        d.put("logo", LOGO);
        return d;
    }

    @Test
    void guardaDatosEIdentidadVisualYLaRestablece() throws Exception {
        String admin = admin();
        put_("/api/institucion", datos(), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("I.E. N° 20191 Alfonso Ugarte"))
                .andExpect(jsonPath("$.colorPrincipal").value("#1a2b3c"))
                .andExpect(jsonPath("$.logo").value(LOGO));
        get_("/api/institucion", null).andExpect(jsonPath("$.direccion").value("Av. Principal 123, San Vicente de Cañete"));
        post_("/api/institucion/restablecer-identidad", null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.colorPrincipal").value("#9b1b1f"))
                .andExpect(jsonPath("$.logo").value(""));
    }

    @Test
    void validaCodigoModularColoresYLogotipo() throws Exception {
        Map<String, Object> d = datos();
        d.put("codigoModular", "12345");
        d.put("colorPrincipal", "rojo");
        d.put("logo", "data:text/html;base64,PGgxPg==");
        d.put("nombre", "IE");
        put_("/api/institucion", d, admin()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("El código modular tiene 7 dígitos.")))
                .andExpect(jsonPath("$.errores", hasItem("Usa colores hexadecimales de 6 dígitos, por ejemplo #9b1b1f.")))
                .andExpect(jsonPath("$.errores", hasItem("El logotipo debe ser una imagen PNG, JPG o WEBP.")))
                .andExpect(jsonPath("$.errores", hasItem("Ingresa el nombre completo de la institución.")));
        put_("/api/institucion", datos(), docente(ELENA)).andExpect(status().isForbidden());
    }

    @Test
    void crudDeFases() throws Exception {
        String admin = admin();
        get_("/api/fases", docente(ELENA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].titulo").value("Exploración"))
                .andExpect(jsonPath("$[3].estado").value("En curso"));
        Map<String, Object> fase = Map.of("orden", 9, "semana", "09", "titulo", "Mantenimiento", "actividad", "Soporte",
                "producto", "Manual", "estado", "Pendiente");
        long id = leer(post_("/api/fases", fase, admin).andExpect(status().isCreated())).get("id").asLong();
        post_("/api/fases", fase, admin).andExpect(status().isConflict());
        put_("/api/fases/" + id, Map.of("orden", 9, "semana", "09", "titulo", "Mantenimiento", "actividad", "Soporte",
                "producto", "Manual", "estado", "en curso"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("En curso"));
        delete_("/api/fases/" + id, admin).andExpect(status().isNoContent());
    }
}

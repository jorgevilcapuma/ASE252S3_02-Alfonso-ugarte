package pe.edu.vallegrande.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base de las pruebas de la API: contexto completo sobre H2 (modo SQL Server) con los datos maestros y de demostración,
 * reloj fijo en el martes 13/10/2026 (America/Lima) y cada prueba revertida al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(PruebaApi.RelojFijo.class)
public abstract class PruebaApi {

    protected static final LocalDate HOY = LocalDate.of(2026, 10, 13);          // martes
    protected static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    protected static final LocalDate VIERNES = LocalDate.of(2026, 10, 9);
    protected static final LocalDate SABADO = LocalDate.of(2026, 10, 10);

    protected static final String ADMIN = "admin@ie20191.edu.pe";
    protected static final String ELENA = "elena.salazar@ie20191.edu.pe";
    protected static final String LUIS = "luis.mendoza@ie20191.edu.pe";
    protected static final String CARLOS = "carlos.mendoza@ie20191.edu.pe";
    protected static final String CLAVE = "123456";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), ZoneId.of("America/Lima"));
        }
    }

    // ---------- Sesión ----------

    protected JsonNode ingresar(String perfil, String correo, String contrasena) throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("perfil", perfil, "correo", correo, "contrasena", contrasena));
        return leer(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk()));
    }

    protected String token(String perfil, String correo) throws Exception {
        return ingresar(perfil, correo, CLAVE).get("token").asText();
    }

    protected String admin() throws Exception {
        return token("administrador", ADMIN);
    }

    protected String docente(String correo) throws Exception {
        return token("docente", correo);
    }

    /** Ingreso del alumno con nombre y DNI (como el formulario del portal). */
    protected JsonNode ingresarAlumno(String nombre, String dni) throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("perfil", "alumno", "nombre", nombre, "dni", dni));
        return leer(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk()));
    }

    // ---------- Peticiones ----------

    protected ResultActions get_(String url, String token) throws Exception {
        return mvc.perform(conToken(get(url), token));
    }

    protected ResultActions post_(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(conCuerpo(post(url), cuerpo, token));
    }

    protected ResultActions put_(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(conCuerpo(put(url), cuerpo, token));
    }

    protected ResultActions patch_(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(conCuerpo(patch(url), cuerpo, token));
    }

    protected ResultActions delete_(String url, String token) throws Exception {
        return mvc.perform(conToken(delete(url), token));
    }

    protected JsonNode leer(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return token == null ? peticion : peticion.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, Object cuerpo, String token) throws Exception {
        MockHttpServletRequestBuilder p = conToken(peticion, token).contentType(MediaType.APPLICATION_JSON);
        return cuerpo == null ? p : p.content(cuerpo instanceof String s ? s : json.writeValueAsString(cuerpo));
    }

    // ---------- Datos sembrados ----------

    protected long seccionId(String nivel, String grado, String letra, String token) throws Exception {
        for (JsonNode s : leer(get_("/api/secciones?nivel=" + nivel + "&grado=" + grado, token).andExpect(status().isOk()))) {
            if (s.get("letra").asText().equals(letra)) {
                return s.get("id").asLong();
            }
        }
        throw new IllegalStateException("No existe la sección " + nivel + " " + grado + " " + letra);
    }

    protected long asignacionId(String codigo, String token) throws Exception {
        for (JsonNode a : leer(get_("/api/asignaciones", token).andExpect(status().isOk()))) {
            if (a.get("codigo").asText().equals(codigo)) {
                return a.get("id").asLong();
            }
        }
        throw new IllegalStateException("No existe el curso " + codigo);
    }

    /** Estudiantes activos de la sección, en orden alfabético. */
    protected JsonNode estudiantesDe(long seccionId, String token) throws Exception {
        return leer(get_("/api/secciones/" + seccionId + "/estudiantes", token).andExpect(status().isOk()));
    }
}

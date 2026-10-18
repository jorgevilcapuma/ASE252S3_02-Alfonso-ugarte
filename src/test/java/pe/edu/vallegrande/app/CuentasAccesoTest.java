package pe.edu.vallegrande.app;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

/** Docentes: el correo de la ficha es su cuenta (se crea o vincula al registrarlos). Alumnos: solo nombre y DNI. */
class CuentasAccesoTest extends PruebaApi {

    private static Map<String, Object> docente(String dni, String correo, String cargo) {
        Map<String, Object> d = new HashMap<>();
        d.put("nombres", "Rosa María");
        d.put("apellidos", "Quispe Torres");
        d.put("dni", dni);
        d.put("cargo", cargo);
        d.put("sexo", "F");
        d.put("correo", correo);
        return d;
    }

    private static Map<String, Object> estudiante(String dni, long aula, String correo) {
        Map<String, Object> e = new HashMap<>();
        e.put("nombres", "Ana Lucía");
        e.put("apellidos", "Torres Vega");
        e.put("dni", dni);
        e.put("gradoSeccionId", aula);
        e.put("correo", correo);
        return e;
    }

    private int ingresoStatus(String perfil, String correo) throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("perfil", perfil, "correo", correo, "contrasena", CLAVE));
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn().getResponse().getStatus();
    }

    @Test
    void registrarUnDocenteConCorreoCreaSuCuentaYYaPuedeIngresar() throws Exception {
        String admin = admin();
        JsonNode creado = leer(post_("/api/docentes", docente("45670001", "Rosa.Quispe@IE20191.edu.pe", "Docente"), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("rosa.quispe@ie20191.edu.pe")));
        long usuarioId = creado.get("usuarioId").asLong();
        get_("/api/usuarios/" + usuarioId, admin).andExpect(jsonPath("$.perfil").value("docente"))
                .andExpect(jsonPath("$.nombre").value("Rosa María Quispe Torres"))
                .andExpect(jsonPath("$.docenteId").value(creado.get("id").asLong()));

        JsonNode sesion = ingresar("docente", "rosa.quispe@ie20191.edu.pe", CLAVE);
        org.assertj.core.api.Assertions.assertThat(sesion.get("sesion").get("docenteId").asLong()).isEqualTo(creado.get("id").asLong());
    }

    @Test
    void editarLaFichaActualizaLaCuentaYEliminarlaLaDesactiva() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/docentes", docente("45670002", "rosa.q@ie20191.edu.pe", "Docente"), admin)).get("id").asLong();

        // Sin correo en el formulario se conserva la cuenta (no se desvincula).
        Map<String, Object> sinCorreo = docente("45670002", null, "Docente");
        put_("/api/docentes/" + id, sinCorreo, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("rosa.q@ie20191.edu.pe"))
                .andExpect(jsonPath("$.usuarioId").isNumber());

        // Cambiar el correo cambia el de ingreso.
        put_("/api/docentes/" + id, docente("45670002", "rosa.quispe.t@ie20191.edu.pe", "Docente"), admin).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(ingresoStatus("docente", "rosa.q@ie20191.edu.pe")).isEqualTo(401);
        ingresar("docente", "rosa.quispe.t@ie20191.edu.pe", CLAVE);

        patch_("/api/docentes/" + id + "/eliminar", null, admin).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(ingresoStatus("docente", "rosa.quispe.t@ie20191.edu.pe")).isEqualTo(403);
        patch_("/api/docentes/" + id + "/restaurar", null, admin).andExpect(status().isOk());
        ingresar("docente", "rosa.quispe.t@ie20191.edu.pe", CLAVE);
    }

    @Test
    void reutilizaUnaCuentaLibreConEseCorreoYRechazaCorreosAjenos() throws Exception {
        String admin = admin();
        long cuenta = leer(post_("/api/usuarios", Map.of("nombre", "Rosa Quispe", "correo", "rosa.libre@ie20191.edu.pe", "perfil", "docente"), admin))
                .get("id").asLong();
        post_("/api/docentes", docente("45670003", "rosa.libre@ie20191.edu.pe", "Docente"), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(cuenta));

        // El correo de la cuenta de una alumna no sirve para un docente, ni el de una cuenta ya vinculada.
        post_("/api/usuarios", Map.of("nombre", "Alumna Prueba", "correo", "alumna.x@ie20191.edu.pe", "perfil", "alumno"), admin);
        post_("/api/docentes", docente("45670004", "alumna.x@ie20191.edu.pe", "Docente"), admin).andExpect(status().isConflict());
        post_("/api/docentes", docente("45670005", "elena.salazar@ie20191.edu.pe", "Docente"), admin).andExpect(status().isConflict());
    }

    @Test
    void otrosCargosNoRecibenCuentaAutomatica() throws Exception {
        post_("/api/docentes", docente("45670006", "aux@ie20191.edu.pe", "Auxiliar"), admin()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("aux@ie20191.edu.pe"))
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @Test
    void elEstudianteSeRegistraSinCuentaEIngresaSoloConNombreYDni() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "2", "A", admin);
        post_("/api/estudiantes", estudiante("60000101", aula, "ana.torres.v@gmail.com"), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana.torres.v@gmail.com"))
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
        ingresarAlumno("Ana Lucía Torres Vega", "60000101");
        // Con correo y contraseña no se ingresa como alumno.
        post_("/api/auth/login", Map.of("perfil", "alumno", "correo", "ana.torres.v@gmail.com", "contrasena", CLAVE), null)
                .andExpect(status().isBadRequest());
    }
}

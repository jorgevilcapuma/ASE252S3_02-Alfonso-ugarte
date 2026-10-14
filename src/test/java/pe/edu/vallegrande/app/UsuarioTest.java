package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Gestión de usuarios (cuentas institucionales). */
class UsuarioTest extends PruebaApi {

    private long usuarioId(String correo, String admin) throws Exception {
        return leer(get_("/api/usuarios?q=" + correo, admin)).get(0).get("id").asLong();
    }

    @Test
    void listaYFiltraLasCuentasSembradas() throws Exception {
        String admin = admin();
        get_("/api/usuarios", admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(9));
        get_("/api/usuarios?perfil=docente", admin).andExpect(jsonPath("$.length()").value(6));
        get_("/api/usuarios?estado=Inactivo", admin).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].correo").value("juan.alvarado@ie20191.edu.pe"));
        get_("/api/usuarios?q=ROJAS", admin).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].docenteId").isNumber());
        get_("/api/usuarios?perfil=director", admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("Valores permitidos: administrador, docente, alumno")));
    }

    @Test
    void creaCuentaConContrasenaInicialYPuedeIngresar() throws Exception {
        String admin = admin();
        post_("/api/usuarios", Map.of("nombre", "  Pedro   Ruiz Soto ", "correo", "Pedro.Ruiz@IE20191.edu.pe", "perfil", "docente"), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Pedro Ruiz Soto"))
                .andExpect(jsonPath("$.correo").value("pedro.ruiz@ie20191.edu.pe"))
                .andExpect(jsonPath("$.estado").value("Activo"))
                .andExpect(jsonPath("$.creado").value(HOY.toString()))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
        ingresar("docente", "pedro.ruiz@ie20191.edu.pe", CLAVE);
    }

    @Test
    void evitaCorreosDuplicadosYDatosInvalidos() throws Exception {
        String admin = admin();
        post_("/api/usuarios", Map.of("nombre", "Otra Persona", "correo", "ELENA.SALAZAR@ie20191.edu.pe", "perfil", "docente"), admin)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("Ese correo ya está registrado."));
        post_("/api/usuarios", Map.of("nombre", "Pedro", "correo", "pedro@ie20191.edu.pe", "perfil", "docente"), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("Ingresa nombres y apellidos."));
        post_("/api/usuarios", Map.of("nombre", "Pedro Ruiz", "correo", "pedro@", "perfil", "docente", "contrasena", "123"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2));
    }

    @Test
    void desactivarBloqueaElIngresoYReactivarLoPermite() throws Exception {
        String admin = admin();
        long luis = usuarioId(LUIS, admin);
        patch_("/api/usuarios/" + luis + "/estado", null, admin).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Inactivo"));
        post_("/api/auth/login", Map.of("perfil", "docente", "correo", LUIS, "contrasena", CLAVE), null).andExpect(status().isForbidden());
        patch_("/api/usuarios/" + luis + "/estado", null, admin).andExpect(jsonPath("$.estado").value("Activo"));
        ingresar("docente", LUIS, CLAVE);
    }

    @Test
    void protegeAlUnicoAdministrador() throws Exception {
        String admin = admin();
        long id = usuarioId(ADMIN, admin);
        patch_("/api/usuarios/" + id + "/estado", null, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("tu propia cuenta")));
        delete_("/api/usuarios/" + id, admin).andExpect(status().isConflict());
    }

    @Test
    void actualizaDatosYContrasena() throws Exception {
        String admin = admin();
        long luis = usuarioId(LUIS, admin);
        Map<String, Object> cuerpo = new HashMap<>(Map.of("nombre", "Luis Alberto Mendoza Castro", "correo", LUIS, "perfil", "docente",
                "estado", "Activo", "contrasena", "nueva-clave"));
        put_("/api/usuarios/" + luis, cuerpo, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis Alberto Mendoza Castro"));
        ingresar("docente", LUIS, "nueva-clave");
    }

    @Test
    void cadaUsuarioCambiaSuPropiaContrasena() throws Exception {
        JsonNode sesion = ingresar("docente", LUIS, CLAVE);
        String token = sesion.get("token").asText();
        long id = sesion.get("sesion").get("usuarioId").asLong();
        put_("/api/usuarios/" + id + "/contrasena", Map.of("contrasenaActual", "equivocada", "contrasenaNueva", "otra-clave"), token)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta."));
        put_("/api/usuarios/" + id + "/contrasena", Map.of("contrasenaActual", CLAVE, "contrasenaNueva", "otra-clave"), token)
                .andExpect(status().isNoContent());
        ingresar("docente", LUIS, "otra-clave");
        put_("/api/usuarios/" + usuarioId(ELENA, admin()) + "/contrasena", Map.of("contrasenaNueva", "xxxxxx"), token)
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminaCuentasSinRegistrosYProtegeLasQueTienenHistorial() throws Exception {
        String admin = admin();
        long elena = usuarioId(ELENA, admin);
        delete_("/api/usuarios/" + elena, admin).andExpect(status().isNoContent());
        get_("/api/usuarios/" + elena, admin).andExpect(status().isNotFound());
        // La ficha docente se conserva, solo pierde la cuenta vinculada.
        get_("/api/docentes", admin).andExpect(jsonPath("$[?(@.nombreCompleto == 'Elena Salazar Ríos')].usuarioId").isEmpty());
        // Carmen Rojas es autora de un comunicado.
        delete_("/api/usuarios/" + usuarioId("carmen.rojas@ie20191.edu.pe", admin), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("Desactívala en lugar de eliminarla")));
    }
}

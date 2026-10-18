package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AutenticacionTest extends PruebaApi {

    @Test
    void administradorIngresaConCorreoYContrasena() throws Exception {
        post_("/api/auth/login", Map.of("perfil", "administrador", "correo", ADMIN, "contrasena", CLAVE), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.sesion.perfil").value("administrador"))
                .andExpect(jsonPath("$.sesion.nombre").value("Administrador General"))
                .andExpect(jsonPath("$.sesion.rutaInicial").value("/administrador/dashboard"));
    }

    @Test
    void docenteRecibeSuFichaEnLaSesion() throws Exception {
        var sesion = ingresar("docente", ELENA, CLAVE).get("sesion");
        org.assertj.core.api.Assertions.assertThat(sesion.get("docenteId").asLong()).isPositive();
        org.assertj.core.api.Assertions.assertThat(sesion.get("rutaInicial").asText()).isEqualTo("/docente/dashboard");
    }

    @Test
    void contrasenaIncorrectaDevuelve401() throws Exception {
        post_("/api/auth/login", Map.of("perfil", "administrador", "correo", ADMIN, "contrasena", "otra-clave"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    }

    @Test
    void validaFormatoAntesDeConsultar() throws Exception {
        post_("/api/auth/login", Map.of("perfil", "docente", "correo", "no-es-correo", "contrasena", CLAVE), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Ingresa un correo institucional válido."));
        post_("/api/auth/login", Map.of("perfil", "docente", "correo", ELENA, "contrasena", "123"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña debe tener 6 caracteres como mínimo."));
        post_("/api/auth/login", Map.of("correo", ELENA, "contrasena", CLAVE), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Selecciona tu perfil de acceso."));
    }

    @Test
    void cuentaDesactivadaNoIngresa() throws Exception {
        // Alumna con una cuenta vinculada: si la cuenta se desactiva, tampoco ingresa con su nombre y DNI.
        String admin = admin();
        long cuenta = leer(post_("/api/usuarios", Map.of("nombre", "Ana Torres Vega", "correo", "ana.tv@ie20191.edu.pe", "perfil", "alumno"), admin))
                .get("id").asLong();
        post_("/api/estudiantes", Map.of("nombres", "Ana", "apellidos", "Torres Vega", "dni", "60000077",
                "gradoSeccionId", gradoSeccionId("Primaria", "2", "A", admin), "usuarioId", cuenta), admin).andExpect(status().isCreated());
        patch_("/api/usuarios/" + cuenta + "/eliminar", null, admin).andExpect(status().isOk());
        post_("/api/auth/login", Map.of("perfil", "alumno", "nombre", "Ana Torres Vega", "dni", "60000077"), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("desactivada")));
    }

    @Test
    void elAlumnoSoloIngresaConNombreYDni() throws Exception {
        post_("/api/auth/login", Map.of("perfil", "alumno", "correo", "lucia.bustamante@ie20191.edu.pe", "contrasena", CLAVE), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Ingresa tu nombre completo con al menos un apellido."));
    }

    @Test
    void cuentaDeOtroPerfilNoIngresa() throws Exception {
        post_("/api/auth/login", Map.of("perfil", "administrador", "correo", ELENA, "contrasena", CLAVE), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Esta cuenta pertenece al perfil Docente."));
    }

    @Test
    void alumnoIngresaConNombreYDniSinImportarTildesNiMayusculas() throws Exception {
        post_("/api/auth/login", Map.of("perfil", "alumno", "nombre", "  juan perez   UGARTE ", "dni", "72819201"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesion.perfil").value("alumno"))
                .andExpect(jsonPath("$.sesion.nombre").value("Juan Pérez Ugarte"))
                .andExpect(jsonPath("$.sesion.estudianteId", notNullValue()))
                .andExpect(jsonPath("$.sesion.rutaInicial").value("/alumno/inicio"));
    }

    @Test
    void alumnoConDatosQueNoCoincidenNoIngresa() throws Exception {
        post_("/api/auth/login", Map.of("perfil", "alumno", "nombre", "Otro Nombre", "dni", "72819201"), null)
                .andExpect(status().isUnauthorized());
        post_("/api/auth/login", Map.of("perfil", "alumno", "nombre", "Juan Pérez Ugarte", "dni", "1234"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El DNI debe tener exactamente 8 dígitos."));
    }

    @Test
    void sinTokenOTokenAlteradoDevuelve401() throws Exception {
        get_("/api/usuarios", null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Inicia sesión para continuar."));
        String token = admin();
        get_("/api/usuarios", token.substring(0, token.length() - 2) + "xx").andExpect(status().isUnauthorized());
    }

    @Test
    void perfilSinPermisoDevuelve403() throws Exception {
        String alumno = ingresarAlumno("Juan Pérez Ugarte", "72819201").get("token").asText();
        get_("/api/usuarios", alumno).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Tu perfil no tiene acceso a esta operación."));
        get_("/api/usuarios", docente(ELENA)).andExpect(status().isForbidden());
    }

    @Test
    void sesionActual() throws Exception {
        get_("/api/auth/sesion", docente(LUIS)).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(LUIS))
                .andExpect(jsonPath("$.nombre").value("Luis Mendoza Castro"));
    }

    @Test
    void estudianteRetiradoNoIngresa() throws Exception {
        long juan = juanPerez();
        patch_("/api/estudiantes/" + juan + "/eliminar", null, admin()).andExpect(status().isOk());
        post_("/api/auth/login", Map.of("perfil", "alumno", "nombre", "Juan Pérez Ugarte", "dni", "72819201"), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("no está activo")));
    }

    @Test
    void rutaInexistenteDevuelve404Claro() throws Exception {
        get_("/api/no-existe", admin()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La ruta solicitada no existe."));
    }
}

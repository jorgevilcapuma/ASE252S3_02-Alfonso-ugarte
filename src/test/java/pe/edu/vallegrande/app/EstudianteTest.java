package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Estudiantes con su grado y sección; retiro y restauración lógicos. */
class EstudianteTest extends PruebaApi {

    private Map<String, Object> estudiante(String dni, long gradoSeccionId) {
        Map<String, Object> e = new HashMap<>();
        e.put("nombres", " Ana  Lucía ");
        e.put("apellidos", "Torres Vega");
        e.put("dni", dni);
        e.put("gradoSeccionId", gradoSeccionId);
        e.put("genero", "F");
        return e;
    }

    @Test
    void alRegistrarloSeVinculaSuCuentaDeAlumnoYPuedeIngresar() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "2", "A", admin);
        long cuenta = leer(post_("/api/usuarios", Map.of("nombre", "Ana Lucía Torres Vega", "correo", "ana.torres@ie20191.edu.pe",
                "perfil", "alumno"), admin).andExpect(status().isCreated())).get("id").asLong();
        long docenteCuenta = leer(post_("/api/usuarios", Map.of("nombre", "Otro Docente", "correo", "otro.docente@ie20191.edu.pe",
                "perfil", "docente"), admin)).get("id").asLong();

        Map<String, Object> conCuentaDocente = estudiante("60000009", aula);
        conCuentaDocente.put("usuarioId", docenteCuenta);
        post_("/api/estudiantes", conCuentaDocente, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La cuenta vinculada debe tener perfil alumno."));

        Map<String, Object> datos = estudiante("60000009", aula);
        datos.put("usuarioId", cuenta);
        post_("/api/estudiantes", datos, admin).andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(cuenta));
        // Con o sin cuenta vinculada, el alumno ingresa con su nombre completo y DNI.
        ingresarAlumno(datos.get("nombres") + " " + datos.get("apellidos"), "60000009");
    }

    @Test
    void listaPorAulaYBuscaPorTexto() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "1", "A", admin);
        get_("/api/estudiantes?gradoSeccionId=" + aula, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].apellidos").value("Alvarado Mendoza"))
                .andExpect(jsonPath("$[0].gradoEtiqueta").value("1ro grado"))
                .andExpect(jsonPath("$[0].nivel").value("Primaria"));
        get_("/api/estudiantes?q=72819201", admin).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Juan Pérez Ugarte"));
        get_("/api/estudiantes?q=ugarte", docente(ELENA)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        get_("/api/estudiantes?estado=Z", admin).andExpect(status().isBadRequest());
    }

    @Test
    void registraEditaYCambiaDeAula() throws Exception {
        String admin = admin();
        long primeroA = gradoSeccionId("Primaria", "1", "A", admin);
        long primeroB = gradoSeccionId("Primaria", "1", "B", admin);
        long id = leer(post_("/api/estudiantes", estudiante("60000001", primeroA), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ana Lucía Torres Vega"))
                .andExpect(jsonPath("$.seccion").value("A"))
                .andExpect(jsonPath("$.estado").value("A"))).get("id").asLong();

        put_("/api/estudiantes/" + id, estudiante("60000001", primeroB), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.gradoSeccionId").value(primeroB))
                .andExpect(jsonPath("$.seccion").value("B"));
        get_("/api/grados-secciones/" + primeroB, admin).andExpect(jsonPath("$.totalEstudiantes").value(13));

        post_("/api/estudiantes", estudiante("60000001", primeroA), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un estudiante con el DNI 60000001."));
        post_("/api/estudiantes", estudiante("60000002", 999999), admin).andExpect(status().isNotFound());
        Map<String, Object> malo = estudiante("12", primeroA);
        malo.remove("gradoSeccionId");
        post_("/api/estudiantes", malo, admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasItem("El DNI debe tener 8 dígitos.")))
                .andExpect(jsonPath("$.errores", hasItem("Selecciona el grado y sección del estudiante.")));
    }

    @Test
    void retiraYRestauraConservandoElHistorial() throws Exception {
        String admin = admin();
        long juan = juanPerez();
        put_("/api/asistencia-estudiantes/estudiantes/" + juan, Map.of("fecha", LUNES.toString(), "estado", "falta"), admin)
                .andExpect(status().isOk());

        patch_("/api/estudiantes/" + juan + "/eliminar", null, admin).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("I"));
        get_("/api/estudiantes?q=72819201", admin).andExpect(jsonPath("$.length()").value(0));
        get_("/api/estudiantes?q=72819201&estado=I", admin).andExpect(jsonPath("$.length()").value(1));
        get_("/api/estudiantes/" + juan + "/asistencia", admin).andExpect(jsonPath("$.length()").value(1));
        put_("/api/asistencia-estudiantes/estudiantes/" + juan, Map.of("fecha", HOY.toString(), "estado", "presente"), admin)
                .andExpect(status().isConflict());
        patch_("/api/estudiantes/" + juan + "/eliminar", null, admin).andExpect(status().isConflict());

        patch_("/api/estudiantes/" + juan + "/restaurar", null, admin).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("A"));
    }

    @Test
    void elAlumnoSoloVeYEditaSuContacto() throws Exception {
        var sesion = ingresarAlumno("Juan Pérez Ugarte", "72819201");
        String alumno = sesion.get("token").asText();
        long juan = sesion.get("sesion").get("estudianteId").asLong();
        String admin = admin();
        long otro = estudiantesDe(gradoSeccionId("Primaria", "1", "A", admin), admin).get(0).get("id").asLong();

        get_("/api/estudiantes/" + juan, alumno).andExpect(status().isOk());
        get_("/api/estudiantes/" + otro, alumno).andExpect(status().isForbidden());
        put_("/api/estudiantes/" + juan + "/contacto", Map.of("telefono", "987111222", "correo", "Juan@Correo.pe"), alumno)
                .andExpect(status().isOk()).andExpect(jsonPath("$.correo").value("juan@correo.pe"));
        put_("/api/estudiantes/" + juan, estudiante("72819201", 1), alumno).andExpect(status().isForbidden());
        get_("/api/estudiantes", alumno).andExpect(status().isForbidden());
    }
}

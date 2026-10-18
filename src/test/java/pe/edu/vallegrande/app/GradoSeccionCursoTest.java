package pe.edu.vallegrande.app;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Tablas maestras grados_secciones y cursos. */
class GradoSeccionCursoTest extends PruebaApi {

    @Test
    void listaLasAulasSembradasEnOrden() throws Exception {
        String admin = admin();
        get_("/api/grados-secciones", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(42))
                .andExpect(jsonPath("$[0].nombre").value("3 años A - Inicial"))
                .andExpect(jsonPath("$[41].nombre").value("5to año C - Secundaria"));
        get_("/api/grados-secciones?nivel=primaria&grado=1", docente(ELENA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].totalEstudiantes").value(13))
                .andExpect(jsonPath("$[2].turno").value("Tarde"));
        get_("/api/grados-secciones?nivel=Universidad", admin).andExpect(status().isBadRequest());
    }

    @Test
    void creaEditaYEliminaUnAula() throws Exception {
        String admin = admin();
        long id = leer(post_("/api/grados-secciones", Map.of("nivel", "Primaria", "grado", "1", "seccion", "d"), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seccion").value("D"))
                .andExpect(jsonPath("$.turno").value("Mañana"))
                .andExpect(jsonPath("$.totalEstudiantes").value(0))).get("id").asLong();
        post_("/api/grados-secciones", Map.of("nivel", "Primaria", "grado", "1", "seccion", "D"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe 1ro grado D - Primaria."));
        put_("/api/grados-secciones/" + id, Map.of("nivel", "Primaria", "grado", "1", "seccion", "D", "turno", "Tarde"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.turno").value("Tarde"));
        patch_("/api/grados-secciones/" + id + "/eliminar", null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("I"));
        patch_("/api/grados-secciones/" + id + "/eliminar", null, admin).andExpect(status().isConflict());
        get_("/api/grados-secciones?estado=I", admin).andExpect(jsonPath("$.length()").value(1));
        get_("/api/grados-secciones?estado=A&nivel=Primaria&grado=1", admin).andExpect(jsonPath("$[*].seccion", not(hasItem("D"))));
        put_("/api/grados-secciones/" + id, Map.of("nivel", "Primaria", "grado", "1", "seccion", "D"), admin).andExpect(status().isConflict());
        patch_("/api/grados-secciones/" + id + "/restaurar", null, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("A"));
        get_("/api/grados-secciones?estado=X", admin).andExpect(status().isBadRequest());

        long conAlumnos = gradoSeccionId("Primaria", "2", "A", admin);
        patch_("/api/grados-secciones/" + conAlumnos + "/eliminar", null, admin).andExpect(status().isConflict());
        post_("/api/grados-secciones", Map.of("nivel", "Primaria", "grado", "1", "seccion", "E"), docente(ELENA)).andExpect(status().isForbidden());
    }

    @Test
    void cursosPorAulaConSuDocente() throws Exception {
        String admin = admin();
        long primeroA = gradoSeccionId("Primaria", "1", "A", admin);
        get_("/api/cursos?gradoSeccionId=" + primeroA, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));

        long luis = ingresar("docente", LUIS, CLAVE).get("sesion").get("docenteId").asLong();
        long segundoA = gradoSeccionId("Primaria", "2", "A", admin);
        Map<String, Object> curso = new HashMap<>(Map.of("nombre", "  Matemática ", "gradoSeccionId", segundoA, "docenteId", luis));
        long id = leer(post_("/api/cursos", curso, admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Matemática"))
                .andExpect(jsonPath("$.aula").value("2do grado A - Primaria"))
                .andExpect(jsonPath("$.docenteNombre").value("Luis Mendoza Castro"))).get("id").asLong();
        post_("/api/cursos", curso, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El curso Matemática ya existe en esa aula."));
        get_("/api/cursos?docenteId=" + luis, admin).andExpect(jsonPath("$.length()").value(2));

        curso.put("nombre", "Razonamiento Matemático");
        put_("/api/cursos/" + id, curso, admin).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Razonamiento Matemático"));
        patch_("/api/cursos/" + id + "/eliminar", null, admin).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("I"));
        get_("/api/cursos?estado=A&docenteId=" + luis, admin).andExpect(jsonPath("$.length()").value(1));
        get_("/api/cursos?estado=I", admin).andExpect(jsonPath("$.length()").value(1));
        put_("/api/cursos/" + id, curso, admin).andExpect(status().isConflict());
        patch_("/api/cursos/" + id + "/restaurar", null, admin).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("A"));
        patch_("/api/cursos/" + id + "/restaurar", null, admin).andExpect(status().isConflict());
    }

    @Test
    void soloPersonalActivoConCargoDocenteDictaCursos() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "2", "A", admin);
        JsonNode administrativo = leer(get_("/api/docentes?cargo=Administrativo", admin)).get(0);
        post_("/api/cursos", Map.of("nombre", "Tutoría", "gradoSeccionId", aula, "docenteId", administrativo.get("id").asLong()), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Solo el personal con cargo Docente puede dictar cursos."));
        post_("/api/cursos", Map.of("nombre", "Tutoría", "gradoSeccionId", aula), admin).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Selecciona el docente del curso."));
    }

    @Test
    void cursoConAsistenciaSeEliminaLogicamenteYYaNoRecibeAsistencia() throws Exception {
        String admin = admin();
        long aula = gradoSeccionId("Primaria", "1", "A", admin);
        long mat = cursoId("Matemática", aula, admin);
        long juan = juanPerez();
        put_("/api/asistencia-estudiantes/estudiantes/" + juan, Map.of("fecha", LUNES.toString(), "cursoId", mat, "estado", "presente"), admin)
                .andExpect(status().isOk());
        patch_("/api/cursos/" + mat + "/eliminar", null, admin).andExpect(status().isOk());
        put_("/api/asistencia-estudiantes/estudiantes/" + juan, Map.of("fecha", LUNES.toString(), "cursoId", mat, "estado", "falta"), admin)
                .andExpect(status().isConflict());
        patch_("/api/cursos/" + mat + "/restaurar", null, admin).andExpect(status().isOk());
    }

    @Test
    void elAlumnoSoloVeLosCursosDeSuAula() throws Exception {
        String admin = admin();
        long primeroA = gradoSeccionId("Primaria", "1", "A", admin);
        String alumno = ingresarAlumno("Juan Pérez Ugarte", "72819201").get("token").asText();
        long segundoA = gradoSeccionId("Primaria", "2", "A", admin);
        // Aunque pida otra aula, recibe solo la suya.
        get_("/api/cursos?gradoSeccionId=" + segundoA, alumno).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[*].gradoSeccionId", everyItem(is((int) primeroA))));
        post_("/api/cursos", Map.of("nombre", "X", "gradoSeccionId", primeroA, "docenteId", 1), alumno).andExpect(status().isForbidden());
    }
}

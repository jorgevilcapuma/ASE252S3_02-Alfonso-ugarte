package pe.edu.vallegrande.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.edu.vallegrande.app.config.PropiedadesApp;
import pe.edu.vallegrande.app.dto.response.AsignacionResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Grado;
import pe.edu.vallegrande.app.model.Horario;
import pe.edu.vallegrande.app.model.NivelEducativo;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.enums.DiaSemana;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.model.enums.Turno;
import pe.edu.vallegrande.app.seguridad.HasherContrasena;
import pe.edu.vallegrande.app.seguridad.SesionUsuario;
import pe.edu.vallegrande.app.seguridad.TokenService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

/** Pruebas unitarias de las piezas sin base de datos. */
class UtilidadesTest {

    @Test
    void separaNombresYApellidosComoElFormulario() {
        assertThat(Textos.separarNombreCompleto("Juan Pérez Ugarte")).containsExactly("Juan", "Pérez Ugarte");
        assertThat(Textos.separarNombreCompleto("Ana Lucía Torres Vega")).containsExactly("Ana Lucía", "Torres Vega");
        assertThat(Textos.separarNombreCompleto("Diana Quispe")).containsExactly("Diana", "Quispe");
        assertThat(Textos.separarNombreCompleto("Diana")).isNull();
        assertThat(Textos.clave("  JUAN  Pérez ")).isEqualTo(Textos.clave("juan perez"));
    }

    @Test
    void catalogosAceptanElTextoDelFrontend() {
        assertThat(EstadoAsistencia.de("Presente")).isEqualTo(EstadoAsistencia.PRESENTE);
        assertThat(Turno.de("manana")).isEqualTo(Turno.MANANA);
        assertThat(DiaSemana.de("miercoles")).isEqualTo(DiaSemana.MIERCOLES);
        assertThatThrownBy(() -> Perfil.de("director")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valores permitidos: administrador, docente, alumno");
    }

    @Test
    void diasHabilesSaltanFinesDeSemana() {
        assertThat(Calendario.diasHabiles(3, LocalDate.of(2026, 10, 13)))
                .containsExactly(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13));
        assertThat(Calendario.diasHabiles(20, LocalDate.of(2026, 10, 13)).get(0)).isEqualTo(LocalDate.of(2026, 9, 16));
    }

    @Test
    void horarioEnTextoAgrupaDiasConLasMismasHoras() {
        NivelEducativo nivel = new NivelEducativo("Primaria", 2);
        Asignacion a = new Asignacion();
        a.setCodigo("MAT-3A");
        a.setCurso(new Curso("Matemática"));
        a.setSeccion(new Seccion(new Grado(nivel, "3", "3ro grado", 3), "A", Turno.MANANA));
        Docente d = new Docente();
        d.setNombres("Ana");
        d.setApellidos("Castillo");
        a.setDocente(d);
        a.reemplazarHorarios(List.of(
                new Horario(DiaSemana.MIERCOLES, LocalTime.of(8, 0), LocalTime.of(9, 30)),
                new Horario(DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30)),
                new Horario(DiaSemana.VIERNES, LocalTime.of(11, 15), LocalTime.of(13, 0))));
        assertThat(AsignacionResponse.de(a, 0).horario()).isEqualTo("Lun - Mié 08:00 - 09:30 · Viernes 11:15 - 13:00");
    }

    @Test
    void contrasenasConSalYTokensFirmadosQueExpiran() {
        HasherContrasena hasher = new HasherContrasena();
        String hash = hasher.generar("123456");
        assertThat(hash).startsWith("pbkdf2$").isNotEqualTo(hasher.generar("123456"));
        assertThat(hasher.coincide("123456", hash)).isTrue();
        assertThat(hasher.coincide("1234567", hash)).isFalse();

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        PropiedadesApp propiedades = new PropiedadesApp("America/Lima", new String[0],
                new PropiedadesApp.Seguridad("secreto-de-prueba", 1, "123456"), null, null);
        Instant inicio = Instant.parse("2026-10-13T15:00:00Z");
        TokenService emisor = new TokenService(mapper, Clock.fixed(inicio, ZoneId.of("UTC")), propiedades);
        String token = emisor.generar(new SesionUsuario(1L, "a@b.pe", "A B", Perfil.DOCENTE, null, 7L, null, emisor.calcularExpiracion()));

        assertThat(emisor.validar(token).docenteId()).isEqualTo(7L);
        TokenService dosHorasDespues = new TokenService(mapper, Clock.fixed(inicio.plusSeconds(7200), ZoneId.of("UTC")), propiedades);
        assertThatThrownBy(() -> dosHorasDespues.validar(token)).isInstanceOf(ApiException.class).hasMessageContaining("expiró");
        TokenService otroSecreto = new TokenService(mapper, Clock.fixed(inicio, ZoneId.of("UTC")),
                new PropiedadesApp("America/Lima", new String[0], new PropiedadesApp.Seguridad("otro", 1, "123456"), null, null));
        assertThatThrownBy(() -> otroSecreto.validar(token)).isInstanceOf(ApiException.class);
    }
}

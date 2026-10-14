package pe.edu.vallegrande.app.dto.response;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.Horario;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.enums.DiaSemana;

/**
 * Curso de una sección con su docente y horario. Sirve a la vista del docente ("Mis cursos":
 * código, nombre, nivel, grado, sección, horario en texto, aula, total de alumnos) y a la del alumno
 * (docente y bloques de horario).
 */
public record AsignacionResponse(
        Long id,
        String codigo,
        Long cursoId,
        String nombre,
        String nivel,
        String grado,
        String gradoEtiqueta,
        String seccion,
        Long seccionId,
        Long docenteId,
        String docente,
        String aula,
        String horario,
        List<BloqueHorario> horarios,
        long totalAlumnos) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public record BloqueHorario(DiaSemana dia, String inicio, String fin) {
    }

    public static AsignacionResponse de(Asignacion a, long totalAlumnos) {
        Seccion s = a.getSeccion();
        List<Horario> ordenados = a.getHorarios().stream()
                .sorted(Comparator.comparing((Horario h) -> h.getDia().ordinal()).thenComparing(Horario::getHoraInicio))
                .toList();
        List<BloqueHorario> bloques = ordenados.stream()
                .map(h -> new BloqueHorario(h.getDia(), h.getHoraInicio().format(HORA), h.getHoraFin().format(HORA)))
                .toList();
        return new AsignacionResponse(a.getId(), a.getCodigo(), a.getCurso().getId(), a.getCurso().getNombre(),
                s.getNivelNombre(), s.getGrado().getCodigo(), s.getGrado().getEtiqueta(), s.getLetra(), s.getId(),
                a.getDocente().getId(), "Prof. " + a.getDocente().getNombreCompleto(), a.getAula(),
                horarioEnTexto(ordenados), bloques, totalAlumnos);
    }

    /** "Lun - Mié 08:00 - 09:30" agrupando los días que comparten horas; "Viernes 11:15 - 13:00" si es un solo día. */
    static String horarioEnTexto(List<Horario> horarios) {
        if (horarios.isEmpty()) {
            return "Sin horario";
        }
        Map<String, List<DiaSemana>> porHoras = new LinkedHashMap<>();
        for (Horario h : horarios) {
            porHoras.computeIfAbsent(rango(h.getHoraInicio(), h.getHoraFin()), clave -> new java.util.ArrayList<>()).add(h.getDia());
        }
        return porHoras.entrySet().stream()
                .map(entrada -> {
                    List<DiaSemana> dias = entrada.getValue();
                    String textoDias = dias.size() == 1 ? dias.get(0).getValor()
                            : dias.stream().map(DiaSemana::getAbreviatura).collect(Collectors.joining(" - "));
                    return textoDias + " " + entrada.getKey();
                })
                .collect(Collectors.joining(" · "));
    }

    private static String rango(LocalTime inicio, LocalTime fin) {
        return inicio.format(HORA) + " - " + fin.format(HORA);
    }
}

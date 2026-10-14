package pe.edu.vallegrande.app.util;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.exception.ApiException;

/** Fechas del calendario escolar (días hábiles de lunes a viernes) según el reloj del colegio. */
@Component
@RequiredArgsConstructor
public class Calendario {

    private final Clock reloj;

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj).withNano(0);
    }

    public int anioActual() {
        return hoy().getYear();
    }

    public static boolean esFinDeSemana(LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /** Los últimos {@code cantidad} días hábiles hasta {@code hasta} (incluido si es hábil), del más antiguo al más reciente. */
    public static List<LocalDate> diasHabiles(int cantidad, LocalDate hasta) {
        List<LocalDate> dias = new ArrayList<>();
        LocalDate cursor = hasta;
        while (dias.size() < cantidad) {
            if (!esFinDeSemana(cursor)) {
                dias.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }
        Collections.reverse(dias);
        return dias;
    }

    public static List<LocalDate> diasDelMes(YearMonth mes) {
        return mes.atDay(1).datesUntil(mes.atEndOfMonth().plusDays(1)).toList();
    }

    /** Solo se registra asistencia de días hábiles que no sean futuros. */
    public void validarFechaRegistrable(LocalDate fecha) {
        if (fecha.isAfter(hoy())) {
            throw ApiException.validacion("No se puede registrar asistencia de una fecha futura.");
        }
        if (esFinDeSemana(fecha)) {
            throw ApiException.validacion("No se registra asistencia en sábado ni domingo.");
        }
    }

    public static YearMonth mes(String texto) {
        try {
            return YearMonth.parse(texto);
        } catch (RuntimeException ex) {
            throw ApiException.validacion("El mes debe tener el formato AAAA-MM.");
        }
    }
}

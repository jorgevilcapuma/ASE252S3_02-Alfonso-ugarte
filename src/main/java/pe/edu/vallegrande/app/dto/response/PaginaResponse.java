package pe.edu.vallegrande.app.dto.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Página de resultados (la numeración empieza en 1, como en el frontend). */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> mapeo) {
        return new PaginaResponse<>(pagina.getContent().stream().map(mapeo).toList(), pagina.getNumber() + 1,
                pagina.getSize(), pagina.getTotalElements(), Math.max(1, pagina.getTotalPages()));
    }
}

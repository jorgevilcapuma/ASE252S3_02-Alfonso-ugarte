package pe.edu.vallegrande.app.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.app.dto.request.InstitucionRequest;
import pe.edu.vallegrande.app.dto.response.InstitucionResponse;
import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Institucion;
import pe.edu.vallegrande.app.repository.InstitucionRepository;
import pe.edu.vallegrande.app.service.InstitucionService;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

@Service
@RequiredArgsConstructor
public class InstitucionServiceImpl implements InstitucionService {

    public static final String COLOR_PRINCIPAL = "#9b1b1f";
    public static final String COLOR_SECUNDARIO = "#fdc425";

    private final InstitucionRepository repositorio;
    private final Calendario calendario;

    @Override
    @Transactional(readOnly = true)
    public InstitucionResponse obtener() {
        return InstitucionResponse.de(buscar());
    }

    @Override
    @Transactional
    public InstitucionResponse actualizar(InstitucionRequest request) {
        Institucion institucion = buscar();
        institucion.setNombre(Textos.limpiar(request.nombre()));
        institucion.setCodigoModular(request.codigoModular().trim());
        institucion.setDireccion(Textos.limpiar(request.direccion()));
        institucion.setColorPrincipal(request.colorPrincipal().toLowerCase());
        institucion.setColorSecundario(request.colorSecundario().toLowerCase());
        institucion.setLogo(request.logo() == null || request.logo().isBlank() ? null : request.logo());
        institucion.setActualizadoEn(calendario.ahora());
        return InstitucionResponse.de(institucion);
    }

    /** "Restablecer identidad": colores del escudo y logotipo por defecto. */
    @Override
    @Transactional
    public InstitucionResponse restablecerIdentidad() {
        Institucion institucion = buscar();
        institucion.setColorPrincipal(COLOR_PRINCIPAL);
        institucion.setColorSecundario(COLOR_SECUNDARIO);
        institucion.setLogo(null);
        institucion.setActualizadoEn(calendario.ahora());
        return InstitucionResponse.de(institucion);
    }

    private Institucion buscar() {
        return repositorio.findFirstByOrderByIdAsc()
                .orElseThrow(() -> ApiException.noEncontrado("La institución aún no está configurada."));
    }
}

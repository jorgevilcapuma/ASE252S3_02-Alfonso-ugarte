package pe.edu.vallegrande.app.service.impl;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.vallegrande.app.exception.ApiException;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.repository.DocenteRepository;

/**
 * Pone al día, al iniciar, los docentes registrados antes de que el correo de la ficha fuera el de la cuenta:
 * completa el correo de las fichas vinculadas y crea o vincula la cuenta de los docentes activos que tienen correo
 * pero no cuenta. Lo que no se puede reparar (p. ej. un correo usado por otra cuenta) solo se informa.
 * Los estudiantes no necesitan cuenta: ingresan con su nombre completo y DNI.
 */
@Component
public class ReparacionCuentas {

    private static final Logger log = LoggerFactory.getLogger(ReparacionCuentas.class);

    private final DocenteRepository docentes;
    private final CuentasAcceso cuentas;

    ReparacionCuentas(DocenteRepository docentes, CuentasAcceso cuentas) {
        this.docentes = docentes;
        this.cuentas = cuentas;
    }

    @Transactional
    public void reparar() {
        int corregidas = 0;
        for (Docente d : docentes.findAll()) {
            if (d.getUsuario() != null) {
                if (!Objects.equals(d.getCorreo(), d.getUsuario().getCorreo())) {
                    d.setCorreo(d.getUsuario().getCorreo());
                    corregidas++;
                }
            } else if (d.esActivo() && d.esDocente() && d.getCorreo() != null) {
                try {
                    Usuario cuenta = cuentas.sincronizar(null, d.getNombres() + " " + d.getApellidos(), d.getCorreo(), Perfil.DOCENTE,
                            Set.of(Perfil.DOCENTE, Perfil.ADMINISTRADOR), id -> docentes.findByUsuarioId(id).isPresent());
                    d.setUsuario(cuenta);
                    corregidas++;
                } catch (ApiException ex) {
                    log.warn("No se pudo crear la cuenta del docente {} ({}): {}", d.getNombreCompleto(), d.getCorreo(), ex.getMessage());
                }
            }
        }
        if (corregidas > 0) {
            log.info("Cuentas de acceso puestas al día: {} fichas de docentes.", corregidas);
        }
    }
}

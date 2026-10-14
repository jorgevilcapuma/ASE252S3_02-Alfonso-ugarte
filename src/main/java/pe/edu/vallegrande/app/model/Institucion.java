package pe.edu.vallegrande.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos del colegio e identidad visual del portal (una sola fila). */
@Entity
@Table(name = "institucion", uniqueConstraints = @UniqueConstraint(name = "uq_institucion_codigo_modular", columnNames = "codigo_modular"))
@Getter
@Setter
@NoArgsConstructor
public class Institucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "codigo_modular", nullable = false, length = 7)
    private String codigoModular;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(name = "color_principal", nullable = false, length = 7)
    private String colorPrincipal;

    @Column(name = "color_secundario", nullable = false, length = 7)
    private String colorSecundario;

    /** Logotipo como data URL (PNG reducido por el frontend); vacío usa el escudo por defecto. */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String logo;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}

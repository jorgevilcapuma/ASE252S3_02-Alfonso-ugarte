package pe.edu.vallegrande.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aviso enviado a la familia desde "Alertas tempranas" (historial completo, no solo el último). */
@Entity
@Table(name = "aviso_familia", indexes = @Index(name = "ix_aviso_estudiante_fecha", columnList = "estudiante_id, enviado_en"))
@Getter
@Setter
@NoArgsConstructor
public class AvisoFamilia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aviso_estudiante"))
    private Estudiante estudiante;

    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn;

    /** Faltas acumuladas en la ventana de alertas al momento del aviso. */
    @Column(nullable = false)
    private Integer faltas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enviado_por", foreignKey = @ForeignKey(name = "fk_aviso_usuario"))
    private Usuario enviadoPor;
}

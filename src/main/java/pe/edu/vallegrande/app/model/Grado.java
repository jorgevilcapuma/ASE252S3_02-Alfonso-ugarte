package pe.edu.vallegrande.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla maestra de grados por nivel. El código es el valor que usa el frontend
 * ("3 años" en Inicial, "1".."6" en Primaria, "1".."5" en Secundaria).
 */
@Entity
@Table(name = "grado", uniqueConstraints = @UniqueConstraint(name = "uq_grado_nivel_codigo", columnNames = {"nivel_id", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nivel_id", nullable = false, foreignKey = @ForeignKey(name = "fk_grado_nivel"))
    private NivelEducativo nivel;

    @Column(nullable = false, length = 20)
    private String codigo;

    /** Texto para mostrar: "1er grado", "3ro año", "4 años". */
    @Column(nullable = false, length = 40)
    private String etiqueta;

    @Column(nullable = false)
    private Integer orden;

    public Grado(NivelEducativo nivel, String codigo, String etiqueta, int orden) {
        this.nivel = nivel;
        this.codigo = codigo;
        this.etiqueta = etiqueta;
        this.orden = orden;
    }
}

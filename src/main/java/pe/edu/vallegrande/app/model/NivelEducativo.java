package pe.edu.vallegrande.app.model;

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

/** Tabla maestra: Inicial, Primaria, Secundaria. */
@Entity
@Table(name = "nivel_educativo", uniqueConstraints = @UniqueConstraint(name = "uq_nivel_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
public class NivelEducativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(nullable = false)
    private Integer orden;

    public NivelEducativo(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;
    }
}

package pe.edu.vallegrande.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import pe.edu.vallegrande.app.model.enums.Turno;

/** Aula: grado + letra (A, B, C) con su turno. */
@Entity
@Table(name = "seccion", uniqueConstraints = @UniqueConstraint(name = "uq_seccion_grado_letra", columnNames = {"grado_id", "letra"}))
@Getter
@Setter
@NoArgsConstructor
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seccion_grado"))
    private Grado grado;

    @Column(nullable = false, length = 2)
    private String letra;

    @Convert(converter = Turno.Convertidor.class)
    @Column(nullable = false, length = 10)
    private Turno turno;

    public Seccion(Grado grado, String letra, Turno turno) {
        this.grado = grado;
        this.letra = letra;
        this.turno = turno;
    }

    public String getNivelNombre() {
        return grado.getNivel().getNombre();
    }
}

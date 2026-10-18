package pe.edu.vallegrande.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.vallegrande.app.model.enums.Nivel;
import pe.edu.vallegrande.app.model.enums.Turno;
import pe.edu.vallegrande.app.util.FiltroEstado;

/**
 * Aula: nivel + grado + sección con su turno. El grado es el valor que usa el frontend
 * ("3 años" en Inicial, "1".."6" en Primaria, "1".."5" en Secundaria).
 */
@Entity
@Table(name = "grados_secciones",
        uniqueConstraints = @UniqueConstraint(name = "uq_grados_secciones", columnNames = {"nivel", "grado", "seccion"}))
@Getter
@Setter
@NoArgsConstructor
public class GradoSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = Nivel.Convertidor.class)
    @Column(nullable = false, length = 12)
    private Nivel nivel;

    @Column(nullable = false, length = 10)
    private String grado;

    /** Letra de la sección: A, B, C... */
    @Column(nullable = false, length = 2)
    private String seccion;

    @Convert(converter = Turno.Convertidor.class)
    @Column(nullable = false, length = 10)
    private Turno turno;

    /** A = activo, I = inactivo (eliminado lógicamente; se puede restaurar). */
    @Column(nullable = false, length = 1)
    private String estado = FiltroEstado.ACTIVO;

    public GradoSeccion(Nivel nivel, String grado, String seccion, Turno turno) {
        this.nivel = nivel;
        this.grado = grado;
        this.seccion = seccion;
        this.turno = turno;
    }

    /** Texto para mostrar el grado: "1ro grado", "3ro año", "4 años". */
    public String getGradoEtiqueta() {
        if (nivel == Nivel.INICIAL) {
            return grado;
        }
        String ordinal = switch (grado) {
            case "1" -> "1ro";
            case "2" -> "2do";
            case "3" -> "3ro";
            case "4" -> "4to";
            case "5" -> "5to";
            case "6" -> "6to";
            default -> grado + "°";
        };
        return ordinal + (nivel == Nivel.SECUNDARIA ? " año" : " grado");
    }

    public boolean esActivo() {
        return FiltroEstado.ACTIVO.equals(estado);
    }

    /** "1ro grado A - Primaria". */
    public String getNombre() {
        return getGradoEtiqueta() + " " + seccion + " - " + nivel.getValor();
    }
}

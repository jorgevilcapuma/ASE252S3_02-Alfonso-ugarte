package pe.edu.vallegrande.app.model;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.vallegrande.app.util.FiltroEstado;

/** Curso dictado en un aula por un docente (p. ej. "Matemática" de 1ro grado A). */
@Entity
@Table(name = "cursos",
        uniqueConstraints = @UniqueConstraint(name = "uq_cursos_nombre_aula", columnNames = {"nombre", "grado_seccion_id"}),
        indexes = @Index(name = "ix_cursos_docente", columnList = "docente_id"))
@Getter
@Setter
@NoArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grado_seccion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cursos_grado_seccion"))
    private GradoSeccion gradoSeccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cursos_docente"))
    private Docente docente;

    /** A = activo, I = inactivo (eliminado lógicamente; se puede restaurar). */
    @Column(nullable = false, length = 1)
    private String estado = FiltroEstado.ACTIVO;

    public Curso(String nombre, GradoSeccion gradoSeccion, Docente docente) {
        this.nombre = nombre;
        this.gradoSeccion = gradoSeccion;
        this.docente = docente;
    }

    public boolean esActivo() {
        return FiltroEstado.ACTIVO.equals(estado);
    }

    /** "Matemática (1ro grado A)". */
    public String getNombreConAula() {
        return nombre + " (" + gradoSeccion.getGradoEtiqueta() + " " + gradoSeccion.getSeccion() + ")";
    }
}

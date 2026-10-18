package pe.edu.vallegrande.app.model;

import java.time.LocalDate;

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

/** Estudiante con su aula actual. El retiro es lógico (estado I) y se puede restaurar (estado A). */
@Entity
/*
 * Una cuenta se vincula a lo sumo a un estudiante: en SQL Server es el índice filtrado
 * uq_estudiantes_usuario (WHERE usuario_id IS NOT NULL), porque un UNIQUE común admite un solo NULL.
 */
@Table(name = "estudiantes",
        uniqueConstraints = @UniqueConstraint(name = "uq_estudiantes_dni", columnNames = "dni"),
        indexes = @Index(name = "ix_estudiantes_grado_seccion", columnList = "grado_seccion_id, estado"))
@Getter
@Setter
@NoArgsConstructor
public class Estudiante {

    public static final String ACTIVO = "A";
    public static final String INACTIVO = "I";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_estudiantes_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grado_seccion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_estudiantes_grado_seccion"))
    private GradoSeccion gradoSeccion;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, length = 8)
    private String dni;

    /** M o F. */
    @Column(length = 1)
    private String genero;

    /** Contacto editable desde "Mi perfil" del alumno. */
    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    /** A = activo, I = retirado. */
    @Column(nullable = false, length = 1)
    private String estado;

    @Column(nullable = false)
    private LocalDate creado;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public boolean esActivo() {
        return ACTIVO.equals(estado);
    }
}

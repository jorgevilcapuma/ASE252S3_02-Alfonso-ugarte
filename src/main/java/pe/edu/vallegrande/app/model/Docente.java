package pe.edu.vallegrande.app.model;

import java.time.LocalDate;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Personal del colegio: docentes, auxiliares, administrativos, directivos y de servicio.
 * Todos marcan asistencia (asistencia_personal); solo el cargo Docente dicta cursos.
 * No se borra: la eliminación es lógica (estado I) y se puede restaurar (estado A).
 */
@Entity
/*
 * Una cuenta se vincula a lo sumo a una persona: en SQL Server es el índice filtrado
 * uq_docentes_usuario (WHERE usuario_id IS NOT NULL), porque un UNIQUE común admite un solo NULL.
 */
@Table(name = "docentes",
        uniqueConstraints = @UniqueConstraint(name = "uq_docentes_dni", columnNames = "dni"),
        indexes = @Index(name = "ix_docentes_estado", columnList = "estado"))
@Getter
@Setter
@NoArgsConstructor
public class Docente {

    public static final String ACTIVO = "A";
    public static final String INACTIVO = "I";
    public static final String CARGO_DOCENTE = "Docente";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_docentes_usuario"))
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, length = 8)
    private String dni;

    /** Docente, Auxiliar, Administrativo, Directivo o Servicio. */
    @Column(nullable = false, length = 20)
    private String cargo;

    @Column(length = 120)
    private String especialidad;

    @Column(length = 80)
    private String area;

    @Column(length = 9)
    private String celular;

    @Column(length = 120)
    private String correo;

    /** M o F. */
    @Column(nullable = false, length = 1)
    private String sexo;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    /** A = activo, I = inactivo (eliminado lógicamente). */
    @Column(nullable = false, length = 1)
    private String estado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public boolean esActivo() {
        return ACTIVO.equals(estado);
    }

    public boolean esDocente() {
        return CARGO_DOCENTE.equals(cargo);
    }
}

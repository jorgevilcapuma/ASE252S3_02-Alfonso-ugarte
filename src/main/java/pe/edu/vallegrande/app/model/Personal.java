package pe.edu.vallegrande.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla maestra del personal del colegio (docentes, auxiliares, administrativos, directivos).
 * No se borra: la eliminación es lógica (estado I) y se puede restaurar (estado A).
 */
@Entity
@Table(name = "personal",
        uniqueConstraints = @UniqueConstraint(name = "uq_personal_dni", columnNames = "dni"),
        indexes = @Index(name = "ix_personal_estado", columnList = "estado"))
@Getter
@Setter
@NoArgsConstructor
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, length = 8)
    private String dni;

    /** Docente, Auxiliar, Administrativo, Directivo o Servicio. */
    @Column(nullable = false, length = 20)
    private String cargo;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;
}

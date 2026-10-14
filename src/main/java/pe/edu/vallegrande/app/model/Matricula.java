package pe.edu.vallegrande.app.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;

/**
 * Matrícula de un estudiante en una sección durante un año académico.
 * Un traslado cambia la sección conservando el historial; el retiro la marca como "Retirada".
 */
@Entity
@Table(name = "matricula",
        uniqueConstraints = @UniqueConstraint(name = "uq_matricula_estudiante_anio", columnNames = {"estudiante_id", "anio_academico"}),
        indexes = @Index(name = "ix_matricula_seccion_estado", columnList = "seccion_id, estado"))
@Getter
@Setter
@NoArgsConstructor
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_matricula_estudiante"))
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_matricula_seccion"))
    private Seccion seccion;

    @Column(name = "anio_academico", nullable = false)
    private Integer anioAcademico;

    @Column(name = "fecha_matricula", nullable = false)
    private LocalDate fechaMatricula;

    @Convert(converter = EstadoMatricula.Convertidor.class)
    @Column(nullable = false, length = 10)
    private EstadoMatricula estado;

    @Column(name = "fecha_retiro")
    private LocalDate fechaRetiro;
}

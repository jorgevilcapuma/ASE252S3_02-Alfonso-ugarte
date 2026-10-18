package pe.edu.vallegrande.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;

/**
 * Asistencia de un estudiante en una fecha. Sin curso es la asistencia diaria del aula;
 * con curso, la de esa clase. Un registro por estudiante, fecha y curso (índices filtrados
 * uq_asistencia_estudiante_curso y uq_asistencia_estudiante_diaria en SQL Server).
 */
@Entity
@Table(name = "asistencia_estudiante", indexes = @Index(name = "ix_asistencia_estudiante_fecha", columnList = "fecha, estado"))
@Getter
@Setter
@NoArgsConstructor
public class AsistenciaEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_estudiante_estudiante"))
    private Estudiante estudiante;

    /** null = asistencia diaria del aula. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", foreignKey = @ForeignKey(name = "fk_asistencia_estudiante_curso"))
    private Curso curso;

    @Column(nullable = false)
    private LocalDate fecha;

    @Convert(converter = EstadoAsistencia.Convertidor.class)
    @Column(nullable = false, length = 12)
    private EstadoAsistencia estado;

    @Column(length = 255)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", foreignKey = @ForeignKey(name = "fk_asistencia_estudiante_usuario"))
    private Usuario registradoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}

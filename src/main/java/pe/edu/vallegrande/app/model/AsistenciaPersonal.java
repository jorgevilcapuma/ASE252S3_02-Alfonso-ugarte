package pe.edu.vallegrande.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
 * Marcación de entrada y salida del personal (tabla docentes: docentes, auxiliares, administrativos): una por persona y fecha.
 * Es independiente de la asistencia de los estudiantes.
 */
@Entity
@Table(name = "asistencia_personal",
        uniqueConstraints = @UniqueConstraint(name = "uq_asistencia_personal_fecha", columnNames = {"docente_id", "fecha"}),
        indexes = @Index(name = "ix_asistencia_personal_fecha", columnList = "fecha"))
@Getter
@Setter
@NoArgsConstructor
public class AsistenciaPersonal {

    public static final String PUNTUAL = "puntual";
    public static final String TARDANZA = "tardanza";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_personal_docente"))
    private Docente docente;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    /** puntual o tardanza, según la hora de entrada y la tolerancia configuradas. */
    @Column(nullable = false, length = 10)
    private String estado;

    @Column(length = 200)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", foreignKey = @ForeignKey(name = "fk_asistencia_personal_usuario"))
    private Usuario registradoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}

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
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;

/**
 * Solicitud de justificación de una inasistencia. Guarda el estado de asistencia previo
 * para restaurarlo si la solicitud se observa.
 */
@Entity
@Table(name = "justificaciones", indexes = {
        @Index(name = "ix_justificaciones_estudiante_fecha", columnList = "estudiante_id, fecha"),
        @Index(name = "ix_justificaciones_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
public class Justificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_justificaciones_estudiante"))
    private Estudiante estudiante;

    /** Curso de la inasistencia (opcional: sin curso se justifica el día completo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", foreignKey = @ForeignKey(name = "fk_justificaciones_curso"))
    private Curso curso;

    /** Fecha de la clase que se justifica. */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 200)
    private String motivo;

    /** Detalle o comentario del alumno/familia. */
    @Column(length = 500)
    private String detalle;

    @Column(name = "archivo_nombre", length = 255)
    private String archivoNombre;

    @Convert(converter = EstadoJustificacion.Convertidor.class)
    @Column(nullable = false, length = 10)
    private EstadoJustificacion estado;

    @Convert(converter = EstadoAsistencia.Convertidor.class)
    @Column(name = "estado_previo", length = 12)
    private EstadoAsistencia estadoPrevio;

    /** Respuesta de quien revisa (docente o administrador). */
    @Column(length = 500)
    private String observacion;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por", foreignKey = @ForeignKey(name = "fk_justificaciones_revisor"))
    private Usuario revisadoPor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;
}

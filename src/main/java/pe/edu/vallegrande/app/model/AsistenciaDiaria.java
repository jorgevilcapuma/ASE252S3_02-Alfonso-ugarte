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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;

/** Registro diario por aula (matriz P/T/F/J): uno por estudiante y fecha. */
@Entity
@Table(name = "asistencia_diaria",
        uniqueConstraints = @UniqueConstraint(name = "uq_asistencia_estudiante_fecha", columnNames = {"estudiante_id", "fecha"}),
        indexes = @Index(name = "ix_asistencia_fecha_estado", columnList = "fecha, estado"))
@Getter
@Setter
@NoArgsConstructor
public class AsistenciaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_estudiante"))
    private Estudiante estudiante;

    @Column(nullable = false)
    private LocalDate fecha;

    @Convert(converter = EstadoAsistencia.Convertidor.class)
    @Column(nullable = false, length = 12)
    private EstadoAsistencia estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", foreignKey = @ForeignKey(name = "fk_asistencia_usuario"))
    private Usuario registradoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}

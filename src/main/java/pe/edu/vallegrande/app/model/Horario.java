package pe.edu.vallegrande.app.model;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.vallegrande.app.model.enums.DiaSemana;

/** Bloque horario de una asignación (día, hora de inicio y fin). */
@Entity
@Table(name = "horario", uniqueConstraints = @UniqueConstraint(name = "uq_horario_bloque", columnNames = {"asignacion_id", "dia", "hora_inicio"}))
@Getter
@Setter
@NoArgsConstructor
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_horario_asignacion"))
    private Asignacion asignacion;

    @Convert(converter = DiaSemana.Convertidor.class)
    @Column(nullable = false, length = 10)
    private DiaSemana dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    public Horario(DiaSemana dia, LocalTime horaInicio, LocalTime horaFin) {
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }
}

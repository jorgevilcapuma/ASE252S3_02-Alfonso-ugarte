package pe.edu.vallegrande.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sesión de asistencia de un curso en una fecha ("Asistencia por curso" del docente). */
@Entity
@Table(name = "sesion_clase",
        uniqueConstraints = @UniqueConstraint(name = "uq_sesion_asignacion_fecha", columnNames = {"asignacion_id", "fecha"}),
        indexes = @Index(name = "ix_sesion_fecha", columnList = "fecha"))
@Getter
@Setter
@NoArgsConstructor
public class SesionClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sesion_asignacion"))
    private Asignacion asignacion;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", foreignKey = @ForeignKey(name = "fk_sesion_usuario"))
    private Usuario registradoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AsistenciaCurso> registros = new ArrayList<>();
}

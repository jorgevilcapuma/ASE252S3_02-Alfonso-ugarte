package pe.edu.vallegrande.app.model;

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
import pe.edu.vallegrande.app.model.enums.EstadoAsistenciaCurso;

/** Estado de un alumno dentro de una sesión de clase. */
@Entity
@Table(name = "asistencia_curso",
        uniqueConstraints = @UniqueConstraint(name = "uq_asistencia_curso_sesion_estudiante", columnNames = {"sesion_id", "estudiante_id"}),
        indexes = @Index(name = "ix_asistencia_curso_estudiante", columnList = "estudiante_id"))
@Getter
@Setter
@NoArgsConstructor
public class AsistenciaCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_curso_sesion"))
    private SesionClase sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_curso_estudiante"))
    private Estudiante estudiante;

    @Convert(converter = EstadoAsistenciaCurso.Convertidor.class)
    @Column(nullable = false, length = 12)
    private EstadoAsistenciaCurso estado;

    @Column(length = 255)
    private String observacion;
}

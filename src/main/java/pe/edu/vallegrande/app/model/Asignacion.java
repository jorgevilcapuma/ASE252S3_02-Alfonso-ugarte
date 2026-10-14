package pe.edu.vallegrande.app.model;

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

/**
 * Curso dictado en una sección por un docente, con aula y horario
 * (lo que el frontend del docente llama "Curso" y el del alumno "CursoAlumno").
 */
@Entity
@Table(name = "asignacion",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_asignacion_codigo", columnNames = "codigo"),
                @UniqueConstraint(name = "uq_asignacion_curso_seccion", columnNames = {"curso_id", "seccion_id"})
        },
        indexes = {
                @Index(name = "ix_asignacion_docente", columnList = "docente_id"),
                @Index(name = "ix_asignacion_seccion", columnList = "seccion_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código visible, por ejemplo MAT-3A. */
    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asignacion_curso"))
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asignacion_seccion"))
    private Seccion seccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asignacion_docente"))
    private Docente docente;

    @Column(length = 60)
    private String aula;

    @OneToMany(mappedBy = "asignacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Horario> horarios = new ArrayList<>();

    public void reemplazarHorarios(List<Horario> nuevos) {
        horarios.clear();
        nuevos.forEach(horario -> {
            horario.setAsignacion(this);
            horarios.add(horario);
        });
    }
}

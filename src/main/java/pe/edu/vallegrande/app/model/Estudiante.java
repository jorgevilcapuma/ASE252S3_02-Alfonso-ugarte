package pe.edu.vallegrande.app.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
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

/** Datos personales del estudiante; su aula se obtiene de la matrícula del año. */
@Entity
/*
 * Una cuenta se vincula a lo sumo a un estudiante: en SQL Server es el índice filtrado
 * uq_estudiante_usuario (WHERE usuario_id IS NOT NULL), porque un UNIQUE común admite un solo NULL.
 */
@Table(name = "estudiante", uniqueConstraints = @UniqueConstraint(name = "uq_estudiante_dni", columnNames = "dni"))
@Getter
@Setter
@NoArgsConstructor
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_estudiante_usuario"))
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, length = 8)
    private String dni;

    /** M o F (filtro de "Mis estudiantes" del docente). */
    @Column(length = 1)
    private String genero;

    /** Contacto editable desde "Mi perfil" del alumno. */
    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Column(nullable = false)
    private LocalDate creado;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}

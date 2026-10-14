package pe.edu.vallegrande.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tabla maestra de cursos / áreas curriculares (Matemática, Comunicación, ...). */
@Entity
@Table(name = "curso", uniqueConstraints = @UniqueConstraint(name = "uq_curso_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    public Curso(String nombre) {
        this.nombre = nombre;
    }
}

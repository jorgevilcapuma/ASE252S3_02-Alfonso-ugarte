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

/** Ficha del docente ("Mi perfil"). La cuenta de acceso es opcional. */
@Entity
/*
 * Una cuenta se vincula a lo sumo a un docente: en SQL Server es el índice filtrado
 * uq_docente_usuario (WHERE usuario_id IS NOT NULL), porque un UNIQUE común admite un solo NULL.
 */
@Table(name = "docente", uniqueConstraints = @UniqueConstraint(name = "uq_docente_dni", columnNames = "dni"))
@Getter
@Setter
@NoArgsConstructor
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_docente_usuario"))
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, length = 8)
    private String dni;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String especialidad;

    @Column(length = 40)
    private String colegiatura;

    @Column(name = "codigo_plaza", length = 30)
    private String codigoPlaza;

    @Column(length = 30)
    private String condicion;

    @Column(name = "nivel_educativo", length = 30)
    private String nivelEducativo;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}

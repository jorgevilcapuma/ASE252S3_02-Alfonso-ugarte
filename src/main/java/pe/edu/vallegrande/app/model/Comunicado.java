package pe.edu.vallegrande.app.model;

import java.time.LocalDate;

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
import pe.edu.vallegrande.app.model.enums.Prioridad;

/** Comunicado de un docente: para un curso concreto o, sin curso, para todos. */
@Entity
@Table(name = "comunicado", indexes = {
        @Index(name = "ix_comunicado_asignacion", columnList = "asignacion_id"),
        @Index(name = "ix_comunicado_fecha", columnList = "fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String contenido;

    @Convert(converter = Prioridad.Convertidor.class)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id", foreignKey = @ForeignKey(name = "fk_comunicado_asignacion"))
    private Asignacion asignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", foreignKey = @ForeignKey(name = "fk_comunicado_autor"))
    private Usuario autor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;
}

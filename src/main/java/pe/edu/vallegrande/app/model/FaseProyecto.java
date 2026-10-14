package pe.edu.vallegrande.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.vallegrande.app.model.enums.EstadoFase;

/** Fases del proyecto (pantalla "Fases" del administrador). */
@Entity
@Table(name = "fase_proyecto", uniqueConstraints = @UniqueConstraint(name = "uq_fase_orden", columnNames = "orden"))
@Getter
@Setter
@NoArgsConstructor
public class FaseProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 10)
    private String semana;

    @Column(nullable = false, length = 80)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String actividad;

    @Column(nullable = false, length = 150)
    private String producto;

    @Convert(converter = EstadoFase.Convertidor.class)
    @Column(nullable = false, length = 12)
    private EstadoFase estado;
}

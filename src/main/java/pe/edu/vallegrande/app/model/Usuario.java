package pe.edu.vallegrande.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;

/** Cuenta institucional administrada desde "Gestión de usuarios". */
@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uq_usuarios_correo", columnNames = "correo"))
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false, length = 200)
    private String contrasenaHash;

    @Convert(converter = Perfil.Convertidor.class)
    @Column(nullable = false, length = 15)
    private Perfil perfil;

    @Convert(converter = EstadoUsuario.Convertidor.class)
    @Column(nullable = false, length = 10)
    private EstadoUsuario estado;

    @Column(nullable = false)
    private LocalDate creado;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;
}

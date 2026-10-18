package pe.edu.vallegrande.app.config;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.app.model.AsistenciaPersonal;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.GradoSeccion;
import pe.edu.vallegrande.app.model.Justificacion;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Nivel;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.repository.GradoSeccionRepository;
import pe.edu.vallegrande.app.repository.UsuarioRepository;
import pe.edu.vallegrande.app.seguridad.HasherContrasena;
import pe.edu.vallegrande.app.service.impl.ReparacionCuentas;
import pe.edu.vallegrande.app.service.impl.GradoSeccionServiceImpl;
import pe.edu.vallegrande.app.util.Calendario;
import pe.edu.vallegrande.app.util.Textos;

/**
 * Carga inicial idempotente (solo con tablas vacías):
 * <ul>
 *   <li>Siempre: grados y secciones (Inicial, Primaria y Secundaria con secciones A, B y C) y la cuenta del administrador.</li>
 *   <li>Con app.datos-demo.habilitado: docentes y personal administrativo con sus cuentas, 12 estudiantes por aula,
 *       cursos de 1ro grado A de Primaria y 3ro año A de Secundaria, historial de asistencia y una justificación pendiente.</li>
 * </ul>
 */
@Slf4j
@Component
@Order(1)
public class DatosIniciales implements ApplicationRunner {

    public static final String CORREO_ADMIN = "admin@ie20191.edu.pe";

    private static final String[] NOMBRES_DEMO = {
            "Lucía Bustamante Morales", "Juan Alvarado Mendoza", "Sofía Castro Villanueva",
            "Carlos Díaz Peña", "Valeria Flores Rojas", "Mateo García Pérez",
            "Camila Huamán Salas", "Diego López Cárdenas", "Andrea Medina Torres",
            "Thiago Navarro Ruiz", "Daniela Ortega León", "Sebastián Ramos Vega"
    };

    private static final Set<String> NOMBRES_FEMENINOS = Set.of("Lucía", "Sofía", "Valeria", "Camila", "Andrea", "Daniela");

    private final PropiedadesApp propiedades;
    private final TransactionTemplate transaccion;
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final HasherContrasena hasher;
    private final Calendario calendario;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReparacionCuentas reparacionCuentas;

    public DatosIniciales(PropiedadesApp propiedades, TransactionTemplate transaccion, EntityManager em, JdbcTemplate jdbc, HasherContrasena hasher, Calendario calendario, GradoSeccionRepository gradoSeccionRepository, UsuarioRepository usuarioRepository,
                          ReparacionCuentas reparacionCuentas) {
        this.reparacionCuentas = reparacionCuentas;
        this.propiedades = propiedades;
        this.transaccion = transaccion;
        this.em = em;
        this.jdbc = jdbc;
        this.hasher = hasher;
        this.calendario = calendario;
        this.gradoSeccionRepository = gradoSeccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        transaccion.executeWithoutResult(estado -> {
            if (gradoSeccionRepository.count() == 0) {
                cargarMaestros();
            }
            boolean sinDocentes = em.createQuery("select count(d) from Docente d", Long.class).getSingleResult() == 0;
            if (propiedades.datosDemo().habilitado() && sinDocentes) {
                cargarDemo();
            }
        });
        // Fichas registradas antes de que el correo fuera la cuenta de acceso.
        reparacionCuentas.reparar();
    }

    // =====================================================================
    // Tablas maestras
    // =====================================================================

    private void cargarMaestros() {
        Map<Nivel, String[]> gradosPorNivel = new java.util.LinkedHashMap<>();
        gradosPorNivel.put(Nivel.INICIAL, new String[]{"3 años", "4 años", "5 años"});
        gradosPorNivel.put(Nivel.PRIMARIA, new String[]{"1", "2", "3", "4", "5", "6"});
        gradosPorNivel.put(Nivel.SECUNDARIA, new String[]{"1", "2", "3", "4", "5"});
        for (Map.Entry<Nivel, String[]> entrada : gradosPorNivel.entrySet()) {
            for (String grado : entrada.getValue()) {
                for (String seccion : new String[]{"A", "B", "C"}) {
                    em.persist(new GradoSeccion(entrada.getKey(), grado, seccion, GradoSeccionServiceImpl.turnoPorDefecto(seccion)));
                }
            }
        }
        if (!usuarioRepository.existsByCorreoIgnoreCase(CORREO_ADMIN)) {
            usuario("Administrador General", CORREO_ADMIN, Perfil.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        }
        em.flush();
        log.info("Tablas maestras cargadas (grados y secciones, administrador).");
    }

    // =====================================================================
    // Datos de demostración (mismos nombres y cuentas que el frontend)
    // =====================================================================

    private void cargarDemo() {
        int anio = calendario.anioActual();

        // --- Cuentas ---
        Usuario elenaU = usuario("Elena Salazar Ríos", "elena.salazar@ie20191.edu.pe", Perfil.DOCENTE, EstadoUsuario.ACTIVO);
        Usuario luisU = usuario("Luis Mendoza Castro", "luis.mendoza@ie20191.edu.pe", Perfil.DOCENTE, EstadoUsuario.ACTIVO);
        Usuario carmenU = usuario("Carmen Rojas Villa", "carmen.rojas@ie20191.edu.pe", Perfil.DOCENTE, EstadoUsuario.ACTIVO);
        Usuario jorgeU = usuario("Jorge Paredes Lima", "jorge.paredes@ie20191.edu.pe", Perfil.DOCENTE, EstadoUsuario.ACTIVO);
        Usuario rosaU = usuario("Rosa Huamán Quispe", "rosa.huaman@ie20191.edu.pe", Perfil.DOCENTE, EstadoUsuario.ACTIVO);
        Usuario carlosU = usuario("Carlos Antonio Mendoza Ríos", "carlos.mendoza@ie20191.edu.pe", Perfil.DOCENTE, EstadoUsuario.ACTIVO);
        Usuario luciaU = usuario("Lucía Bustamante Morales", "lucia.bustamante@ie20191.edu.pe", Perfil.ALUMNO, EstadoUsuario.ACTIVO);
        Usuario juanU = usuario("Juan Alvarado Mendoza", "juan.alvarado@ie20191.edu.pe", Perfil.ALUMNO, EstadoUsuario.INACTIVO);
        for (Usuario u : List.of(elenaU, luisU, carmenU, jorgeU, rosaU, carlosU, luciaU, juanU)) {
            u.setCreado(LocalDate.of(anio, 3, 1));
        }

        // --- Docentes ---
        Docente carlos = personal("Carlos Antonio", "Mendoza Ríos", "41829304", "Docente", "Matemática y Ciencias", "M", carlosU);
        carlos.setCelular("987654321");
        carlos.setFechaIngreso(LocalDate.of(2018, 3, 15));
        Docente elena = personal("Elena", "Salazar Ríos", "41829305", "Docente", "Comunicación", "F", elenaU);
        Docente luis = personal("Luis", "Mendoza Castro", "41829306", "Docente", "Matemática", "M", luisU);
        Docente carmen = personal("Carmen", "Rojas Villa", "41829307", "Docente", "Personal Social", "F", carmenU);
        Docente jorge = personal("Jorge", "Paredes Lima", "41829308", "Docente", "Ciencia y Tecnología", "M", jorgeU);
        Docente rosa = personal("Rosa", "Huamán Quispe", "41829309", "Docente", "Educación Física", "F", rosaU);
        Docente diana = personal("Diana", "Quispe", "41829310", "Docente", "Arte y Cultura", "F", null);
        Docente mario = personal("Mario", "Torres", "41829311", "Docente", "Inglés", "M", null);
        Docente ana = personal("Ana", "Castillo", "41829312", "Docente", "Educación Religiosa", "F", null);

        // --- Personal administrativo, auxiliar, directivo y de servicio ---
        personal("Gloria", "Fernández Paz", "41829320", "Directivo", null, "F", null).setArea("Dirección");
        personal("Raúl", "Chávez Soto", "41829321", "Administrativo", null, "M", null).setArea("Secretaría");
        personal("Milagros", "Vega Ramos", "41829322", "Auxiliar", null, "F", null).setArea("Primaria");
        personal("Pedro", "Lázaro Cruz", "41829323", "Servicio", null, "M", null).setArea("Mantenimiento");

        // --- Estudiantes: 12 por aula + el alumno de la demo de ingreso ---
        Map<String, GradoSeccion> aulas = new HashMap<>();
        List<GradoSeccion> ordenadas = gradoSeccionRepository.listar(null, null, null);
        ordenadas.forEach(g -> aulas.put(g.getNivel().getValor() + "|" + g.getGrado() + "|" + g.getSeccion(), g));
        List<Estudiante> estudiantes = new ArrayList<>();
        int numero = 0;
        for (GradoSeccion aula : ordenadas) {
            for (String nombre : NOMBRES_DEMO) {
                numero++;
                estudiantes.add(estudiante(nombre, String.format("%08d", 70_000_000 + numero), aula, anio));
            }
        }
        GradoSeccion primeroA = aulas.get("Primaria|1|A");
        Estudiante juanPerez = estudiante("Juan Pérez Ugarte", "72819201", primeroA, anio);
        estudiantes.add(juanPerez);
        // Cuentas de alumno vinculadas a estudiantes de 1ro grado A de Primaria.
        estudiantes.stream().filter(e -> e.getGradoSeccion() == primeroA).forEach(e -> {
            if (e.getNombreCompleto().equals("Lucía Bustamante Morales")) {
                e.setUsuario(luciaU);
                e.setCorreo(luciaU.getCorreo());
            } else if (e.getNombreCompleto().equals("Juan Alvarado Mendoza")) {
                e.setUsuario(juanU);
            }
        });

        // --- Cursos ---
        List<Curso> cursos = new ArrayList<>();
        Object[][] cursosPrimeroA = {
                {"Comunicación", elena}, {"Matemática", luis}, {"Personal Social", carmen}, {"Ciencia y Tecnología", jorge},
                {"Educación Física", rosa}, {"Arte y Cultura", diana}, {"Inglés", mario}, {"Educación Religiosa", ana}, {"Tutoría", elena}
        };
        for (Object[] c : cursosPrimeroA) {
            cursos.add(curso((String) c[0], primeroA, (Docente) c[1]));
        }
        GradoSeccion terceroSecA = aulas.get("Secundaria|3|A");
        cursos.add(curso("Matemática", terceroSecA, carlos));
        cursos.add(curso("Ciencia y Tecnología", terceroSecA, carlos));
        cursos.add(curso("Comunicación", terceroSecA, elena));
        em.flush();

        // --- Historial: asistencia diaria de estudiantes y marcaciones del personal ---
        int filas = historialEstudiantes(estudiantes, juanPerez);
        List<Docente> todoElPersonal = em.createQuery("select d from Docente d", Docente.class).getResultList();
        todoElPersonal.forEach(this::marcacionesDemo);

        log.info("Datos de demostración cargados: {} docentes y personal, {} estudiantes, {} cursos, {} registros de asistencia.",
                todoElPersonal.size(), estudiantes.size(), cursos.size(), filas);
    }

    /**
     * Asistencia diaria de los días hábiles anteriores a hoy: ~6 % de faltas y ~6 % de tardanzas.
     * El alumno de la demo falta el último día y tiene una justificación pendiente de esa fecha.
     */
    private int historialEstudiantes(List<Estudiante> estudiantes, Estudiante juanPerez) {
        int dias = propiedades.datosDemo().diasHistorial();
        if (dias <= 0) {
            return 0;
        }
        LocalDate hoy = calendario.hoy();
        List<LocalDate> fechas = Calendario.diasHabiles(dias + 1, hoy).stream().filter(f -> f.isBefore(hoy)).toList();
        Timestamp ahora = Timestamp.valueOf(calendario.ahora());
        List<Object[]> filas = new ArrayList<>();
        for (Estudiante e : estudiantes) {
            for (LocalDate fecha : fechas) {
                long azar = hash(e.getDni() + "|" + fecha) % 100;
                EstadoAsistencia estado = azar < 6 ? EstadoAsistencia.FALTA : azar < 12 ? EstadoAsistencia.TARDANZA : EstadoAsistencia.PRESENTE;
                if (e == juanPerez) {
                    estado = fecha.equals(fechas.get(fechas.size() - 1)) ? EstadoAsistencia.FALTA : EstadoAsistencia.PRESENTE;
                }
                filas.add(new Object[]{e.getId(), Date.valueOf(fecha), estado.getValor(), ahora});
            }
        }
        jdbc.batchUpdate("insert into asistencia_estudiante (estudiante_id, fecha, estado, actualizado_en) values (?, ?, ?, ?)", filas);

        if (!fechas.isEmpty()) {
            Justificacion j = new Justificacion();
            j.setEstudiante(juanPerez);
            j.setFecha(fechas.get(fechas.size() - 1));
            j.setMotivo("Cita médica comunicada por la familia.");
            j.setEstado(EstadoJustificacion.PENDIENTE);
            j.setEstadoPrevio(EstadoAsistencia.FALTA);
            j.setFechaSolicitud(LocalDateTime.of(j.getFecha(), LocalTime.of(18, 0)));
            em.persist(j);
        }
        return filas.size();
    }

    /**
     * Entradas y salidas de los días hábiles anteriores a hoy: ~3 % de ausencias, ~8 % de tardanzas
     * (después de 07:55) y el resto entre 07:25 y 07:54. Mismo cálculo que el modo local del frontend.
     */
    private void marcacionesDemo(Docente p) {
        int dias = propiedades.datosDemo().diasHistorial();
        if (dias <= 0) {
            return;
        }
        LocalDate hoy = calendario.hoy();
        for (LocalDate fecha : Calendario.diasHabiles(dias + 1, hoy)) {
            if (!fecha.isBefore(hoy)) {
                continue;
            }
            long azar = hash(p.getDni() + "|personal|" + fecha) % 100;
            if (azar >= 97) {
                continue;
            }
            long minutos = hash(p.getDni() + "|" + fecha + "|entrada") % 20;
            LocalTime entrada = azar < 8 ? LocalTime.of(7, 56).plusMinutes(minutos) : LocalTime.of(7, 25).plusMinutes(minutos + azar % 10);
            AsistenciaPersonal a = new AsistenciaPersonal();
            a.setDocente(p);
            a.setFecha(fecha);
            a.setHoraEntrada(entrada);
            a.setHoraSalida(LocalTime.of(13, 30).plusMinutes(hash(p.getDni() + "|" + fecha + "|salida") % 40));
            a.setEstado(azar < 8 ? AsistenciaPersonal.TARDANZA : AsistenciaPersonal.PUNTUAL);
            a.setActualizadoEn(calendario.ahora());
            em.persist(a);
        }
    }

    private Usuario usuario(String nombre, String correo, Perfil perfil, EstadoUsuario estado) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setPerfil(perfil);
        u.setEstado(estado);
        u.setCreado(calendario.hoy());
        u.setContrasenaHash(hasher.generar(propiedades.seguridad().contrasenaInicial()));
        em.persist(u);
        return u;
    }

    private Docente personal(String nombres, String apellidos, String dni, String cargo, String especialidad, String sexo, Usuario usuario) {
        Docente d = new Docente();
        d.setNombres(nombres);
        d.setApellidos(apellidos);
        d.setDni(dni);
        d.setCargo(cargo);
        d.setEspecialidad(especialidad);
        d.setSexo(sexo);
        d.setUsuario(usuario);
        d.setCorreo(usuario == null ? null : usuario.getCorreo());
        d.setEstado(Docente.ACTIVO);
        d.setCreadoEn(calendario.ahora());
        em.persist(d);
        return d;
    }

    private Estudiante estudiante(String nombreCompleto, String dni, GradoSeccion aula, int anio) {
        String[] partes = Textos.separarNombreCompleto(nombreCompleto);
        Estudiante e = new Estudiante();
        e.setNombres(partes[0]);
        e.setApellidos(partes[1]);
        e.setDni(dni);
        e.setGradoSeccion(aula);
        e.setGenero(NOMBRES_FEMENINOS.contains(partes[0]) ? "F" : "M");
        e.setEstado(Estudiante.ACTIVO);
        e.setCreado(LocalDate.of(anio, 3, 1));
        em.persist(e);
        return e;
    }

    private Curso curso(String nombre, GradoSeccion aula, Docente docente) {
        Curso c = new Curso(nombre, aula, docente);
        em.persist(c);
        return c;
    }

    /** Mismo hash (mezcla murmur3) que el frontend para repartir faltas de forma estable. */
    private static long hash(String texto) {
        int h = 7;
        for (char c : texto.toCharArray()) {
            h = h * 31 + c;
        }
        h = (h ^ (h >>> 16)) * 0x85ebca6b;
        h = (h ^ (h >>> 13)) * 0xc2b2ae35;
        return (h ^ (h >>> 16)) & 0xffffffffL;
    }
}

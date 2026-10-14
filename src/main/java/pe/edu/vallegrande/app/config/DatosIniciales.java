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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.app.model.Asignacion;
import pe.edu.vallegrande.app.model.Comunicado;
import pe.edu.vallegrande.app.model.Curso;
import pe.edu.vallegrande.app.model.Docente;
import pe.edu.vallegrande.app.model.Estudiante;
import pe.edu.vallegrande.app.model.FaseProyecto;
import pe.edu.vallegrande.app.model.Grado;
import pe.edu.vallegrande.app.model.Horario;
import pe.edu.vallegrande.app.model.Institucion;
import pe.edu.vallegrande.app.model.Justificacion;
import pe.edu.vallegrande.app.model.Matricula;
import pe.edu.vallegrande.app.model.NivelEducativo;
import pe.edu.vallegrande.app.model.Personal;
import pe.edu.vallegrande.app.model.Seccion;
import pe.edu.vallegrande.app.model.SesionClase;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.model.enums.DiaSemana;
import pe.edu.vallegrande.app.model.enums.EstadoAsistencia;
import pe.edu.vallegrande.app.model.enums.EstadoFase;
import pe.edu.vallegrande.app.model.enums.EstadoJustificacion;
import pe.edu.vallegrande.app.model.enums.EstadoMatricula;
import pe.edu.vallegrande.app.model.enums.EstadoUsuario;
import pe.edu.vallegrande.app.model.enums.Perfil;
import pe.edu.vallegrande.app.model.enums.Prioridad;
import pe.edu.vallegrande.app.repository.InstitucionRepository;
import pe.edu.vallegrande.app.repository.NivelEducativoRepository;
import pe.edu.vallegrande.app.repository.UsuarioRepository;
import pe.edu.vallegrande.app.seguridad.HasherContrasena;
import pe.edu.vallegrande.app.service.impl.EstructuraServiceImpl;
import pe.edu.vallegrande.app.service.impl.InstitucionServiceImpl;
import pe.edu.vallegrande.app.util.Calendario;

/**
 * Carga inicial idempotente (solo con tablas vacías):
 * <ul>
 *   <li>Siempre: institución, niveles, grados, secciones, cursos, fases y la cuenta del administrador.</li>
 *   <li>Con app.datos-demo.habilitado: los datos de demostración del frontend (docentes, cuentas, 12 estudiantes
 *       por aula, cursos con horario, historial de asistencia, justificaciones y comunicados).</li>
 * </ul>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DatosIniciales implements ApplicationRunner {

    private static final String[] NOMBRES_DEMO = {
            "Lucía Bustamante Morales", "Juan Alvarado Mendoza", "Sofía Castro Villanueva",
            "Carlos Díaz Peña", "Valeria Flores Rojas", "Mateo García Pérez",
            "Camila Huamán Salas", "Diego López Cárdenas", "Andrea Medina Torres",
            "Thiago Navarro Ruiz", "Daniela Ortega León", "Sebastián Ramos Vega"
    };

    private static final java.util.Set<String> NOMBRES_FEMENINOS = java.util.Set.of("Lucía", "Sofía", "Valeria", "Camila", "Andrea", "Daniela");

    private final PropiedadesApp propiedades;
    private final TransactionTemplate transaccion;
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final HasherContrasena hasher;
    private final Calendario calendario;
    private final InstitucionRepository institucionRepository;
    private final NivelEducativoRepository nivelRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(ApplicationArguments args) {
        transaccion.executeWithoutResult(estado -> {
            if (institucionRepository.count() == 0) {
                cargarMaestros();
            }
            boolean sinEstudiantes = em.createQuery("select count(e) from Estudiante e", Long.class).getSingleResult() == 0;
            if (propiedades.datosDemo().habilitado() && sinEstudiantes) {
                cargarDemo();
            }
            // Aparte: también completa bases creadas antes de existir la tabla personal.
            if (propiedades.datosDemo().habilitado()
                    && em.createQuery("select count(p) from Personal p", Long.class).getSingleResult() == 0) {
                cargarPersonal();
            }
        });
    }

    // =====================================================================
    // Tablas maestras
    // =====================================================================

    private void cargarMaestros() {
        Institucion institucion = new Institucion();
        institucion.setNombre("I.E. 20191 Alfonso Ugarte");
        institucion.setCodigoModular("0285940");
        institucion.setDireccion("San Vicente de Cañete, Lima, Perú");
        institucion.setColorPrincipal(InstitucionServiceImpl.COLOR_PRINCIPAL);
        institucion.setColorSecundario(InstitucionServiceImpl.COLOR_SECUNDARIO);
        institucion.setActualizadoEn(calendario.ahora());
        em.persist(institucion);

        if (nivelRepository.count() == 0) {
            Map<String, String[]> gradosPorNivel = new java.util.LinkedHashMap<>();
            gradosPorNivel.put("Inicial", new String[]{"3 años", "4 años", "5 años"});
            gradosPorNivel.put("Primaria", new String[]{"1", "2", "3", "4", "5", "6"});
            gradosPorNivel.put("Secundaria", new String[]{"1", "2", "3", "4", "5"});
            int ordenNivel = 1;
            for (Map.Entry<String, String[]> entrada : gradosPorNivel.entrySet()) {
                NivelEducativo nivel = new NivelEducativo(entrada.getKey(), ordenNivel++);
                em.persist(nivel);
                int ordenGrado = 1;
                for (String codigo : entrada.getValue()) {
                    Grado grado = new Grado(nivel, codigo, etiquetaGrado(nivel.getNombre(), codigo), ordenGrado++);
                    em.persist(grado);
                    for (String letra : new String[]{"A", "B", "C"}) {
                        em.persist(new Seccion(grado, letra, EstructuraServiceImpl.turnoPorDefecto(letra)));
                    }
                }
            }
        }

        for (String nombre : new String[]{"Comunicación", "Matemática", "Personal Social", "Ciencia y Tecnología", "Educación Física",
                "Arte y Cultura", "Inglés", "Educación Religiosa", "Tutoría", "Psicomotriz", "Comunicación integral",
                "Física Elemental (Ciencia y Tecnología)", "Álgebra Avanzada", "Desarrollo Personal, Ciudadanía y Cívica",
                "Educación para el Trabajo (Computación)", "Razonamiento Matemático"}) {
            if (em.createQuery("select count(c) from Curso c where c.nombre = :n", Long.class).setParameter("n", nombre).getSingleResult() == 0) {
                em.persist(new Curso(nombre));
            }
        }

        Object[][] fases = {
                {"01", "Exploración", "Investigación de la comunicación dispersa y el control manual de asistencia.", "Ficha de diagnóstico institucional", EstadoFase.COMPLETADA},
                {"02", "Planeación", "Análisis de hallazgos, componentes e infraestructura para orientar la solución.", "Plan del proyecto", EstadoFase.COMPLETADA},
                {"03", "Requerimientos", "Definición de la matriz transaccional, justificaciones y alertas tempranas.", "Documento de requerimientos", EstadoFase.COMPLETADA},
                {"04", "Diseño técnico", "Diseño de pantallas y modelado de la base de datos relacional.", "Prototipo y arquitectura técnica", EstadoFase.EN_CURSO},
                {"05–06", "Desarrollo parcial", "Construcción web e integración de servicios y conexión a datos.", "Versión parcial del sistema web", EstadoFase.PENDIENTE},
                {"07", "Validación", "Pruebas de asistencia, justificaciones y alertas con retroalimentación.", "Informe de validación", EstadoFase.PENDIENTE},
                {"08", "Presentación final", "Demostración del sistema y sustentación de la solución.", "Presentación final", EstadoFase.PENDIENTE}
        };
        if (em.createQuery("select count(f) from FaseProyecto f", Long.class).getSingleResult() == 0) {
            for (int i = 0; i < fases.length; i++) {
                FaseProyecto fase = new FaseProyecto();
                fase.setOrden(i + 1);
                fase.setSemana((String) fases[i][0]);
                fase.setTitulo((String) fases[i][1]);
                fase.setActividad((String) fases[i][2]);
                fase.setProducto((String) fases[i][3]);
                fase.setEstado((EstadoFase) fases[i][4]);
                em.persist(fase);
            }
        }

        if (!usuarioRepository.existsByCorreoIgnoreCase("admin@ie20191.edu.pe")) {
            usuario("Administrador General", "admin@ie20191.edu.pe", Perfil.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        }
        em.flush();
        log.info("Tablas maestras cargadas (institución, estructura, cursos, fases, administrador).");
    }

    /** Igual que AsistenciaService.etiquetaGrado del frontend: "1ro grado", "3ro año", "4 años". */
    private static String etiquetaGrado(String nivel, String codigo) {
        if ("Inicial".equals(nivel)) {
            return codigo;
        }
        String ordinal = switch (codigo) {
            case "1" -> "1ro";
            case "2" -> "2do";
            case "3" -> "3ro";
            case "4" -> "4to";
            case "5" -> "5to";
            case "6" -> "6to";
            default -> codigo + "°";
        };
        return ordinal + ("Secundaria".equals(nivel) ? " año" : " grado");
    }

    // =====================================================================
    // Datos de demostración (mismos nombres y cuentas que el frontend)
    // =====================================================================

    private void cargarDemo() {
        int anio = calendario.anioActual();
        LocalDate hoy = calendario.hoy();

        // --- Cuentas (UsuariosService del frontend) ---
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

        // --- Docentes (el perfil completo de "Mi perfil" del frontend corresponde a Carlos Antonio Mendoza Ríos) ---
        Docente carlos = docente("Carlos Antonio", "Mendoza Ríos", "41829304", carlosU, "987 654 321", "Matemática y Ciencias");
        carlos.setColegiatura("CPPe N° 45812");
        carlos.setCodigoPlaza("PLZ-2026-9921");
        carlos.setCondicion("Nombrado");
        carlos.setNivelEducativo("Secundaria");
        carlos.setFechaIngreso(LocalDate.of(2018, 3, 15));
        Docente elena = docente("Elena", "Salazar Ríos", "41829305", elenaU, null, "Comunicación");
        Docente luis = docente("Luis", "Mendoza Castro", "41829306", luisU, null, "Matemática");
        Docente carmen = docente("Carmen", "Rojas Villa", "41829307", carmenU, null, "Personal Social");
        Docente jorge = docente("Jorge", "Paredes Lima", "41829308", jorgeU, null, "Ciencia y Tecnología");
        Docente rosa = docente("Rosa", "Huamán Quispe", "41829309", rosaU, null, "Educación Física");
        Docente diana = docente("Diana", "Quispe", "41829310", null, null, "Arte y Cultura");
        Docente mario = docente("Mario", "Torres", "41829311", null, null, "Inglés");
        Docente ana = docente("Ana", "Castillo", "41829312", null, null, "Educación Religiosa");

        // --- Secciones por clave "Nivel|grado|letra" ---
        Map<String, Seccion> secciones = new HashMap<>();
        em.createQuery("select s from Seccion s join fetch s.grado g join fetch g.nivel", Seccion.class).getResultList()
                .forEach(s -> secciones.put(s.getNivelNombre() + "|" + s.getGrado().getCodigo() + "|" + s.getLetra(), s));

        // --- Estudiantes: 12 por aula (como crearEstudiantes del frontend) + el alumno de la demo de ingreso ---
        List<Estudiante> estudiantes = new ArrayList<>();
        Map<Long, Seccion> seccionDe = new HashMap<>();
        int numero = 0;
        List<Seccion> ordenadas = em.createQuery("select s from Seccion s join fetch s.grado g join fetch g.nivel n order by n.orden, g.orden, s.letra",
                Seccion.class).getResultList();
        for (Seccion seccion : ordenadas) {
            for (String nombre : NOMBRES_DEMO) {
                numero++;
                Estudiante e = estudiante(nombre, String.format("%08d", 70_000_000 + numero), seccion, anio);
                estudiantes.add(e);
                seccionDe.put(e.getId(), seccion);
            }
        }
        Seccion primeroA = secciones.get("Primaria|1|A");
        Estudiante juanPerez = estudiante("Juan Pérez Ugarte", "72819201", primeroA, anio);
        estudiantes.add(juanPerez);
        seccionDe.put(juanPerez.getId(), primeroA);
        // Cuentas de alumno vinculadas a los estudiantes de 1° Primaria A.
        estudiantes.stream().filter(e -> seccionDe.get(e.getId()) == primeroA).forEach(e -> {
            if (e.getNombreCompleto().equals("Lucía Bustamante Morales")) {
                e.setUsuario(luciaU);
                e.setCorreo(luciaU.getCorreo());
            } else if (e.getNombreCompleto().equals("Juan Alvarado Mendoza")) {
                e.setUsuario(juanU);
            }
        });

        // --- Cursos de 1° Primaria A con horario (alumno.data.ts del frontend) ---
        List<Asignacion> asignaciones = new ArrayList<>();
        asignaciones.add(asignacion("COM-1A", "Comunicación", primeroA, elena, "Aula 04", "Lunes 08:00-09:15", "Miércoles 09:15-10:30", "Jueves 11:00-12:30"));
        asignaciones.add(asignacion("MAT-1A", "Matemática", primeroA, luis, "Aula 04", "Lunes 09:15-10:30", "Martes 11:00-12:30", "Jueves 08:00-09:15", "Viernes 11:00-12:30"));
        asignaciones.add(asignacion("PS-1A", "Personal Social", primeroA, carmen, "Aula 04", "Martes 08:00-09:15", "Viernes 09:15-10:30"));
        asignaciones.add(asignacion("CYT-1A", "Ciencia y Tecnología", primeroA, jorge, "Aula 04", "Martes 09:15-10:30", "Miércoles 11:00-12:30", "Jueves 09:15-10:30"));
        asignaciones.add(asignacion("EF-1A", "Educación Física", primeroA, rosa, "Patio principal", "Miércoles 08:00-09:15", "Viernes 08:00-09:15"));
        asignaciones.add(asignacion("AYC-1A", "Arte y Cultura", primeroA, diana, "Aula 04", "Lunes 11:00-12:30", "Jueves 12:30-14:00"));
        asignaciones.add(asignacion("ING-1A", "Inglés", primeroA, mario, "Aula 04", "Martes 12:30-14:00", "Viernes 12:30-14:00"));
        asignaciones.add(asignacion("REL-1A", "Educación Religiosa", primeroA, ana, "Aula 04", "Lunes 12:30-14:00"));
        asignaciones.add(asignacion("TUT-1A", "Tutoría", primeroA, elena, "Aula 04", "Miércoles 12:30-14:00"));

        // --- Cursos del panel docente (docente.service.ts), repartidos para que ningún docente tenga horarios cruzados ---
        asignaciones.add(asignacion("PSI-5A", "Psicomotriz", secciones.get("Inicial|5 años|A"), diana, "Patio de Juegos / Aula 01", "Lunes 08:30-09:15", "Miércoles 08:30-09:15"));
        asignaciones.add(asignacion("COM-4B", "Comunicación integral", secciones.get("Inicial|4 años|B"), elena, "Aula Inicial 02", "Martes 09:30-10:15", "Jueves 09:30-10:15"));
        asignaciones.add(asignacion("MAT-3A", "Matemática", secciones.get("Primaria|3|A"), carmen, "Aula 201", "Lunes 08:00-09:30", "Miércoles 08:00-09:30"));
        asignaciones.add(asignacion("GEO-2B", "Personal Social", secciones.get("Primaria|2|B"), ana, "Aula 104", "Martes 08:00-09:30", "Jueves 08:00-09:30"));
        asignaciones.add(asignacion("EDF-4A", "Educación Física", secciones.get("Primaria|4|A"), rosa, "Losa Deportiva", "Miércoles 10:00-11:30"));
        asignaciones.add(asignacion("MAT-3B", "Matemática", secciones.get("Secundaria|3|B"), carlos, "Aula 202", "Martes 10:00-11:30", "Jueves 10:00-11:30"));
        asignaciones.add(asignacion("FIS-4A", "Física Elemental (Ciencia y Tecnología)", secciones.get("Secundaria|4|A"), jorge, "Laboratorio 1", "Viernes 08:00-11:00"));
        asignaciones.add(asignacion("ALG-5A", "Álgebra Avanzada", secciones.get("Secundaria|5|A"), carlos, "Aula 301", "Lunes 09:15-10:30", "Miércoles 09:15-10:30"));
        asignaciones.add(asignacion("DPCC-4B", "Desarrollo Personal, Ciudadanía y Cívica", secciones.get("Secundaria|4|B"), carlos, "Aula 205", "Lunes 11:30-13:00", "Miércoles 11:30-13:00"));
        asignaciones.add(asignacion("EPT-5B", "Educación para el Trabajo (Computación)", secciones.get("Secundaria|5|B"), carlos, "Aula de Cómputo", "Jueves 11:30-13:00"));
        asignaciones.add(asignacion("RM-3C", "Razonamiento Matemático", secciones.get("Secundaria|3|C"), carlos, "Aula 203", "Viernes 09:30-11:00"));
        em.flush();

        // --- Historial de asistencia diaria (días hábiles anteriores a hoy) ---
        int dias = propiedades.datosDemo().diasHistorial();
        List<LocalDate> fechas = dias <= 0 ? List.of()
                : Calendario.diasHabiles(dias + 1, hoy).stream().filter(f -> f.isBefore(hoy)).toList();
        Map<String, EstadoAsistencia> diario = new HashMap<>();
        List<Object[]> filasDiarias = new ArrayList<>();
        Timestamp ahora = Timestamp.valueOf(calendario.ahora());
        for (Estudiante e : estudiantes) {
            long perfil = hash(e.getDni()) % 12;
            double probabilidadFalta = perfil == 0 ? .2 : perfil == 1 ? .11 : .025;
            for (LocalDate fecha : fechas) {
                double azar = (hash(e.getDni() + "|" + fecha) % 1000) / 1000.0;
                EstadoAsistencia estado = azar < probabilidadFalta ? EstadoAsistencia.FALTA
                        : azar < probabilidadFalta + .05 ? EstadoAsistencia.TARDANZA
                        : azar < probabilidadFalta + .07 ? EstadoAsistencia.JUSTIFICADO
                        : EstadoAsistencia.PRESENTE;
                if (e == juanPerez && fecha.equals(fechas.get(fechas.size() - 1))) {
                    estado = EstadoAsistencia.FALTA;   // la justificación pendiente de la demo
                }
                diario.put(e.getId() + "|" + fecha, estado);
                filasDiarias.add(new Object[]{e.getId(), Date.valueOf(fecha), estado.getValor(), ahora});
            }
        }
        jdbc.batchUpdate("insert into asistencia_diaria (estudiante_id, fecha, estado, actualizado_en) values (?, ?, ?, ?)", filasDiarias);

        // --- Sesiones por curso en los días de su horario (estado tomado del registro diario) ---
        Map<Long, List<Estudiante>> porSeccion = new HashMap<>();
        estudiantes.forEach(e -> porSeccion.computeIfAbsent(seccionDe.get(e.getId()).getId(), k -> new ArrayList<>()).add(e));
        List<Object[]> filasCurso = new ArrayList<>();
        List<Object[]> faltasCurso = new ArrayList<>();
        for (Asignacion a : asignaciones) {
            Usuario registra = a.getDocente().getUsuario();
            for (LocalDate fecha : fechas) {
                DiaSemana dia = DiaSemana.desde(fecha.getDayOfWeek());
                if (a.getHorarios().stream().noneMatch(h -> h.getDia() == dia)) {
                    continue;
                }
                SesionClase sesion = new SesionClase();
                sesion.setAsignacion(a);
                sesion.setFecha(fecha);
                sesion.setRegistradoPor(registra);
                sesion.setActualizadoEn(fecha.atTime(14, 0));
                em.persist(sesion);
                em.flush();
                for (Estudiante e : porSeccion.getOrDefault(a.getSeccion().getId(), List.of())) {
                    EstadoAsistencia estado = diario.get(e.getId() + "|" + fecha);
                    filasCurso.add(new Object[]{sesion.getId(), e.getId(), estado.enCurso().getValor()});
                    if (estado == EstadoAsistencia.FALTA && a.getSeccion() == primeroA && e != juanPerez) {
                        faltasCurso.add(new Object[]{e, a, fecha, estado});
                    }
                }
            }
        }
        jdbc.batchUpdate("insert into asistencia_curso (sesion_id, estudiante_id, estado) values (?, ?, ?)", filasCurso);

        // --- Justificaciones pendientes (justificaciones de ejemplo del frontend) ---
        if (!fechas.isEmpty()) {
            justificacion(juanPerez, null, fechas.get(fechas.size() - 1), "Cita médica comunicada por la familia.", null, EstadoAsistencia.FALTA);
        }
        String[][] motivos = {
                {"Cita Médica Sanitas", "Adjunto constancia de atención médica emitida por la clínica."},
                {"Salud / Gripe fuerte", "Descanso médico por 48 horas."}
        };
        for (int i = 0; i < Math.min(motivos.length, faltasCurso.size()); i++) {
            Object[] f = faltasCurso.get(faltasCurso.size() - 1 - i);
            justificacion((Estudiante) f[0], (Asignacion) f[1], (LocalDate) f[2], motivos[i][0], motivos[i][1], (EstadoAsistencia) f[3]);
        }

        // --- Comunicados ---
        Asignacion mat3a = asignaciones.stream().filter(a -> a.getCodigo().equals("MAT-3A")).findFirst().orElseThrow();
        comunicado("Evaluación Parcial de Matemática", "Se recuerda a los estudiantes que el examen parcial se realizará este viernes.",
                Prioridad.ALTA, mat3a, carmenU, hoy.minusDays(3));
        Usuario admin = usuarioRepository.findByCorreoIgnoreCase("admin@ie20191.edu.pe").orElse(null);
        comunicado("Revisa tu asistencia", "Consulta periódicamente las asistencias de tus cursos y avisa al colegio si identificas un dato que deba revisarse.",
                Prioridad.MEDIA, null, admin, hoy.minusDays(7));
        comunicado("Justificaciones", "Las solicitudes de inasistencia requieren revisión del personal autorizado antes de actualizar el registro.",
                Prioridad.BAJA, null, admin, hoy.minusDays(7));

        em.flush();
        log.info("Datos de demostración cargados: {} estudiantes, {} cursos por sección, {} registros diarios, {} registros por curso.",
                estudiantes.size(), asignaciones.size(), filasDiarias.size(), filasCurso.size());
    }

    /** Los docentes registrados pasan a la tabla maestra del personal como personal activo. */
    private void cargarPersonal() {
        Set<String> varones = Set.of("Carlos Antonio", "Luis", "Jorge", "Mario");
        List<Docente> docentes = em.createQuery("select d from Docente d left join fetch d.usuario", Docente.class).getResultList();
        for (Docente d : docentes) {
            Personal p = new Personal();
            p.setNombres(d.getNombres());
            p.setApellidos(d.getApellidos());
            p.setDni(d.getDni());
            p.setCargo("Docente");
            p.setArea(d.getEspecialidad());
            p.setCorreo(d.getUsuario() == null ? null : d.getUsuario().getCorreo());
            p.setSexo(varones.contains(d.getNombres()) ? "M" : "F");
            p.setFechaIngreso(d.getFechaIngreso());
            p.setEstado("A");
            p.setCreatedAt(calendario.ahora());
            em.persist(p);
        }
        log.info("Personal cargado: {} docentes como personal activo.", docentes.size());
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

    private Docente docente(String nombres, String apellidos, String dni, Usuario usuario, String telefono, String especialidad) {
        Docente d = new Docente();
        d.setNombres(nombres);
        d.setApellidos(apellidos);
        d.setDni(dni);
        d.setUsuario(usuario);
        d.setTelefono(telefono);
        d.setEspecialidad(especialidad);
        em.persist(d);
        return d;
    }

    private Estudiante estudiante(String nombreCompleto, String dni, Seccion seccion, int anio) {
        String[] partes = pe.edu.vallegrande.app.util.Textos.separarNombreCompleto(nombreCompleto);
        Estudiante e = new Estudiante();
        e.setNombres(partes[0]);
        e.setApellidos(partes[1]);
        e.setDni(dni);
        e.setGenero(NOMBRES_FEMENINOS.contains(partes[0]) ? "F" : "M");
        e.setCreado(LocalDate.of(anio, 3, 1));
        em.persist(e);
        Matricula m = new Matricula();
        m.setEstudiante(e);
        m.setSeccion(seccion);
        m.setAnioAcademico(anio);
        m.setFechaMatricula(LocalDate.of(anio, 3, 1));
        m.setEstado(EstadoMatricula.ACTIVA);
        em.persist(m);
        return e;
    }

    /** Bloques en formato "Lunes 08:00-09:15". */
    private Asignacion asignacion(String codigo, String curso, Seccion seccion, Docente docente, String aula, String... bloques) {
        Asignacion a = new Asignacion();
        a.setCodigo(codigo);
        a.setCurso(em.createQuery("select c from Curso c where c.nombre = :n", Curso.class).setParameter("n", curso).getSingleResult());
        a.setSeccion(seccion);
        a.setDocente(docente);
        a.setAula(aula);
        List<Horario> horarios = new ArrayList<>();
        for (String bloque : bloques) {
            String[] partes = bloque.split(" ");
            String[] horas = partes[1].split("-");
            horarios.add(new Horario(DiaSemana.de(partes[0]), LocalTime.parse(horas[0]), LocalTime.parse(horas[1])));
        }
        a.reemplazarHorarios(horarios);
        em.persist(a);
        return a;
    }

    private void justificacion(Estudiante e, Asignacion a, LocalDate fecha, String motivo, String detalle, EstadoAsistencia previo) {
        Justificacion j = new Justificacion();
        j.setEstudiante(e);
        j.setAsignacion(a);
        j.setFecha(fecha);
        j.setMotivo(motivo);
        j.setDetalle(detalle);
        j.setEstado(EstadoJustificacion.PENDIENTE);
        j.setEstadoPrevio(previo);
        j.setFechaSolicitud(LocalDateTime.of(fecha, LocalTime.of(18, 0)));
        em.persist(j);
    }

    private void comunicado(String titulo, String contenido, Prioridad prioridad, Asignacion a, Usuario autor, LocalDate fecha) {
        Comunicado c = new Comunicado();
        c.setTitulo(titulo);
        c.setContenido(contenido);
        c.setPrioridad(prioridad);
        c.setAsignacion(a);
        c.setAutor(autor);
        c.setFechaCreacion(fecha);
        em.persist(c);
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

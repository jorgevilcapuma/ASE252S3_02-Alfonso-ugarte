import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, PerfilAcceso } from '../../../core/services/auth.service';

interface Mensaje {
  emisor: 'bot' | 'usuario';
  texto: string;
}

interface OpcionChat {
  pregunta: string;
  respuesta: string;
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent implements OnInit {
  private authService = inject(AuthService);

  abierto = signal(false);
  mensajes = signal<Mensaje[]>([]);
  opciones = signal<OpcionChat[]>([]);
  escribiendo = signal(false);

  ngOnInit() {
    const sesion = this.authService.sesion;
    // Si no hay sesión, por defecto será 'general'
    const perfil = sesion?.perfil || 'general'; 
    const nombre = sesion?.nombre?.split(' ')[0] || 'Usuario';

    this.cargarPreguntasPorRol(perfil, nombre);
  }

  toggleChat() {
    this.abierto.update(valor => !valor);
  }

  enviarPregunta(opcion: OpcionChat) {
    this.mensajes.update(msgs => [...msgs, { emisor: 'usuario', texto: opcion.pregunta }]);
    this.escribiendo.set(true);

    setTimeout(() => {
      this.escribiendo.set(false);
      this.mensajes.update(msgs => [...msgs, { emisor: 'bot', texto: opcion.respuesta }]);
    }, 1200);
  }

  private cargarPreguntasPorRol(perfil: PerfilAcceso | string, nombre: string) {
    const saludo = `¡Hola, ${nombre}! Soy el asistente virtual del Portal Ugartino. ¿En qué te puedo ayudar hoy?`;
    this.mensajes.set([{ emisor: 'bot', texto: saludo }]);

    if (perfil === 'docente') {
      // --- OPCIONES PARA DOCENTES ---
      this.opciones.set([
        { pregunta: '¿Cómo registro asistencia?', respuesta: 'Ve al menú "Registrar asistencia", selecciona tu curso y marca a los alumnos presentes, ausentes o tardes.' },
        { pregunta: '¿Cómo reviso justificaciones?', respuesta: 'Entra a "Justificaciones" y podrás aprobar o rechazar los documentos que suban los alumnos.' },
        { pregunta: '¿Cómo genero un reporte?', respuesta: 'Ve a la sección "Reportes", elige el mes y descarga el PDF con el resumen de asistencias.' }
      ]);
    } else if (perfil === 'administrador') {
      // --- OPCIONES PARA ADMINISTRADORES ---
      this.opciones.set([
        { pregunta: '¿Cómo creo un usuario?', respuesta: 'Ve al menú "Usuarios" y haz clic en el botón "Nuevo". Puedes registrar docentes o personal administrativo.' },
        { pregunta: '¿Cómo gestiono las secciones?', respuesta: 'En "Grados y secciones" puedes agregar o editar las aulas de la institución.' },
        { pregunta: '¿Dónde veo las alertas globales?', respuesta: 'En el menú "Reportes de alertas" verás a todos los alumnos con faltas consecutivas en la institución.' },
        { pregunta: '¿Cómo restauro un alumno?', respuesta: 'En la lista de estudiantes, activa el filtro "Mostrar eliminados" y haz clic en el botón de restaurar.' }
      ]);
    } else if (perfil === 'alumno' || perfil === 'estudiante') {
      // --- OPCIONES PARA ALUMNOS ---
      this.opciones.set([
        { pregunta: '¿Cómo justifico una falta?', respuesta: 'Ve al menú "Mis Justificaciones", haz clic en "Nueva solicitud", completa los datos y adjunta tu evidencia (PDF o foto).' },
        { pregunta: '¿Cómo veo mis faltas?', respuesta: 'En la sección "Mi Asistencia" puedes ver el historial de tus faltas, tardanzas y asistencias, así como tu porcentaje de asistencia.' },
        { pregunta: '¿Cómo reviso si aprobaron mi justificación?', respuesta: 'En "Mis Justificaciones" podrás ver el estado de tus solicitudes: Pendiente (en revisión), Aprobada o Rechazada (con la observación del docente).' },
        { pregunta: '¿Qué pasa si acumulo muchas faltas?', respuesta: 'Si acumulas 3 faltas en un periodo, el sistema generará una alerta. Es importante que justifiques tus inasistencias con tiempo.' }
      ]);
    } else {
      // --- OPCIONES GENERALES (Para Directores, Personal, o cualquier otro rol) ---
      this.opciones.set([
        { pregunta: '¿Cómo registro mi asistencia?', respuesta: 'Si eres personal de la institución, ve al menú "Asistencia del personal" para marcar tu entrada o salida.' },
        { pregunta: '¿Dónde veo los avisos importantes?', respuesta: 'Los comunicados oficiales aparecen en el panel principal (Dashboard) o en la sección de "Comunicados".' },
        { pregunta: '¿Cómo contacto a soporte técnico?', respuesta: 'Si tienes problemas con el sistema, escribe a soporte@ie20191.edu.pe o llama a la Dirección.' },
        { pregunta: '¿Cómo reviso las estadísticas generales?', respuesta: 'En el Dashboard principal puedes ver gráficos de asistencia global y reportes de alertas de toda la institución.' }
      ]);
    }
  }
}
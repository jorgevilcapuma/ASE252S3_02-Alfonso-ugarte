import * as R from './reglas';

describe('Reglas de validación', () => {
  it('nombres: solo letras (con tildes, ñ, apóstrofo y guion)', () => {
    expect(R.nombre('María-José D\'Ávila Ñaupari')).toBeNull();
    expect(R.nombre('Ju4n')).toBe(R.MENSAJE_NOMBRE);
    expect(R.nombre(' ')).toContain('obligatorios');
    expect(R.nombreCompleto('Pedro')).toBe('Ingresa nombres y apellidos.');
    expect(R.nombreCompleto('Pedro Ruiz')).toBeNull();
  });

  it('DNI, celular, teléfono y correo', () => {
    expect(R.dni('12345678')).toBeNull();
    expect(R.dni('1234567a')).not.toBeNull();
    expect(R.celularOpcional('')).toBeNull();
    expect(R.celularOpcional('987654321')).toBeNull();
    expect(R.celularOpcional('887654321')).not.toBeNull();
    expect(R.telefonoOpcional('01 234-5678')).toBeNull();
    expect(R.telefonoOpcional('abc')).not.toBeNull();
    expect(R.correoOpcional('')).toBeNull();
    expect(R.correoOpcional('ana@colegio')).toBe(R.MENSAJE_CORREO);
    expect(R.correo('ana@ie20191.edu.pe')).toBeNull();
  });

  it('fechas: no futuras y en días de clase', () => {
    expect(R.fechaNoFutura('2026-10-22', '2026-10-21')).not.toBeNull();
    expect(R.diaHabil('2026-10-17')).not.toBeNull();   // sábado
    expect(R.diaHabil('2026-10-16')).toBeNull();       // viernes
  });

  it('grados por nivel y sección de una letra', () => {
    expect(R.gradoEnNivel('Inicial', '4 años')).toBeNull();
    expect(R.gradoEnNivel('Inicial', '6')).not.toBeNull();
    expect(R.gradoEnNivel('Primaria', '6')).toBeNull();
    expect(R.gradoEnNivel('Primaria', '9')).toBe('En Primaria el grado va de 1 a 6.');
    expect(R.gradoEnNivel('Secundaria', '3ro')).toBeNull();
    expect(R.seccion('B')).toBeNull();
    expect(R.seccion('AB')).not.toBeNull();
  });
});

package pe.edu.vallegrande.app.model.enums;

import jakarta.persistence.AttributeConverter;

/** Guarda un catálogo {@link ValorTexto} como su texto (columna NVARCHAR con CHECK en la base). */
public abstract class ConvertidorValor<E extends Enum<E> & ValorTexto> implements AttributeConverter<E, String> {

    private final Class<E> tipo;
    private final String campo;

    protected ConvertidorValor(Class<E> tipo, String campo) {
        this.tipo = tipo;
        this.campo = campo;
    }

    @Override
    public String convertToDatabaseColumn(E valor) {
        return valor == null ? null : valor.getValor();
    }

    @Override
    public E convertToEntityAttribute(String texto) {
        return ValorTexto.desde(tipo, texto, campo);
    }
}

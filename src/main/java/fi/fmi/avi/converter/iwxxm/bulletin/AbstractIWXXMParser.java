package fi.fmi.avi.converter.iwxxm.bulletin;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;

public abstract class AbstractIWXXMParser<T, S> extends IWXXMConverterBase implements AviMessageSpecificConverter<T, S> {

    abstract protected Document parseAsDom(T input) throws ConversionException;

}

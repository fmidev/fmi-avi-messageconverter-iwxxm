package fi.fmi.avi.converter.iwxxm.bulletin;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;

public class GenericBulletinIWXXMStringParser extends AbstractGenericBulletinIWXXMParser<String> {

    @Override
    protected Document parseAsDom(final String input) throws ConversionException {
        return IWXXMConverterBase.parseStringToDOM(input);
    }
}

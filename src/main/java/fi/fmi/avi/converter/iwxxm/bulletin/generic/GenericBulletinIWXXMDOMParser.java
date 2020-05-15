package fi.fmi.avi.converter.iwxxm.bulletin.generic;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;

public class GenericBulletinIWXXMDOMParser extends AbstractGenericBulletinIWXXMParser<Document> {

    @Override
    protected Document parseAsDom(final Document input) throws ConversionException {
        return input;
    }
}

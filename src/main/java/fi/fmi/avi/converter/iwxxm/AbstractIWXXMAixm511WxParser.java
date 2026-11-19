package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/** Selects the AIXM 5.1.1 Wx JAXB context for the shared parsing pipeline. */
public abstract class AbstractIWXXMAixm511WxParser<T, S extends AviationWeatherMessageOrCollection>
        extends AbstractIWXXMJAXBParser<T, S> {

    @Override
    protected JAXBContext getJAXBContext() throws JAXBException {
        return AbstractIWXXMAixm511WxSerializer.getAixm511WxJAXBContext();
    }
}

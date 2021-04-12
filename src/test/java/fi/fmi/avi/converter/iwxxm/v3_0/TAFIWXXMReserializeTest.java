package fi.fmi.avi.converter.iwxxm.v3_0;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMReserializeTest;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.taf.TAF;

public class TAFIWXXMReserializeTest extends AbstractIWXXMReserializeTest<TAF> {
    @Override
    protected ConversionSpecification<TAF, String> getPojoToIwxxmConversionSpecification() {
        return IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING;
    }

    @Override
    protected ConversionSpecification<String, TAF> getIwxxmToPojoConversionSpecification() {
        return IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO;
    }

    @Test
    public void TAFEndToEndTest() throws IOException, SAXException {
        testReserialize("taf-A5-1.xml");
    }

    @Test
    public void TAFEndToEndCancellationTest() throws IOException, SAXException {
        testReserialize("taf-A5-2.xml");
    }
}

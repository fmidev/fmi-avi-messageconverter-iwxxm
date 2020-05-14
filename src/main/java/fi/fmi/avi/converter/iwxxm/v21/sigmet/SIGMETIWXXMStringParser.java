package fi.fmi.avi.converter.iwxxm.v21.sigmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

/**
 * Specialization of {@link AbstractSIGMETIWXXMParser} for String input.
 */
public class SIGMETIWXXMStringParser extends AbstractSIGMETIWXXMParser<String> {
     /**
     * Returns the TAF input message as A DOM Document.
     *
     * @param input
     *         the XML Document input as a String
     *
     * @return the input parsed as DOM
     *
     * @throws ConversionException
     *         if an exception occurs while converting input to DOM
     */
    @Override
    protected Document parseAsDom(final String input) throws ConversionException {
        return IWXXMConverterBase.parseStringToDOM(input);
    }

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SpaceWeatherAdvisoryType.class.getResourceAsStream("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
        schemaInfo.setSchematronRules(SpaceWeatherAdvisoryType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));
        return schemaInfo;
    }
}

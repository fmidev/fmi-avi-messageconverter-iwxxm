package fi.fmi.avi.converter.iwxxm.v21.metar;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

/**
 * Specialization of {@link AbstractMETARIWXXMParser} for DOM Document content.
 */
public class METARIWXXMDOMParser extends AbstractMETARIWXXMParser<Document> {

    /**
     * Returns the METAR input message as A DOM Document.
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
    protected Document parseAsDom(final Document input) throws ConversionException {
        return input;
    }

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SpaceWeatherAdvisoryType.class.getResourceAsStream("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
        schemaInfo.setSchematronRules(SpaceWeatherAdvisoryType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));
        return schemaInfo;
    }
}
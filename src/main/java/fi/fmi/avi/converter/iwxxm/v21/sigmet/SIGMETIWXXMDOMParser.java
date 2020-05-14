package fi.fmi.avi.converter.iwxxm.v21.sigmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

/**
 * Specialization of {@link AbstractSIGMETIWXXMParser} for DOM Document content.
 */
public class SIGMETIWXXMDOMParser extends AbstractSIGMETIWXXMParser<Document> {

    /**
     * This implementation simple passes the input Document through.
     *
     * @param input
     *         the raw input format
     *
     * @return the parsed DOM
     *
     */
    @Override
    protected Document parseAsDom(final Document input)  throws ConversionException {
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

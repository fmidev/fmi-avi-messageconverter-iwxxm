package fi.fmi.avi.converter.iwxxm.v21;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMParser;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

public abstract class AbstractIWXXM21Parser<T, S extends AviationWeatherMessageOrCollection> extends AbstractIWXXMParser<T, S> {

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(icao.iwxxm21.ReportType.class.getResourceAsStream("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
        schemaInfo.setSchematronRules(icao.iwxxm21.ReportType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));
        return schemaInfo;
    }
}

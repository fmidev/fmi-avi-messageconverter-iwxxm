package fi.fmi.avi.converter.iwxxm.v2025_2;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511FullParser;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Full;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

public abstract class AbstractIWXXM20252Parser<T, S extends AviationWeatherMessageOrCollection> extends AbstractIWXXMAixm511FullParser<T, S> {

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Full.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(icao.iwxxm2025_2.ReportType.class.getResource("/int/icao/iwxxm/2025_2/iwxxm.xsd"));
        schemaInfo.addSchematronRule(icao.iwxxm2025_2.ReportType.class.getResource("/schematron/xslt/int/icao/iwxxm/2025_2/rule/iwxxm.xsl"));
        return schemaInfo;
    }

}

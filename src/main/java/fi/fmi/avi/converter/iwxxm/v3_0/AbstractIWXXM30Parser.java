package fi.fmi.avi.converter.iwxxm.v3_0;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511WxParser;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Wx;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

public abstract class AbstractIWXXM30Parser<T, S extends AviationWeatherMessageOrCollection> extends AbstractIWXXMAixm511WxParser<T, S> {

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Wx.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(icao.iwxxm30.ReportType.class.getResource("/int/icao/iwxxm/3.0.0/iwxxm.xsd"));
        schemaInfo.addSchematronRule(icao.iwxxm30.ReportType.class.getResource("/schematron/xslt/int/icao/iwxxm/3.0.0/rule/iwxxm.xsl"));
        return schemaInfo;
    }

}

package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511WxParser;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Wx;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;


public abstract class AbstractIWXXM21Parser<T, S extends AviationWeatherMessageOrCollection> extends AbstractIWXXMAixm511WxParser<T, S> {

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Wx.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(icao.iwxxm21.ReportType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
        schemaInfo.addSchematronRule(icao.iwxxm21.ReportType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));
        return schemaInfo;
    }

}

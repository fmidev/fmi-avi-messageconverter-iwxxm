package fi.fmi.avi.converter.iwxxm.v3_0;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

public abstract class AbstractIWXXM30Serializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMSerializer<T, S> {
    private static IWXXMNamespaceContext nsCtx;

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SpaceWeatherAdvisoryType.class.getResourceAsStream("/int/icao/iwxxm/3.0.0/iwxxm.xsd"));
        schemaInfo.addSchematronRule(SpaceWeatherAdvisoryType.class.getResource("/schematron/xslt/int/icao/iwxxm/3.0.0/rule/iwxxm.xsl"));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/3.0", "http://schemas.wmo.int/iwxxm/3.0/iwxxm.xsd");
        return schemaInfo;
    }

    @Override
    protected IWXXMNamespaceContext getNamespaceContext() {
        return getNSContext();
    }

    private static synchronized IWXXMNamespaceContext getNSContext() {
        if (nsCtx == null) {
            nsCtx = new IWXXMNamespaceContext();
            nsCtx.setPrefix("http://icao.int/iwxxm/3.0", "iwxxm");
            nsCtx.setPrefix("http://icao.int/iwxxm/2.1", "iwxxm2");
        }
        return nsCtx;
    }

}

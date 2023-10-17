package fi.fmi.avi.converter.iwxxm.v3_0;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511WxSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

import java.util.List;

public abstract class AbstractIWXXM30Serializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMAixm511WxSerializer<T, S> {
    private static IWXXMNamespaceContext nsCtx;

    private static synchronized IWXXMNamespaceContext getNSContext() {
        if (nsCtx == null) {
            nsCtx = new IWXXMNamespaceContext();
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2.1", "iwxxm21");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/3.0", "iwxxm");
        }
        return nsCtx;
    }

    @Override
    public XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SpaceWeatherAdvisoryType.class.getResource("/int/icao/iwxxm/3.0.0/iwxxm.xsd"));
        schemaInfo.addSchematronRule(SpaceWeatherAdvisoryType.class.getResource("/schematron/xslt/int/icao/iwxxm/3.0.0/rule/iwxxm.xsl"));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/3.0", "http://schemas.wmo.int/iwxxm/3.0/iwxxm.xsd");
        return schemaInfo;
    }

    @Override
    protected IWXXMNamespaceContext getNamespaceContext() {
        return getNSContext();
    }

    protected static <E> E getFirstOrNull(final List<E> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

}

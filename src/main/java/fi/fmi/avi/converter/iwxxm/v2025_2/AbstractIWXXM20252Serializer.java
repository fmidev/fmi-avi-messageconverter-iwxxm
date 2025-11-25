package fi.fmi.avi.converter.iwxxm.v2025_2;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511FullSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Full;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import icao.iwxxm2025_2.SpaceWeatherAdvisoryType;

import java.util.List;

public abstract class AbstractIWXXM20252Serializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMAixm511FullSerializer<T, S> {

    private static final String IWXXM_2025_2_SCHEMA_PATH = "/int/icao/iwxxm/2025_2/iwxxm.xsd";
    private static final String IWXXM_2025_2_SCHEMATRON_RULE_PATH = "/schematron/xslt/int/icao/iwxxm/2025_2/rule/iwxxm.xsl";
    private static IWXXMNamespaceContext nsCtx;

    private static synchronized IWXXMNamespaceContext getNSContext() {
        if (nsCtx == null) {
            nsCtx = new IWXXMNamespaceContext();
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2.1", "iwxxm21");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/3.0", "iwxxm30");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2023-1", "iwxxm2023_1");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2025-2", "iwxxm");
        }
        return nsCtx;
    }

    protected static <E> E getFirstOrNull(final List<E> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @Override
    public XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Full.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SpaceWeatherAdvisoryType.class.getResource(IWXXM_2025_2_SCHEMA_PATH));
        schemaInfo.addSchematronRule(SpaceWeatherAdvisoryType.class.getResource(IWXXM_2025_2_SCHEMATRON_RULE_PATH));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/2025-2", "http://schemas.wmo.int/iwxxm/2025-2/iwxxm.xsd");
        return schemaInfo;
    }

    @Override
    protected IWXXMNamespaceContext getNamespaceContext() {
        return getNSContext();
    }

}

package fi.fmi.avi.converter.iwxxm.v2023_1;

import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511FullSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Full;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import icao.iwxxm20231.SIGMETType;

import java.util.List;

public abstract class AbstractIWXXM2023_1Serializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMAixm511FullSerializer<T, S> {

    private static final String IWXXM_2023_1_SCHEMA_PATH = "/int/icao/iwxxm/20231/iwxxm.xsd";
    private static final String IWXXM_2023_1_SCHEMATRON_RULE_PATH = "/schematron/xslt/int/icao/iwxxm/20231/rule/iwxxm.xsl";
    private static IWXXMNamespaceContext nsCtx;

    private static synchronized IWXXMNamespaceContext getNSContext() {
        if (nsCtx == null) {
            nsCtx = new IWXXMNamespaceContext();
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2.1", "iwxxm21");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/3.0", "iwxxm30");
            nsCtx.overrideNamespacePrefix("http://icao.int/iwxxm/2023-1", "iwxxm");
        }
        return nsCtx;
    }

    @Override
    public XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Full.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SIGMETType.class.getResource(IWXXM_2023_1_SCHEMA_PATH));
        schemaInfo.addSchematronRule(SIGMETType.class.getResource(IWXXM_2023_1_SCHEMATRON_RULE_PATH));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/2023-1", "http://schemas.wmo.int/iwxxm/2023-1/iwxxm.xsd");
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

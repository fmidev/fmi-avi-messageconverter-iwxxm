package fi.fmi.avi.converter.iwxxm.profile;

import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Full;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import wmo.collect2014.MeteorologicalBulletinType;

public class Aixm511FullSchemaProfile implements IWXXMSchemaProfile {

    @Override
    public XMLSchemaInfo createSchemaInfo(final boolean secureProcessing) {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Full.getInstance(), secureProcessing);
        schemaInfo.addSchemaSource(MeteorologicalBulletinType.class.getResource("/int/wmo/collect/1.2/collect.xsd"));
        schemaInfo.addSchematronRule(MeteorologicalBulletinType.class.getResource("/schematron/xslt/int/wmo/collect/1.2/rule/collect.xsl"));
        schemaInfo.addSchemaLocation("http://def.wmo.int/collect/2014", "http://schemas.wmo.int/collect/1.2/collect.xsd");
        return schemaInfo;
    }

}


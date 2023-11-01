package fi.fmi.avi.converter.iwxxm.bulletin;

import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolverAixm511Wx;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import wmo.collect2014.MeteorologicalBulletinType;

public abstract class AbstractBulletinIWXXMAixm511WxSerializer<T, U extends AviationWeatherMessage, S extends MeteorologicalBulletin<U>>
        extends AbstractBulletinIWXXMSerializer<T, U, S> {

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Wx.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(MeteorologicalBulletinType.class.getResource("/int/wmo/collect/1.2/collect.xsd"));
        schemaInfo.addSchematronRule(MeteorologicalBulletinType.class.getResource("/schematron/xslt/int/wmo/collect/1.2/rule/collect.xsl"));
        schemaInfo.addSchemaLocation("http://def.wmo.int/collect/2014", "http://schemas.wmo.int/collect/1.2/collect.xsd");
        return schemaInfo;
    }

}

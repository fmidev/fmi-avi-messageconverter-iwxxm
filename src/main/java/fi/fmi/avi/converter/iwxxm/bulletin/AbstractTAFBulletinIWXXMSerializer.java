package fi.fmi.avi.converter.iwxxm.bulletin;

import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import icao.iwxxm21.TAFType;

public abstract class AbstractTAFBulletinIWXXMSerializer<T> extends AbstractBulletinIWXXMSerializer<T, TAF, TAFType, TAFBulletin> {

    @Override
    protected Class<TAFType> getMessageJAXBClass() {
        return TAFType.class;
    }
}

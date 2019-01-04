package fi.fmi.avi.converter.iwxxm.bulletin;

import java.util.List;
import java.util.Optional;

import net.opengis.gml32.AbstractFeatureType;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import wmo.collect2014.MeteorologicalBulletinType;
import wmo.collect2014.MeteorologicalInformationMemberPropertyType;

public class IWXXMGenericBulletinScanner extends AbstractIWXXMScanner {
    public static List<ConversionIssue> collectMETARProperties(final MeteorologicalBulletinType input,
            final ReferredObjectRetrievalContext refCtx,
            final BulletinProperties properties, final ConversionHints hints) {
        IssueList retval = new IssueList();

        retval.addAll(collectHeading(input.getBulletinIdentifier(), properties, refCtx));
        for (MeteorologicalInformationMemberPropertyType metProp: input.getMeteorologicalInformation()) {
            Optional<AbstractFeatureType> metInfo = resolveProperty(metProp, "abstractFeature", AbstractFeatureType.class, refCtx);
            if (metInfo.isPresent()) {
                collectGenericAviationWeatherMessage(metInfo.get(), properties, refCtx);
            }
        }
        return retval;
    }

    private static IssueList collectHeading(final String bulletinIdentifier, final BulletinProperties properties, final ReferredObjectRetrievalContext refCtx) {
        IssueList retval = new IssueList();

        return retval;
    }

    private static IssueList collectGenericAviationWeatherMessage(final AbstractFeatureType feature, final BulletinProperties properties,
            final ReferredObjectRetrievalContext refCtx) {
        IssueList retval = new IssueList();
        Class<? extends AbstractFeatureType> clz = feature.getClass();

        return retval;
    }
}

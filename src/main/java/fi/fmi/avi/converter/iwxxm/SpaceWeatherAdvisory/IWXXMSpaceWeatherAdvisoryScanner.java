package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.AdvisoryNumberImpl;
import icao.iwxxm30.SpaceWeatherAdvisoryPropertyType;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

public class IWXXMSpaceWeatherAdvisoryScanner extends AbstractIWXXMScanner {
    private static final Logger LOG = LoggerFactory.getLogger(IWXXMSpaceWeatherAdvisoryScanner.class);

    public static List<ConversionIssue> collectTAFProperties(final SpaceWeatherAdvisoryType input, final ReferredObjectRetrievalContext refCtx,
            final SpaceWeatherAdvisoryPropertyType properties,
            final ConversionHints hints) {
        IssueList retval = new IssueList();

        String advisoryNumber = input.getAdvisoryNumber().getValue();
        if(!advisoryNumber.isEmpty()) {
            String[] splitAdvisoryNumber = advisoryNumber.split("/");
            AdvisoryNumber an =
                    AdvisoryNumberImpl.builder()
                            .setYear(Integer.parseInt(splitAdvisoryNumber[0]))
                            .setSerialNumber(Integer.parseInt(splitAdvisoryNumber[1]))
                            .build();



        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Status is missing"));
        }



        return retval;
    }
}

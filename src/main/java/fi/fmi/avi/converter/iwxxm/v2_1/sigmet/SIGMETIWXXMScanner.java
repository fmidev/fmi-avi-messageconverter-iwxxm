package fi.fmi.avi.converter.iwxxm.v2_1.sigmet;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.AviationCodeListUser;
import icao.iwxxm21.SIGMETReportStatusType;
import icao.iwxxm21.SIGMETType;

public class SIGMETIWXXMScanner extends AbstractIWXXMScanner {
    private static final Logger LOG = LoggerFactory.getLogger(SIGMETIWXXMScanner.class);

    public static List<ConversionIssue> collectSIGMETProperties(final SIGMETType input, final ReferredObjectRetrievalContext refCtx,
            final SIGMETProperties properties, final ConversionHints hints) {
        IssueList retval = new IssueList();

        SIGMETReportStatusType status = input.getStatus();
        if (status != null) {
            properties.set(SIGMETProperties.Name.STATUS, AviationCodeListUser.SigmetAirmetReportStatus.valueOf(status.name()));
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Status is missing"));
        }

        return retval;
    }

}

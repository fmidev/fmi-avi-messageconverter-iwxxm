package fi.fmi.avi.converter.iwxxm.metar;

import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import icao.iwxxm21.METARType;

/**
 * Created by rinne on 25/07/2018.
 */
public class IWXXMMETARScanner extends AbstractIWXXMScanner {

    public static List<ConversionIssue> collectMETARProperties(final METARType input, final ReferredObjectRetrievalContext refCtx,
            final METARProperties properties, final ConversionHints hints) {
        IssueList retval = new IssueList();

        //TODO: the actual validation and property collection
        return retval;
    }
}

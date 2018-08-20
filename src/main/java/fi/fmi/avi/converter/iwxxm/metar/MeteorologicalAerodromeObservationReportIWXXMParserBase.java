package fi.fmi.avi.converter.iwxxm.metar;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMParser;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import icao.iwxxm21.MeteorologicalAerodromeObservationReportType;

/**
 * Created by rinne on 01/08/2018.
 */
public abstract class MeteorologicalAerodromeObservationReportIWXXMParserBase<T, S extends MeteorologicalTerminalAirReport> extends AbstractIWXXMParser<T, S> {

    protected METARImpl.Builder getBuilder(final MeteorologicalAerodromeObservationReportType input, final ReferredObjectRetrievalContext refCtx,
            final ConversionResult<? extends MeteorologicalTerminalAirReport> result, final ConversionHints hints) {
        METARProperties properties = new METARProperties(input);

        //Other specific validation (using JAXB elements)
        result.addIssue(IWXXMMETARScanner.collectMETARProperties(input, refCtx, properties, hints));

        //Build the METAR:
        Optional<AviationCodeListUser.MetarStatus> status = properties.get(METARProperties.Name.STATUS, AviationCodeListUser.MetarStatus.class);
        if (!status.isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "METAR status not known, unable to " + "proceed"));
            return null;
        }
        METARImpl.Builder metarBuilder = new METARImpl.Builder();
        metarBuilder.setStatus(status.get());

        METARProperties metarProperties = new METARProperties(input);
        result.addIssue(IWXXMMETARScanner.collectMETARProperties(input, refCtx, metarProperties, hints));

        //TODO: build from properties

        return metarBuilder;
    }
}

package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimePeriodPropertyType;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.taf.TAF;
import icao.iwxxm30.ReportStatusType;
import icao.iwxxm30.TAFType;

public abstract class TAFIWXXMSerializer<T> extends AbstractIWXXM30Serializer<TAF, T> {

    protected abstract T render(TAFType taf, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;


    @Override
    public ConversionResult<T> convertMessage(final TAF input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        if(!checkCompleteTimeReferences(input, result)) {
            return result;
        }

        checkAerodromeReferencePositions(input,result);

        final String issueTimeId = "time-" + UUID.randomUUID().toString();
        final String validTimeId = "time-" + UUID.randomUUID().toString();
        final String foiId = "foi-" + UUID.randomUUID().toString();
        final String processId = "process-" + UUID.randomUUID().toString();
        final String aerodromeId = "ad-" + UUID.randomUUID().toString();

        TAFType taf = create(TAFType.class);
        taf.setId("taf-" + UUID.randomUUID().toString());

        final AviationCodeListUser.TAFStatus status = input.getStatus();
        taf.setReportStatus(ReportStatusType.valueOf(status.name()));

        if (input.getIssueTime().get().getCompleteTime().isPresent()) {
            taf.setIssueTime(create(TimeInstantPropertyType.class, (prop) -> {
                createTimeInstantProperty(input, prop, issueTimeId);
            }));
        }



        if (AviationCodeListUser.TAFStatus.MISSING != status) {
            if (input.getValidityTime().isPresent()) {
                final Optional<PartialOrCompleteTimeInstant> start = input.getValidityTime().get().getStartTime();
                final Optional<PartialOrCompleteTimeInstant> end = input.getValidityTime().get().getEndTime();
                if (!start.isPresent() || !end.isPresent()) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is missing start or end"));
                    return result;
                }
                if (!start.get().getCompleteTime().isPresent() || !end.get().getCompleteTime().isPresent()) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is not a fully qualified time period"));
                    return result;
                }
                taf.setValidPeriod(create(TimePeriodPropertyType.class, (prop) -> {
                    createTimePeriodPropertyType(prop, start.get(), end.get(), validTimeId);
                }));
            }
            //this.updateChangeForecast(input, taf, issueTimeId, validTimeId, foiId, processId, result);
        }






        try {
            //this.updateMessageMetadata(input, result, taf);
            final T rendered = this.render(taf, hints);
            //result.addIssue(validate(rendered, getSchemaInfo(), hints));
            System.out.print(rendered);
            //result.setConvertedMessage(rendered);
            result.setConvertedMessage(rendered);

        } catch (final Exception e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        final InputStream retval = this.getClass().getResourceAsStream("TAF30CleanUp.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    public static class ToDOM extends TAFIWXXMSerializer<Document> {

        @Override
        protected Document render(final TAFType taf, final ConversionHints hints) throws ConversionException {
            return this.renderXMLDocument(taf, hints);
        }

        @Override
        protected IssueList validate(final Document output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return TAFIWXXMSerializer.validateDOMAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }

    public static class ToString extends TAFIWXXMSerializer<String> {
        @Override
        protected String render(final TAFType taf, final ConversionHints hints) throws ConversionException {
            final Document result = renderXMLDocument(taf, hints);
            return renderDOMToString(result, hints);
        }

        @Override
        protected IssueList validate(final String output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return TAFIWXXMSerializer.validateStringAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }
}

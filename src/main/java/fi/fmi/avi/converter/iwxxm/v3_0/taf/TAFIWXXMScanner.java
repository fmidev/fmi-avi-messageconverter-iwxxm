package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Scanner;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherAdvisoryProperties;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import icao.iwxxm30.TAFType;

public class TAFIWXXMScanner extends AbstractIWXXM30Scanner {
    public static List<ConversionIssue> collectTAFProperties(final TAFType input, final ReferredObjectRetrievalContext refCtx,
            final TAFProperties properties, final ConversionHints hints) {
        final IssueList issueList = new IssueList();

        final GenericReportProperties meta = new GenericReportProperties();
        issueList.addAll(AbstractIWXXM30Scanner.collectReportMetadata(input, meta, hints));
        properties.set(SpaceWeatherAdvisoryProperties.Name.REPORT_METADATA, meta);

        if(input.getIssueTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> issueTime = getCompleteTimeInstant(input.getIssueTime(), refCtx);
            if (!issueTime.isPresent()) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Issue time is not valid"));
            } else {
                properties.set(TAFProperties.Name.ISSUE_TIME, issueTime.get());
            }
        }

        if (input.getValidPeriod() != null) {
            final Optional<ZonedDateTime> startTimeZoned = getStartTime(input.getValidPeriod().getTimePeriod(), refCtx);
            final Optional<ZonedDateTime> endTimeZoned = getEndTime(input.getValidPeriod().getTimePeriod(), refCtx);
            if (startTimeZoned.isPresent() && endTimeZoned.isPresent()) {
                Optional<PartialOrCompleteTimePeriod> startTime = Optional.of(PartialOrCompleteTimeInstant.of(startTimeZoned.get()));
                Optional<PartialOrCompleteTimeInstant> endTime = Optional.of(PartialOrCompleteTimeInstant.of(startTimeZoned.get()));

                final Optional<PartialOrCompleteTimePeriod> validPeriod = PartialOrCompleteTimePeriod.builder()
                        .setStartTime(startTime.get())
                        .setEndTime(endTime.get())
                        .build();
            }

            System.out.print("Valid period ");
            System.out.println("*****************");
        }

        if (input.getAerodrome() != null) {
            System.out.print("Aerodrome ");
            System.out.println("*****************");
        }

        if (input.getBaseForecast()!= null) {
            System.out.print("Base forecast ");
            System.out.println("*****************");
        }

        if (input.getChangeForecast()!= null) {
            System.out.print("Change forecast ");
            System.out.println("*****************");
        }

        if (input.getCancelledReportValidPeriod()!= null) {
            System.out.print("Cancel message ");
            System.out.println("*****************");
        }

        return issueList;
    }
}

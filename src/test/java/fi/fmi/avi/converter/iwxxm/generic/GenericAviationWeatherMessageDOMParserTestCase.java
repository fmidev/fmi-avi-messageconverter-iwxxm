package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;

import java.util.*;

public final class GenericAviationWeatherMessageDOMParserTestCase {

    private final String fileName;
    private final String namespace;
    private final MessageType messageType;
    private final ConversionHints hints;
    private final boolean translated;
    private final ReportStatus reportStatus;
    private final boolean nil;
    private final String issueTime;
    private final String observationTime;
    private final String validityStart;
    private final String validityEnd;
    private final Map<LocationIndicatorType, String> locationIndicators;
    private final List<ExpectedIssue> expectedIssues;

    private GenericAviationWeatherMessageDOMParserTestCase(final Builder builder) {
        this.fileName = Objects.requireNonNull(builder.fileName, "fileName");
        this.namespace = Objects.requireNonNull(builder.namespace, "namespace");
        this.messageType = Objects.requireNonNull(builder.messageType, "messageType");
        this.hints = builder.hints != null ? builder.hints : new ConversionHints();
        this.translated = builder.translated;
        this.reportStatus = builder.reportStatus;
        this.nil = builder.nil;
        this.issueTime = builder.issueTime;
        this.observationTime = builder.observationTime;
        this.validityStart = builder.validityStart;
        this.validityEnd = builder.validityEnd;
        this.locationIndicators = builder.locationIndicators != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.locationIndicators))
                : Collections.emptyMap();
        this.expectedIssues = builder.expectedIssues != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.expectedIssues))
                : Collections.emptyList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFileName() {
        return fileName;
    }

    public String getNamespace() {
        return namespace;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public ConversionHints getHints() {
        return hints;
    }

    public boolean isTranslated() {
        return translated;
    }

    public ReportStatus getReportStatus() {
        return reportStatus;
    }

    public boolean hasReportStatus() {
        return reportStatus != null;
    }

    public boolean isNil() {
        return nil;
    }

    public String getIssueTime() {
        return issueTime;
    }

    public boolean hasIssueTime() {
        return issueTime != null;
    }

    public boolean hasObservationTime() {
        return observationTime != null;
    }

    public String getObservationTime() {
        return observationTime;
    }

    public boolean hasValidityPeriod() {
        return validityStart != null && validityEnd != null;
    }

    public String getValidityStart() {
        return validityStart;
    }

    public String getValidityEnd() {
        return validityEnd;
    }

    public Map<LocationIndicatorType, String> getLocationIndicators() {
        return locationIndicators;
    }

    public boolean expectsIssues() {
        return !expectedIssues.isEmpty();
    }

    public List<ExpectedIssue> getExpectedIssues() {
        return expectedIssues;
    }

    @Override
    public String toString() {
        return fileName;
    }

    public static final class ExpectedIssue {
        private final ConversionIssue.Severity severity;
        private final ConversionIssue.Type type;
        private final String messageSubstring;

        private ExpectedIssue(final ConversionIssue.Severity severity,
                              final ConversionIssue.Type type,
                              final String messageSubstring) {
            this.severity = severity;
            this.type = type;
            this.messageSubstring = messageSubstring;
        }

        public static ExpectedIssue of(final ConversionIssue.Severity severity,
                                       final ConversionIssue.Type type,
                                       final String messageSubstring) {
            return new ExpectedIssue(severity, type, messageSubstring);
        }

        public static ExpectedIssue of(final ConversionIssue.Severity severity,
                                       final ConversionIssue.Type type) {
            return new ExpectedIssue(severity, type, null);
        }

        public ConversionIssue.Severity getSeverity() {
            return severity;
        }

        public ConversionIssue.Type getType() {
            return type;
        }

        public String getMessageSubstring() {
            return messageSubstring;
        }

        public boolean hasMessageSubstring() {
            return messageSubstring != null;
        }
    }

    public static final class Builder {
        private String fileName;
        private String namespace;
        private MessageType messageType;
        private ConversionHints hints;
        private boolean translated;
        private ReportStatus reportStatus = ReportStatus.NORMAL;
        private String issueTime;
        private String observationTime;
        private String validityStart;
        private String validityEnd;
        private Map<LocationIndicatorType, String> locationIndicators;
        private List<ExpectedIssue> expectedIssues;
        private boolean nil;

        private Builder() {
        }

        public Builder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder namespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder messageType(final MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder hints(final ConversionHints hints) {
            this.hints = hints;
            return this;
        }

        public Builder translated(final boolean translated) {
            this.translated = translated;
            return this;
        }

        public Builder translated() {
            return translated(true);
        }

        public Builder reportStatus(final ReportStatus reportStatus) {
            this.reportStatus = reportStatus;
            return this;
        }

        public Builder issueTime(final String issueTime) {
            this.issueTime = issueTime;
            return this;
        }

        public Builder observationTime(final String observationTime) {
            this.observationTime = observationTime;
            return this;
        }

        public Builder validityPeriod(final String start, final String end) {
            this.validityStart = start;
            this.validityEnd = end;
            return this;
        }

        public Builder noValidityPeriod() {
            this.validityStart = null;
            this.validityEnd = null;
            return this;
        }

        public Builder locationIndicator(final LocationIndicatorType type, final String value) {
            if (this.locationIndicators == null) {
                this.locationIndicators = new HashMap<>();
            }
            this.locationIndicators.put(type, value);
            return this;
        }

        public Builder aerodrome(final String icaoCode) {
            return locationIndicator(LocationIndicatorType.AERODROME, icaoCode);
        }

        public Builder issuingCentre(final String name) {
            return locationIndicator(LocationIndicatorType.ISSUING_CENTRE, name);
        }

        public Builder nil(final boolean nil) {
            this.nil = nil;
            return this;
        }

        public Builder sigmetLocationIndicators(final String atsUnit, final String atsRegion, final String mwo) {
            return locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, atsUnit)
                    .locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, atsRegion)
                    .locationIndicator(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, mwo);
        }

        public Builder expectedIssue(final ConversionIssue.Severity severity,
                                     final ConversionIssue.Type type,
                                     final String messageSubstring) {
            if (this.expectedIssues == null) {
                this.expectedIssues = new ArrayList<>();
            }
            this.expectedIssues.add(ExpectedIssue.of(severity, type, messageSubstring));
            return this;
        }

        public GenericAviationWeatherMessageDOMParserTestCase build() {
            return new GenericAviationWeatherMessageDOMParserTestCase(this);
        }
    }
}


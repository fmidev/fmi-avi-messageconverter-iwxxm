package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GenericAviationWeatherMessageDOMParserTestCase {

    private final String fileName;
    private final String namespace;
    private final MessageType messageType;
    private final ConversionHints hints;
    private final boolean translated;
    private final ReportStatus reportStatus;
    private final String issueTime;
    private final String validityStart;
    private final String validityEnd;
    private final Map<LocationIndicatorType, String> locationIndicators;

    private GenericAviationWeatherMessageDOMParserTestCase(final Builder builder) {
        this.fileName = Objects.requireNonNull(builder.fileName, "fileName");
        this.namespace = Objects.requireNonNull(builder.namespace, "namespace");
        this.messageType = Objects.requireNonNull(builder.messageType, "messageType");
        this.hints = builder.hints != null ? builder.hints : new ConversionHints();
        this.translated = builder.translated;
        this.reportStatus = Objects.requireNonNull(builder.reportStatus, "reportStatus");
        this.issueTime = Objects.requireNonNull(builder.issueTime, "issueTime");
        this.validityStart = builder.validityStart;
        this.validityEnd = builder.validityEnd;
        this.locationIndicators = builder.locationIndicators != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.locationIndicators))
                : Collections.emptyMap();
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

    public String getIssueTime() {
        return issueTime;
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

    @Override
    public String toString() {
        return fileName;
    }

    public static final class Builder {
        private String fileName;
        private String namespace;
        private MessageType messageType;
        private ConversionHints hints;
        private boolean translated;
        private ReportStatus reportStatus = ReportStatus.NORMAL;
        private String issueTime;
        private String validityStart;
        private String validityEnd;
        private Map<LocationIndicatorType, String> locationIndicators;

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

        public Builder sigmetLocationIndicators(final String atsUnit, final String atsRegion, final String mwo) {
            return locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, atsUnit)
                    .locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, atsRegion)
                    .locationIndicator(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, mwo);
        }

        public GenericAviationWeatherMessageDOMParserTestCase build() {
            return new GenericAviationWeatherMessageDOMParserTestCase(this);
        }
    }
}


package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A configurable, version-agnostic generic IWXXM scanner that can be configured
 * declaratively for any message type.
 * <p>
 * Configuration options:
 * <ul>
 *   <li>A message type resolver (fixed or dynamic based on element name)</li>
 *   <li>Whether report status is required or optional</li>
 *   <li>Whether to extract validity time</li>
 *   <li>Which location indicators to extract</li>
 *   <li>A field XPath provider for the message type</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * GenericIWXXMScanner tafScanner = GenericIWXXMScanner.builder()
 *         .fieldProvider(new TAFFieldXPathProvider())
 *         .messageType(MessageType.TAF)
 *         .requireReportStatus(true)
 *         .extractValidityTime(true)
 *         .locationIndicator(LocationIndicatorType.AERODROME, IWXXMField.AERODROME)
 *         .build();
 * </pre>
 */
public class GenericIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    private final Function<Element, MessageType> messageTypeResolver;
    private final boolean requireReportStatus;
    private final boolean extractValidityTime;
    private final boolean extractObservationTime;
    private final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> locationIndicatorFields;

    private GenericIWXXMScanner(final Builder builder) {
        super(builder.fieldXPathProvider);
        this.messageTypeResolver = Objects.requireNonNull(builder.messageTypeResolver, "messageTypeResolver");
        this.requireReportStatus = builder.requireReportStatus;
        this.extractValidityTime = builder.extractValidityTime;
        this.extractObservationTime = builder.extractObservationTime;
        this.locationIndicatorFields = Collections.unmodifiableMap(
                new EnumMap<>(builder.locationIndicatorFields));
    }

    /**
     * Creates a new builder for configuring a GenericIWXXMScanner.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder) {
        final IssueList issues = new IssueList();

        final MessageType messageType = messageTypeResolver.apply(featureElement);
        if (messageType == null) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.SYNTAX,
                    "Unable to determine message type from element: " + featureElement.getLocalName()));
            return issues;
        }
        builder.setMessageType(messageType);

        if (requireReportStatus) {
            collectReportStatus(featureElement, xpath, builder, issues);
        } else {
            collectOptionalReportStatus(featureElement, xpath, builder, issues);
        }

        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, issues);

        if (extractObservationTime) {
            collectObservationTimeUsingFieldProvider(featureElement, xpath, builder, issues);
        }

        if (extractValidityTime) {
            collectValidityTimeUsingFieldProvider(featureElement, xpath, builder, issues);
        }

        if (!locationIndicatorFields.isEmpty()) {
            collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder,
                    locationIndicatorFields, issues);
        }

        return issues;
    }

    /**
     * Builder for creating configured GenericIWXXMScanner instances.
     */
    public static class Builder {
        private final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> locationIndicatorFields =
                new EnumMap<>(GenericAviationWeatherMessage.LocationIndicatorType.class);
        private FieldXPathProvider fieldXPathProvider;
        private Function<Element, MessageType> messageTypeResolver;
        private boolean requireReportStatus = true;
        private boolean extractValidityTime = false;
        private boolean extractObservationTime = false;

        private Builder() {
        }

        /**
         * Sets the field XPath provider for this scanner.
         * This is required.
         *
         * @param provider the field XPath provider
         * @return this builder
         */
        public Builder fieldProvider(final FieldXPathProvider provider) {
            this.fieldXPathProvider = Objects.requireNonNull(provider, "fieldXPathProvider");
            return this;
        }

        /**
         * Sets a fixed message type for this scanner.
         * Use this when the scanner handles only one message type.
         *
         * @param messageType the fixed message type
         * @return this builder
         */
        public Builder messageType(final MessageType messageType) {
            Objects.requireNonNull(messageType, "messageType");
            this.messageTypeResolver = element -> messageType;
            return this;
        }

        /**
         * Sets a dynamic message type resolver for this scanner.
         * Use this when the scanner handles multiple message types (e.g. METAR/SPECI).
         *
         * @param resolver function that determines message type from the root element
         * @return this builder
         */
        public Builder messageTypeResolver(final Function<Element, MessageType> resolver) {
            this.messageTypeResolver = Objects.requireNonNull(resolver, "messageTypeResolver");
            return this;
        }

        /**
         * Sets whether report status is required.
         * If true (default), an error is added when report status cannot be parsed.
         * If false, missing report status is silently ignored (for VAA/TCA in IWXXM 2.1).
         *
         * @param required whether report status is required
         * @return this builder
         */
        public Builder requireReportStatus(final boolean required) {
            this.requireReportStatus = required;
            return this;
        }

        /**
         * Sets whether to extract validity time.
         * Default is false.
         *
         * @param extract whether to extract validity time
         * @return this builder
         */
        public Builder extractValidityTime(final boolean extract) {
            this.extractValidityTime = extract;
            return this;
        }

        /**
         * Sets whether to extract observation time.
         * Default is false. Should be enabled for METAR/SPECI messages.
         *
         * @param extract whether to extract observation time
         * @return this builder
         */
        public Builder extractObservationTime(final boolean extract) {
            this.extractObservationTime = extract;
            return this;
        }

        /**
         * Adds a location indicator to extract.
         *
         * @param type  the location indicator type
         * @param field the IWXXM field that provides the XPath for this location indicator
         * @return this builder
         */
        public Builder locationIndicator(final GenericAviationWeatherMessage.LocationIndicatorType type,
                                         final IWXXMField field) {
            this.locationIndicatorFields.put(
                    Objects.requireNonNull(type, "type"),
                    Objects.requireNonNull(field, "field"));
            return this;
        }

        /**
         * Builds the configured GenericIWXXMScanner.
         *
         * @return a new GenericIWXXMScanner instance
         * @throws NullPointerException if required fields are not set
         */
        public GenericIWXXMScanner build() {
            Objects.requireNonNull(fieldXPathProvider, "fieldProvider must be set");
            Objects.requireNonNull(messageTypeResolver, "messageType or messageTypeResolver must be set");
            return new GenericIWXXMScanner(this);
        }
    }
}


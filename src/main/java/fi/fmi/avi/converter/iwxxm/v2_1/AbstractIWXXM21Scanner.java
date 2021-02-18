package fi.fmi.avi.converter.iwxxm.v2_1;

import static fi.fmi.avi.model.immutable.WeatherImpl.WEATHER_CODES;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.PointType;
import net.opengis.gml32.ReferenceType;
import net.opengis.om20.OMObservationType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import aero.aixm511.AirportHeliportType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;
import icao.iwxxm21.CloudAmountReportedAtAerodromeType;
import icao.iwxxm21.CloudLayerPropertyType;
import icao.iwxxm21.CloudLayerType;
import icao.iwxxm21.DistanceWithNilReasonType;
import icao.iwxxm21.MeteorologicalAerodromeForecastRecordType;
import icao.iwxxm21.MeteorologicalAerodromeObservationRecordType;
import icao.iwxxm21.MeteorologicalAerodromeTrendForecastRecordType;
import icao.iwxxm21.RelationalOperatorType;
import icao.iwxxm21.ReportType;
import icao.iwxxm21.SigConvectiveCloudTypeType;
import wmo.metce2013.ProcessType;

public class AbstractIWXXM21Scanner extends AbstractIWXXMScanner {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIWXXM21Scanner.class);

    public static IssueList collectReportMetadata(final ReportType input, final GenericReportProperties properties, final ConversionHints hints) {
        final IssueList retval = new IssueList();

        //Issues for the permissibleUsage reported already by XML Schema or Schematron validation, so not checking them here:
        if (input.getPermissibleUsage() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.valueOf(input.getPermissibleUsage().name()));
        }
        if (input.getPermissibleUsageReason() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON,
                    AviationCodeListUser.PermissibleUsageReason.valueOf(input.getPermissibleUsageReason().name()));
        }
        properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, input.getPermissibleUsageSupplementary());
        properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, input.getTranslatedBulletinID());

        XMLGregorianCalendar cal = input.getTranslatedBulletinReceptionTime();
        if (cal != null) {
            final ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, time);
        }

        cal = input.getTranslationTime();
        if (cal != null) {
            final ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATION_TIME, time);
        }

        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, input.getTranslationCentreDesignator());
        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, input.getTranslationCentreName());
        properties.set(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, input.getTranslationFailedTAC());

        if (!properties.contains(GenericReportProperties.Name.TRANSLATION_TIME)) {
            if (hints != null && hints.containsKey(ConversionHints.KEY_TRANSLATION_TIME)) {
                final Object value = hints.get(ConversionHints.KEY_TRANSLATION_TIME);
                if (ConversionHints.VALUE_TRANSLATION_TIME_AUTO.equals(value)) {
                    properties.set(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.now());
                } else if (value instanceof ZonedDateTime) {
                    properties.set(GenericReportProperties.Name.TRANSLATION_TIME, value);
                }
            }
        }
        return retval;
    }

    public static IssueList collectCommonObsMetadata(final OMObservationType observation, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final String contextPath, final ConversionHints hints) {
        final IssueList retval = new IssueList();
        //forecast type
        if (observation.getType() != null) {
            properties.set(OMObservationProperties.Name.TYPE, observation.getType().getHref());
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Observation type is missing in " + contextPath));
        }
        //result time
        if (observation.getResultTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> resultTime = getCompleteTimeInstant(observation.getResultTime(), refCtx);
            if (resultTime.isPresent() && resultTime.get().getCompleteTime().isPresent()) {
                properties.set(OMObservationProperties.Name.RESULT_TIME, resultTime.get());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Result time is not valid or cannot be resolved in " + contextPath));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Result time is missing in " + contextPath));
        }

        //validTime
        if (observation.getValidTime() != null) {
            final Optional<PartialOrCompleteTimePeriod> validTime = getCompleteTimePeriod(observation.getValidTime(), refCtx);
            validTime.ifPresent((time) -> {
                properties.set(OMObservationProperties.Name.VALID_TIME, time);
            });
        }

        //procedure
        if (observation.getProcedure() != null) {
            final Optional<ProcessType> process = resolveProperty(observation.getProcedure(), ProcessType.class, refCtx);

            if (process.isPresent()) {
                properties.set(OMObservationProperties.Name.PROCEDURE, process.get());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Could not resolve the process property as METCE Process value in " + contextPath));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No process given in " + contextPath));
        }

        //observed property
        if (observation.getObservedProperty() != null) {
            final ReferenceType obsProp = observation.getObservedProperty();
            if (obsProp != null) {
                properties.set(OMObservationProperties.Name.OBSERVED_PROPERTY, obsProp.getHref());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No observedProperty given in " + contextPath));
            }
        }

        //foi
        if (observation.getFeatureOfInterest() != null) {
            final Optional<SFSpatialSamplingFeatureType> sft = resolveProperty(observation.getFeatureOfInterest(), "abstractFeature",
                    SFSpatialSamplingFeatureType.class, refCtx);
            if (sft.isPresent()) {

                final List<FeaturePropertyType> sampledFeatures = sft.get().getSampledFeature();
                if (sampledFeatures.isEmpty()) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                            "No sampled feature (for aerodrome) in TAF"));
                } else if (sampledFeatures.size() != 1) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one sampled feature in TAF"));
                } else {
                    final Optional<AirportHeliportType> airport = resolveProperty(sampledFeatures.get(0), "abstractFeature", AirportHeliportType.class, refCtx);
                    if (airport.isPresent()) {
                        final Optional<Aerodrome> drome = buildAerodrome(airport.get(), retval, refCtx);
                        drome.ifPresent((d) -> {
                            properties.set(OMObservationProperties.Name.AERODROME, d);
                        });
                    } else {
                        retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Aerodrome info not resolvable from FOI");
                    }
                }

                final ShapeType shape = sft.get().getShape();
                if (shape != null) {
                    final Optional<PointType> point = resolveProperty(shape, "abstractGeometry", PointType.class, refCtx);
                    if (!point.isPresent()) {
                        retval.add(
                                new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "The spatial sampling feature shape is not a Point in " + contextPath));
                    } else {
                        final ElevatedPointImpl.Builder posBuilder = ElevatedPointImpl.builder();
                        boolean canBuildPos = true;
                        if (point.get().getPos() != null) {
                            final DirectPositionType dp = point.get().getPos();
                            final CoordinateReferenceSystemImpl.Builder crsBuilder = mergeToBuilder(point.get(), CoordinateReferenceSystemImpl.builder());
                            mergeToBuilder(dp, crsBuilder);
                            try {
                                posBuilder.setCrs(crsBuilder.build());
                            } catch (final IllegalStateException e) {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No SRS for sampling point position: " + e.getMessage()));
                            }
                            if (dp.getValue() != null) {
                                posBuilder.addAllCoordinates(dp.getValue());
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No value for sampling point position"));
                            }

                            if (canBuildPos) {
                                properties.set(OMObservationProperties.Name.SAMPLING_POINT, posBuilder.build());
                            }

                        } else if (point.get().getCoordinates() != null) {
                            retval.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                    "Found elevated " + "position defined using the deprecated GML CoordinatesType, skipping elevated position info"));
                        }
                    }
                } else {
                    retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No shape in spatial sampling feature in " + contextPath));
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Could not find feature of interest of type SFSpatialSamplingFeature in " + contextPath));
            }
        }
        return retval;
    }

    public static Optional<MeteorologicalAerodromeForecastRecordType> getAerodromeForecastRecordResult(final OMObservationType fct,
            final ReferredObjectRetrievalContext refCtx) {
        final Object o = fct.getResult();
        if (o instanceof Node) {
            final Node resultNode = ((Node) o);
            final Node recordNode = resultNode.getFirstChild();
            if (recordNode != null && "MeteorologicalAerodromeForecastRecord".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(
                    recordNode.getNamespaceURI())) {
                try {
                    final JAXBElement<MeteorologicalAerodromeForecastRecordType> record = refCtx.getJAXBBinder()
                            .unmarshal(recordNode, MeteorologicalAerodromeForecastRecordType.class);
                    return Optional.of(record.getValue());
                } catch (final JAXBException e) {
                    LOG.error("Strange, could not unmarshall MeteorologicalAerodromeForecastRecord DOM Node into"
                            + "MeteorologicalAerodromeForecastRecordType JAXElement, returning an empty Optional", e);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<MeteorologicalAerodromeTrendForecastRecordType> getAerodromeTrendRecordResult(final OMObservationType fct,
            final ReferredObjectRetrievalContext refCtx) {
        final Object o = fct.getResult();
        if (o instanceof Node) {
            final Node resultNode = ((Node) o);
            final Node recordNode = resultNode.getFirstChild();
            if (recordNode != null && "MeteorologicalAerodromeTrendForecastRecord".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(
                    recordNode.getNamespaceURI())) {
                try {
                    final JAXBElement<MeteorologicalAerodromeTrendForecastRecordType> record = refCtx.getJAXBBinder()
                            .unmarshal(recordNode, MeteorologicalAerodromeTrendForecastRecordType.class);
                    return Optional.of(record.getValue());
                } catch (final JAXBException e) {
                    LOG.error("Strange, could not unmarshall MeteorologicalAerodromeTrendForecastRecordType DOM Node into"
                            + "MeteorologicalAerodromeTrendForecastRecordType JAXElement, returning an empty Optional", e);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<MeteorologicalAerodromeObservationRecordType> getAerodromeObservationRecordResult(final OMObservationType fct,
            final ReferredObjectRetrievalContext refCtx) {
        final Object o = fct.getResult();
        if (o instanceof Node) {
            final Node resultNode = ((Node) o);
            final Node recordNode = resultNode.getFirstChild();
            if (recordNode != null && "MeteorologicalAerodromeObservationRecord".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(
                    recordNode.getNamespaceURI())) {
                try {
                    final JAXBElement<MeteorologicalAerodromeObservationRecordType> record = refCtx.getJAXBBinder()
                            .unmarshal(recordNode, MeteorologicalAerodromeObservationRecordType.class);
                    return Optional.of(record.getValue());
                } catch (final JAXBException e) {
                    LOG.error("Strange, could not unmarshall MeteorologicalAerodromeObservationRecord DOM Node into "
                            + "MeteorologicalAerodromeObservationRecordType JAXElement, returning an empty Optional", e);
                }
            }
        }
        return Optional.empty();
    }

    public static void withWeatherBuilderFor(final ReferenceType weather, final ConversionHints hints, final Consumer<WeatherImpl.Builder> resultHandler,
            final Consumer<ConversionIssue> issueHandler) {
        withWeatherBuilderFor(weather.getHref(), weather.getTitle(), hints, resultHandler, issueHandler);
    }

    public static void withCloudLayerBuilderFor(final CloudLayerPropertyType layerProp, final ReferredObjectRetrievalContext refCtx,
            final Consumer<CloudLayerImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        final IssueList issues = new IssueList();
        final Optional<CloudLayerType> layer = resolveProperty(layerProp, CloudLayerType.class, refCtx);
        if (layer.isPresent()) {
            final CloudLayerImpl.Builder layerBuilder = CloudLayerImpl.builder();
            withCloudBase(layer.get(), refCtx, layerBuilder::setBase, issues::add, contextPath);
            withCloudAmount(layer.get(), refCtx, layerBuilder::setAmount, issues::add, contextPath);
            withCloudType(layer.get(), refCtx, layerBuilder::setCloudType, issues::add, contextPath);
            resultHandler.accept(layerBuilder);
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not resolve cloud layer in " + contextPath));
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    protected static void withCloudBase(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx, final Consumer<NumericMeasure> resultHandler,
            final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        withCloudBase(layer, refCtx, resultHandler, null, issueHandler, contextPath);
    }

    public static void withCloudBase(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx, final Consumer<NumericMeasure> resultHandler,
            final Consumer<List<String>> nilReasonHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        if (layer != null) {
            withNillableChild(layer, layer.getBase(), DistanceWithNilReasonType.class, new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "base"), refCtx,
                    (value) -> {
                        resultHandler.accept(asNumericMeasure(value).get());
                    }, nilReasonHandler);
        }
    }

    protected static void withCloudAmount(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx,
            final Consumer<AviationCodeListUser.CloudAmount> resultHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        withCloudAmount(layer, refCtx, resultHandler, null, issueHandler, contextPath);
    }

    public static void withCloudAmount(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx,
            final Consumer<AviationCodeListUser.CloudAmount> resultHandler, final Consumer<List<String>> nilReasonHandler,
            final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        if (layer != null) {
            withNillableChild(layer, layer.getAmount(), CloudAmountReportedAtAerodromeType.class,
                    new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "amount"), refCtx, (value) -> {
                        if (value.getHref() != null && value.getHref()
                                .startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME)) {
                            final String amountCode = value.getHref()
                                    .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME.length());
                            try {
                                final AviationCodeListUser.CloudAmount amountValue = AviationCodeListUser.CloudAmount.valueOf(amountCode);
                                resultHandler.accept(amountValue);
                            } catch (final IllegalArgumentException e) {
                                issueHandler.accept(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                        "Could not parse code list value '" + amountCode + "' for code list "
                                                + AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + " in " + contextPath));
                            }

                        } else {
                            issueHandler.accept(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                    "Cloud amount code '" + value.getHref() + "' does not start with "
                                            + AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + " in " + contextPath));
                        }
                    }, nilReasonHandler);
        }
    }

    protected static void withCloudType(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx,
            final Consumer<AviationCodeListUser.CloudType> resultHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        withCloudType(layer, refCtx, resultHandler, null, issueHandler, contextPath);
    }

    protected static Optional<AviationCodeListUser.RelationalOperator> asRelationalOperator(final RelationalOperatorType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(AviationCodeListUser.RelationalOperator.valueOf(source.name()));
    }

    public static void withCloudType(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx,
            final Consumer<AviationCodeListUser.CloudType> resultHandler, final Consumer<List<String>> nilReasonHandler,
            final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        if (layer != null) {
            final JAXBElement<SigConvectiveCloudTypeType> type = layer.getCloudType();
            if (type != null && !type.isNil()) {
                withNillableChild(layer, type.getValue(), SigConvectiveCloudTypeType.class,
                        new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "cloudType"), refCtx, (value) -> {
                            if (value.getHref() != null) {
                                if (value.getHref().startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE)) {
                                    final String typeCode = value.getHref()
                                            .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE.length());
                                    try {
                                        final AviationCodeListUser.CloudType typeValue = AviationCodeListUser.CloudType.valueOf(typeCode);
                                        resultHandler.accept(typeValue);
                                    } catch (final IllegalArgumentException e) {
                                        issueHandler.accept(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                                                "Could not parse code list value '" + typeCode + "' for code list "
                                                        + AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + " in " + contextPath));
                                    }
                                } else {
                                    issueHandler.accept(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                            "Cloud type code '" + type.getValue().getHref() + "' does not start with "
                                                    + AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + " in " + contextPath));
                                }
                            }
                        }, nilReasonHandler);
            }
        }
    }
}

package fi.fmi.avi.converter.iwxxm;

import static fi.fmi.avi.model.immutable.WeatherImpl.WEATHER_CODES;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.PointType;
import net.opengis.gml32.ReferenceType;
import net.opengis.om20.OMObservationType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.AirportHeliportType;
import aero.aixm511.CodeAirportHeliportDesignatorType;
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.v21.OMObservationProperties;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
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

/**
 * Common functionality for parsing validation of IWXXM messages.
 */
public abstract class AbstractIWXXMScanner extends IWXXMConverterBase {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIWXXMScanner.class);

    protected static IssueList collectReportMetadata(final ReportType input, final GenericReportProperties properties, final ConversionHints hints) {
        IssueList retval = new IssueList();

        //Issues for these reported already by XML Schema or Schematron validation, so not checking them here:
        if (input.getPermissibleUsage() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.valueOf(input.getPermissibleUsage().name()));
        }
        if (input.getPermissibleUsageReason() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.valueOf(input.getPermissibleUsageReason().name()));
        }
        properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, input.getPermissibleUsageSupplementary());
        properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, input.getTranslatedBulletinID());

        XMLGregorianCalendar cal = input.getTranslatedBulletinReceptionTime();
        if (cal != null) {
            ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, time);
        }

        cal = input.getTranslationTime();
        if (cal != null) {
            ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATION_TIME, time);
        }

        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, input.getTranslationCentreDesignator());
        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, input.getTranslationCentreName());
        properties.set(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, input.getTranslationFailedTAC());

        if (!properties.contains(GenericReportProperties.Name.TRANSLATION_TIME)) {
            if (hints != null && hints.containsKey(ConversionHints.KEY_TRANSLATION_TIME)) {
                Object value = hints.get(ConversionHints.KEY_TRANSLATION_TIME);
                if (ConversionHints.VALUE_TRANSLATION_TIME_AUTO.equals(value)) {
                    properties.set(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.now());
                } else if (value instanceof ZonedDateTime) {
                    properties.set(GenericReportProperties.Name.TRANSLATION_TIME, value);
                }
            }
        }
        return retval;
    }

    protected static Optional<Aerodrome> buildAerodrome(final AirportHeliportType airport, final IssueList retval,
            final ReferredObjectRetrievalContext refCtx) {
        AerodromeImpl.Builder aerodromeBuilder = AerodromeImpl.builder();
        List<AirportHeliportTimeSlicePropertyType> slices = airport.getTimeSlice();
        if (slices.isEmpty()) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No time slice (for aerodrome)"));
            return Optional.empty();
        } else if (slices.size() != 1) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one time slice (for aerodrome)"));
            return Optional.empty();
        } else {
            Optional<AirportHeliportTimeSliceType> slice = resolveProperty(slices.get(0), AirportHeliportTimeSliceType.class, refCtx);
            if (slice.isPresent()) {
                CodeAirportHeliportDesignatorType designator = slice.get().getDesignator();
                if (designator == null || designator.getValue() == null) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No designator for " + "aerodrome"));
                } else {
                    aerodromeBuilder.setDesignator(designator.getValue());
                }
                if (slice.get().getLocationIndicatorICAO() != null) {
                    aerodromeBuilder.setLocationIndicatorICAO(slice.get().getLocationIndicatorICAO().getValue());
                }
                if (slice.get().getDesignatorIATA() != null) {
                    aerodromeBuilder.setDesignatorIATA(slice.get().getDesignatorIATA().getValue());
                }
                if (slice.get().getPortName() != null) {
                    aerodromeBuilder.setName(slice.get().getPortName().getValue());
                }

                ValDistanceVerticalType elevation = slice.get().getFieldElevation();
                ElevatedPointPropertyType pointProp = slice.get().getARP();
                if (pointProp != null && pointProp.getNilReason() == null) {
                    Optional<ElevatedPointType> elPoint = resolveProperty(pointProp, ElevatedPointType.class, refCtx);
                    if (elPoint.isPresent()) {
                        String srsName = elPoint.get().getSrsName();
                        GeoPositionImpl.Builder posBuilder = GeoPositionImpl.builder();
                        boolean canBuildPos = true;
                        //use ref point elevation as fallback for the aerodrome elevation
                        if (elevation == null && elPoint.get().getElevation() != null) {
                            elevation = elPoint.get().getElevation();
                            if (elevation.getNilReason() == null) {
                                posBuilder.setNullableElevationUom(elevation.getUom());
                                if (elevation.getValue() != null) {
                                    posBuilder.setElevationValue(Double.parseDouble(elevation.getValue()));
                                }
                            }
                        }
                        if (elPoint.get().getPos() != null) {
                            DirectPositionType dp = elPoint.get().getPos();
                            if (dp.getSrsName() != null) {
                                srsName = dp.getSrsName();
                            }
                            if (srsName != null) {
                                posBuilder.setCoordinateReferenceSystemId(srsName);
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No SRS name for ARP elevated point position"));
                            }
                            if (dp.getValue() != null) {
                                posBuilder.addAllCoordinates(dp.getValue());
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No value for ARP elevated point position"));
                            }

                            if (canBuildPos) {
                                aerodromeBuilder.setReferencePoint(posBuilder.build());
                            }

                        } else if (elPoint.get().getCoordinates() != null) {
                            retval.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                    "Found elevated position defined using the deprecated GML CoordinatesType, skipping elevated position info"));
                        }
                    }
                }

                if (elevation != null && elevation.getNilReason() == null) {
                    aerodromeBuilder.setFieldElevationValue(Double.parseDouble(elevation.getValue()));
                    aerodromeBuilder.setNullableFieldElevationUom(elevation.getUom());
                }
            }
        }
        return Optional.of(aerodromeBuilder.build());
    }

    protected static IssueList collectCommonObsMetadata(final OMObservationType observation, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final String contextPath, final ConversionHints hints) {
        IssueList retval = new IssueList();
        //forecast type
        if (observation.getType() != null) {
            properties.set(OMObservationProperties.Name.TYPE, observation.getType().getHref());
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Observation type is missing in " + contextPath));
        }
        //result time
        if (observation.getResultTime() != null) {
            Optional<PartialOrCompleteTimeInstant> resultTime = getCompleteTimeInstant(observation.getResultTime(), refCtx);
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
            Optional<PartialOrCompleteTimePeriod> validTime = getCompleteTimePeriod(observation.getValidTime(), refCtx);
            validTime.ifPresent((time) -> {
                properties.set(OMObservationProperties.Name.VALID_TIME, time);
            });
        }

        //procedure
        if (observation.getProcedure() != null) {
            Optional<ProcessType> process = resolveProperty(observation.getProcedure(), ProcessType.class, refCtx);

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
            ReferenceType obsProp = observation.getObservedProperty();
            if (obsProp != null) {
                properties.set(OMObservationProperties.Name.OBSERVED_PROPERTY, obsProp.getHref());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No observedProperty given in " + contextPath));
            }
        }

        //foi
        if (observation.getFeatureOfInterest() != null) {
            Optional<SFSpatialSamplingFeatureType> sft = resolveProperty(observation.getFeatureOfInterest(), "abstractFeature",
                    SFSpatialSamplingFeatureType.class, refCtx);
            if (sft.isPresent()) {

                List<FeaturePropertyType> sampledFeatures = sft.get().getSampledFeature();
                if (sampledFeatures.isEmpty()) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                            "No sampled feature (for aerodrome) in TAF"));
                } else if (sampledFeatures.size() != 1) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one sampled feature in TAF"));
                } else {
                    Optional<AirportHeliportType> airport = resolveProperty(sampledFeatures.get(0), "abstractFeature", AirportHeliportType.class, refCtx);
                    if (airport.isPresent()) {
                        Optional<Aerodrome> drome = buildAerodrome(airport.get(), retval, refCtx);
                        drome.ifPresent((d) -> {
                            properties.set(OMObservationProperties.Name.AERODROME, d);
                        });
                    } else {
                        retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Aerodrome info not resolvable from FOI");
                    }
                }

                ShapeType shape = sft.get().getShape();
                if (shape != null) {
                    Optional<PointType> point = resolveProperty(shape, "abstractGeometry", PointType.class, refCtx);
                    if (!point.isPresent()) {
                        retval.add(
                                new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "The spatial sampling feature shape is not a Point in " + contextPath));
                    } else {
                        GeoPositionImpl.Builder posBuilder = GeoPositionImpl.builder();
                        boolean canBuildPos = true;
                        if (point.get().getPos() != null) {
                            DirectPositionType dp = point.get().getPos();
                            if (dp.getSrsName() != null) {
                                posBuilder.setCoordinateReferenceSystemId(dp.getSrsName());
                            } else {
                                if (point.get().getSrsName() != null) {
                                    posBuilder.setCoordinateReferenceSystemId(point.get().getSrsName());
                                } else {
                                    canBuildPos = false;
                                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                            "No SRS name for sampling point position"));
                                }
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

    protected static Optional<MeteorologicalAerodromeForecastRecordType> getAerodromeForecastRecordResult(final OMObservationType fct,
            final ReferredObjectRetrievalContext refCtx) {
        Object o = fct.getResult();
        if (o instanceof Node) {
            Node resultNode = ((Node) o);
            Node recordNode = resultNode.getFirstChild();
            if (recordNode != null && "MeteorologicalAerodromeForecastRecord".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(
                    recordNode.getNamespaceURI())) {
                try {
                    JAXBElement<MeteorologicalAerodromeForecastRecordType> record = refCtx.getJAXBBinder()
                            .unmarshal(recordNode, MeteorologicalAerodromeForecastRecordType.class);
                    return Optional.of(record.getValue());
                } catch (JAXBException e) {
                    LOG.error("Strange, could not unmarshall MeteorologicalAerodromeForecastRecord DOM Node into"
                            + "MeteorologicalAerodromeForecastRecordType JAXElement, returning an empty Optional", e);
                }
            }
        }
        return Optional.empty();
    }

    protected static Optional<MeteorologicalAerodromeTrendForecastRecordType> getAerodromeTrendRecordResult(final OMObservationType fct,
            final ReferredObjectRetrievalContext refCtx) {
        Object o = fct.getResult();
        if (o instanceof Node) {
            Node resultNode = ((Node) o);
            Node recordNode = resultNode.getFirstChild();
            if (recordNode != null && "MeteorologicalAerodromeTrendForecastRecord".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(
                    recordNode.getNamespaceURI())) {
                try {
                    JAXBElement<MeteorologicalAerodromeTrendForecastRecordType> record = refCtx.getJAXBBinder()
                            .unmarshal(recordNode, MeteorologicalAerodromeTrendForecastRecordType.class);
                    return Optional.of(record.getValue());
                } catch (JAXBException e) {
                    LOG.error("Strange, could not unmarshall MeteorologicalAerodromeTrendForecastRecordType DOM Node into"
                            + "MeteorologicalAerodromeTrendForecastRecordType JAXElement, returning an empty Optional", e);
                }
            }
        }
        return Optional.empty();
    }

    protected static Optional<MeteorologicalAerodromeObservationRecordType> getAerodromeObservationRecordResult(final OMObservationType fct,
            final ReferredObjectRetrievalContext refCtx) {
        Object o = fct.getResult();
        if (o instanceof Node) {
            Node resultNode = ((Node) o);
            Node recordNode = resultNode.getFirstChild();
            if (recordNode != null && "MeteorologicalAerodromeObservationRecord".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(
                    recordNode.getNamespaceURI())) {
                try {
                    JAXBElement<MeteorologicalAerodromeObservationRecordType> record = refCtx.getJAXBBinder()
                            .unmarshal(recordNode, MeteorologicalAerodromeObservationRecordType.class);
                    return Optional.of(record.getValue());
                } catch (JAXBException e) {
                    LOG.error("Strange, could not unmarshall MeteorologicalAerodromeObservationRecord DOM Node into "
                            + "MeteorologicalAerodromeObservationRecordType JAXElement, returning an empty Optional", e);
                }
            }
        }
        return Optional.empty();
    }

    protected static Optional<NumericMeasure> asNumericMeasure(final MeasureType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(NumericMeasureImpl.builder().setValue(source.getValue()).setUom(source.getUom()).build());
    }

    protected static Optional<AviationCodeListUser.RelationalOperator> asRelationalOperator(final RelationalOperatorType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(AviationCodeListUser.RelationalOperator.valueOf(source.name()));
    }

    protected static void withWeatherBuilderFor(final ReferenceType weather, final ConversionHints hints, final Consumer<WeatherImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        String codeListValue = weather.getHref();
        if (codeListValue != null && codeListValue.startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER)) {
            String code = codeListValue.substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER.length());
            String description = weather.getTitle();
            WeatherImpl.Builder wBuilder = WeatherImpl.builder();
            boolean codeOk = false;
            if (hints == null || hints.isEmpty() || !hints.containsKey(ConversionHints.KEY_WEATHER_CODES) || ConversionHints.VALUE_WEATHER_CODES_STRICT_WMO_4678
                    .equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                // Only the official codes allowed by default
                if (WEATHER_CODES.containsKey(code)) {
                    wBuilder.setCode(code).setDescription(WEATHER_CODES.get(code));
                    codeOk = true;
                } else {
                    issue = new ConversionIssue(ConversionIssue.Type.SYNTAX, "Illegal weather code " + code + " found with strict WMO 4678 " + "checking");
                }
            } else {
                if (ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    wBuilder.setCode(code);
                    if (description != null) {
                        wBuilder.setDescription(description);
                    } else if (WEATHER_CODES.containsKey(code)) {
                        wBuilder.setDescription(WEATHER_CODES.get(code));
                    }
                } else if (ConversionHints.VALUE_WEATHER_CODES_IGNORE_NON_WMO_4678.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    if (WEATHER_CODES.containsKey(code)) {
                        wBuilder.setCode(code).setDescription(WEATHER_CODES.get(code));
                        codeOk = true;
                    }
                }
            }
            if (codeOk) {
                resultHandler.accept(wBuilder);
            }
        } else {
            issue = new ConversionIssue(ConversionIssue.Type.SYNTAX, "Weather codelist value does not begin with " + AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER);
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    protected static void withCloudLayerBuilderFor(final CloudLayerPropertyType layerProp, final ReferredObjectRetrievalContext refCtx,
                                                   final Consumer<CloudLayerImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        IssueList issues = new IssueList();
        Optional<CloudLayerType> layer = resolveProperty(layerProp, CloudLayerType.class, refCtx);
        if (layer.isPresent()) {
            CloudLayerImpl.Builder layerBuilder = CloudLayerImpl.builder();
            withCloudBase(layer.get(), refCtx, layerBuilder::setBase, issues::add, contextPath);
            withCloudAmount(layer.get(), refCtx, layerBuilder::setAmount, issues::add, contextPath);
            withCloudType(layer.get(), refCtx, layerBuilder::setCloudType, issues::add, contextPath);
            resultHandler.accept(layerBuilder);
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not resolve cloud layer in " + contextPath));
        }
        for (ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    protected static void withCloudBase(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx, final Consumer<NumericMeasure> resultHandler,
            final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        withCloudBase(layer, refCtx, resultHandler, null, issueHandler, contextPath);
    }

    protected static void withCloudBase(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx, final Consumer<NumericMeasure> resultHandler,
            final Consumer<List<String>> nilReasonHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        if (layer != null) {
            withNillableChild(layer, layer.getBase(), DistanceWithNilReasonType.class, new QName(IWXXMNamespaceContext.getURI("iwxxm"), "base"), refCtx,
                    (value) -> {
                        resultHandler.accept(asNumericMeasure(value).get());
                    }, nilReasonHandler);
        }
    }

    protected static void withCloudAmount(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx,
            final Consumer<AviationCodeListUser.CloudAmount> resultHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        withCloudAmount(layer, refCtx, resultHandler, null, issueHandler, contextPath);
    }

    protected static void withCloudAmount(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx,
            final Consumer<AviationCodeListUser.CloudAmount> resultHandler, final Consumer<List<String>> nilReasonHandler,
            final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        if (layer != null) {
            withNillableChild(layer, layer.getAmount(), CloudAmountReportedAtAerodromeType.class, new QName(IWXXMNamespaceContext.getURI("iwxxm"), "amount"),
                    refCtx, (value) -> {
                        if (value.getHref() != null && value.getHref()
                                .startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME)) {
                            String amountCode = value.getHref()
                                    .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME.length());
                    try {
                        AviationCodeListUser.CloudAmount amountValue = AviationCodeListUser.CloudAmount.fromInt(Integer.parseInt(amountCode));
                        resultHandler.accept(amountValue);
                    } catch (NumberFormatException e) {
                        issueHandler.accept(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                "Could not parse code list value '" + amountCode + "' as an integer for code list "
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

    protected static void withCloudType(final CloudLayerType layer, final ReferredObjectRetrievalContext refCtx,
            final Consumer<AviationCodeListUser.CloudType> resultHandler, final Consumer<List<String>> nilReasonHandler,
            final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        if (layer != null) {
            JAXBElement<SigConvectiveCloudTypeType> type = layer.getCloudType();
            if (type != null && !type.isNil()) {
                withNillableChild(layer, type.getValue(), SigConvectiveCloudTypeType.class, new QName(IWXXMNamespaceContext.getURI("iwxxm"), "cloudType"),
                        refCtx, (value) -> {
                            if (value.getHref() != null) {
                                if (value.getHref().startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE)) {
                                    String typeCode = value.getHref().substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE.length());
                            try {
                                AviationCodeListUser.CloudType typeValue = AviationCodeListUser.CloudType.fromInt(Integer.parseInt(typeCode));
                                resultHandler.accept(typeValue);
                            } catch (NumberFormatException e) {
                                issueHandler.accept(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                                        "Could not parse code list value '" + typeCode + "' as an integer for code list "
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

    protected static <T> void withNillableChild(final Object parent, final T child, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        withNthNillableChild(parent, child, clz, childElementName, refCtx, 0, valueHandler, nilReasonHandler);
    }

    protected static <T> void withNthNillableChild(final Object parent, final T child, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final int n, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        List<String> nilReasons = new ArrayList<>();
        if (child == null) {
            Optional<String> nilReason = refCtx.getNilReasonForNthChild(parent, childElementName, n);
            nilReason.ifPresent(reason -> nilReasons.addAll(Arrays.asList(reason.split("\\s"))));
        } else {
            try {
                Class[] params = new Class[0];
                Method getNilReason = clz.getMethod("getNilReason", params);
                Object[] paramValues = new Object[0];
                Object value = getNilReason.invoke(child, paramValues);
                if (value != null) {
                    if (List.class.isAssignableFrom(value.getClass())) {
                        List<?> values = (List<?>) value;
                        for (Object o : values) {
                            if (o instanceof String) {
                                nilReasons.add((String) o);
                            }
                        }
                    } else if (String.class.isAssignableFrom(value.getClass())) {
                        nilReasons.addAll(Arrays.asList(((String) value).split("\\s")));
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                //NOOP
            }
        }
        if (child != null && nilReasons.isEmpty()) {
            valueHandler.accept(child);
        } else if (!nilReasons.isEmpty()) {
            nilReasonHandler.accept(nilReasons);
        }
    }

    protected static <T> void withEachNillableChild(final Object parent, final Iterable<T> children, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        int i = 0;
        for (T child:children) {
            withNthNillableChild(parent, child, clz, childElementName, refCtx, i++, valueHandler, nilReasonHandler);
        }
    }
}

package fi.fmi.avi.converter.iwxxm;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;

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
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import icao.iwxxm21.MeteorologicalAerodromeForecastRecordType;
import icao.iwxxm21.MeteorologicalAerodromeObservationRecordType;
import icao.iwxxm21.MeteorologicalAerodromeTrendForecastRecordType;
import icao.iwxxm21.RelationalOperatorType;
import icao.iwxxm21.ReportType;
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
                    properties.set(GenericReportProperties.Name.TRANSLATION_TIME, (ZonedDateTime) value);
                }
            }
        }
        return retval;
    }

    protected static Optional<Aerodrome> buildAerodrome(final AirportHeliportType airport, final IssueList retval,
            final ReferredObjectRetrievalContext refCtx) {
        AerodromeImpl.Builder aerodromeBuilder = new AerodromeImpl.Builder();
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
                if (slice.get().getDesignator().getNilReason() == null) {
                    aerodromeBuilder.setDesignator(slice.get().getDesignator().getValue());
                } else {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No designator for " + "aerodrome"));
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
                        GeoPositionImpl.Builder posBuilder = new GeoPositionImpl.Builder();
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
                                posBuilder.setCoordinates(dp.getValue().toArray(new Double[] {}));
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
                    if ("M".equals(elevation.getUom())) {
                        aerodromeBuilder.setFieldElevationValue(Double.parseDouble(elevation.getValue()));
                    } else {
                        retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                "Field elevation unit is '" + elevation.getUom() + "', 'M' is required"));
                    }
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
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "Observation type for IWXXM 2.1 aerodrome forecast Observation type is missing in " + contextPath));
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
            if (AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES.equals(obsProp.getHref())) {
                properties.set(OMObservationProperties.Name.OBSERVED_PROPERTY, obsProp.getHref());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                        "Observed property does not match " + AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES + " in " + contextPath));
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
                    airport.ifPresent((p) -> {
                        Optional<Aerodrome> drome = buildAerodrome(p, retval, refCtx);
                        drome.ifPresent((d) -> {
                            properties.set(OMObservationProperties.Name.AERODROME, d);
                        });
                    });
                }

                ShapeType shape = sft.get().getShape();
                if (shape != null) {
                    Optional<PointType> point = resolveProperty(shape, "abstractGeometry", PointType.class, refCtx);
                    if (!point.isPresent()) {
                        retval.add(
                                new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "The spatial sampling feature shape is not a Point in " + contextPath));
                    } else {
                        GeoPositionImpl.Builder posBuilder = new GeoPositionImpl.Builder();
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
                                posBuilder.setCoordinates(dp.getValue().toArray(new Double[] {}));
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
            if (recordNode != null && "MeteorologicalAerodromeTrendForecastRecordType".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(
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
        return Optional.of(new NumericMeasureImpl.Builder().setValue(source.getValue()).setUom(source.getUom()).build());
    }

    protected static Optional<AviationCodeListUser.RelationalOperator> asRelationalOperator(final RelationalOperatorType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(AviationCodeListUser.RelationalOperator.valueOf(source.name()));
    }
}
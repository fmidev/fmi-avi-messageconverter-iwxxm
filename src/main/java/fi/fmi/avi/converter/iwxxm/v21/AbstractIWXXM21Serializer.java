package fi.fmi.avi.converter.iwxxm.v21;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import net.opengis.gml32.AbstractGeometryType;
import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.AngleType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.PointType;
import net.opengis.gml32.ReferenceType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.om20.OMObservationType;
import net.opengis.om20.TimeObjectPropertyType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

import aero.aixm511.AirportHeliportType;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.GeoPosition;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import icao.iwxxm21.AerodromeCloudForecastType;
import icao.iwxxm21.AngleWithNilReasonType;
import icao.iwxxm21.CloudAmountReportedAtAerodromeType;
import icao.iwxxm21.CloudLayerType;
import icao.iwxxm21.DistanceWithNilReasonType;
import icao.iwxxm21.LengthWithNilReasonType;
import icao.iwxxm21.SigConvectiveCloudTypeType;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

public abstract class AbstractIWXXM21Serializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMSerializer<T, S> {

    public static Optional<PartialOrCompleteTimePeriod> getCompleteTimePeriod(final TimeObjectPropertyType timeObjectPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<AbstractTimeObjectType> to = resolveProperty(timeObjectPropertyType, "abstractTimeObject", AbstractTimeObjectType.class, refCtx);
        if (to.isPresent()) {
            if (TimePeriodType.class.isAssignableFrom(to.get().getClass())) {
                final TimePeriodType tp = (TimePeriodType) to.get();
                final PartialOrCompleteTimePeriod.Builder retval = PartialOrCompleteTimePeriod.builder();
                getStartTime(tp, refCtx).ifPresent((start) -> {
                    retval.setStartTime(PartialOrCompleteTimeInstant.builder()//
                            .setCompleteTime(start).build());
                });

                getEndTime(tp, refCtx).ifPresent((end) -> {
                    retval.setEndTime(PartialOrCompleteTimeInstant.builder()//
                            .setCompleteTime(end).build());
                });
                return Optional.of(retval.build());
            }
        }
        return Optional.empty();
    }

    public static Optional<PartialOrCompleteTimeInstant> getCompleteTimeInstant(final TimeObjectPropertyType timeObjectPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<AbstractTimeObjectType> to = resolveProperty(timeObjectPropertyType, "abstractTimeObject", AbstractTimeObjectType.class, refCtx);
        if (to.isPresent()) {
            if (TimeInstantType.class.isAssignableFrom(to.get().getClass())) {
                final TimeInstantType ti = (TimeInstantType) to.get();
                final Optional<ZonedDateTime> time = getTime(ti.getTimePosition());
                if (time.isPresent()) {
                    return Optional.of(PartialOrCompleteTimeInstant.builder().setCompleteTime(time).build());
                }
            } else {
                throw new IllegalArgumentException("Time object is not a time instant");
            }
        }
        return Optional.empty();
    }


    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(SpaceWeatherAdvisoryType.class.getResourceAsStream("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
        schemaInfo.addSchematronRule(SpaceWeatherAdvisoryType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/2.1", "https://schemas.wmo.int/iwxxm/2.1.1/iwxxm.xsd");
        schemaInfo.addSchemaLocation("http://def.wmo.int/metce/2013", "http://schemas.wmo.int/metce/1.2/metce.xsd");
        schemaInfo.addSchemaLocation("http://www.opengis.net/samplingSpatial/2.0", "http://schemas.opengis.net/samplingSpatial/2.0/spatialSamplingFeature.xsd");
        return schemaInfo;
    }

    @SuppressWarnings("unchecked")
    protected void updateSamplingFeature(final Aerodrome input, final OMObservationType target, final String foiId, final String aerodromeId,
            final ConversionResult<?> result) {
        if (input == null) {
            throw new IllegalArgumentException("Aerodrome info is null");
        }

        target.setFeatureOfInterest(
                create(FeaturePropertyType.class, (prop) -> prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, (samsFeature) -> {
                    samsFeature.setId(foiId);
                    samsFeature.setType(create(ReferenceType.class, (ref) -> {
                        ref.setHref("http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingPoint");
                        ref.setTitle("Sampling point");
                    }));

                    samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, (samProp) -> {
                        final AirportHeliportType aerodrome = create(AirportHeliportType.class);
                        this.setAerodromeData(aerodrome, input, aerodromeId);
                        samProp.setAbstractFeature(wrap(aerodrome, AirportHeliportType.class));
                    }));

                    if (input.getReferencePoint().isPresent()) {
                        samsFeature.setShape(create(ShapeType.class, (shape) -> {
                            final JAXBElement<?> wrapped = wrap(create(PointType.class, (point) -> {
                                final Optional<GeoPosition> inputPos = input.getReferencePoint();
                                if (inputPos.isPresent()) {
                                    point.setId("point-" + UUID.randomUUID().toString());

                                    point.setSrsName(inputPos.get().getCoordinateReferenceSystemId());
                                    if (inputPos.get().getCoordinates() != null) {
                                        point.setSrsDimension(BigInteger.valueOf(inputPos.get().getCoordinates().size()));
                                        point.setPos(create(DirectPositionType.class,
                                                (pos) -> pos.getValue().addAll(inputPos.get().getCoordinates())));
                                    }
                                }
                            }), PointType.class);

                            /*
                             * Something is not right here in either the schema or the JAXB bindings:
                             *
                             * The method should be
                             *    shape.setAbstractGeometry(JAXBElement<? extends AbstractGeometry>)
                             * but it's generated as
                             *    shape.setAbstractGeometry(JAXBElement<AbstractGeometry>)
                             *
                             * Have to work around it with an unsafe cast:
                             */
                            shape.setAbstractGeometry((JAXBElement<AbstractGeometryType>) wrapped);
                        }));
                    }
                }))));
    }

    protected void updateForecastClouds(final CloudForecast source, final AerodromeCloudForecastType target, final ConversionResult<?> result) {
        if (source != null) {
            target.setId("cfct-" + UUID.randomUUID().toString());
            final Optional<NumericMeasure> measure = source.getVerticalVisibility();
            if (measure.isPresent()) {
                final QName eName = new QName(IWXXMNamespaceContext.getURI("iwxxm"), "verticalVisibility");
                final LengthWithNilReasonType vvValue = create(LengthWithNilReasonType.class, (vv) -> {
                    vv.setValue(measure.get().getValue());
                    vv.setUom(measure.get().getUom());
                });
                target.setVerticalVisibility(new JAXBElement<>(eName, LengthWithNilReasonType.class, vvValue));
            }
            if (source.getLayers().isPresent()) {
                for (final CloudLayer layer : source.getLayers().get()) {
                    target.getLayer()
                            .add(create(AerodromeCloudForecastType.Layer.class,
                                    (l) -> l.setCloudLayer(create(CloudLayerType.class, (cl) -> this.setForecastCloudLayerData(cl, layer)))));
                }
            }
        }
    }

    protected void setForecastCloudLayerData(final CloudLayerType target, final CloudLayer source) {
        if (source != null) {
            if (source.getBase().isPresent()) {
                target.setBase(asMeasure(source.getBase().get(), DistanceWithNilReasonType.class));
            }
            Optional<AviationCodeListUser.CloudAmount> amount = source.getAmount();
            if (amount.isPresent()) {
                target.setAmount(create(CloudAmountReportedAtAerodromeType.class, (amt) -> {
                    amt.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + amount.get().getCode());
                    amt.setTitle(amount.get().name() + ", from codelist " + AviationCodeListUser.CODELIST_CLOUD_AMOUNT_REPORTED_AT_AERODROME);
                }));
            }
            Optional<AviationCodeListUser.CloudType> type = source.getCloudType();
            if (type.isPresent()) {
                final QName eName = new QName(IWXXMNamespaceContext.getURI("iwxxm"), "cloudType");
                final SigConvectiveCloudTypeType cloudType = create(SigConvectiveCloudTypeType.class, (convCloud) -> {
                    convCloud.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + type.get().getCode());
                    convCloud.setTitle(type.get().name() + ", from codelist " + AviationCodeListUser.CODELIST_SIGNIFICANT_CONVECTIVE_CLOUD_TYPE);
                });
                target.setCloudType(new JAXBElement<>(eName, SigConvectiveCloudTypeType.class, cloudType));
            }
        }
    }

    protected MeasureType asMeasure(final NumericMeasure source) {
        return asMeasure(source, MeasureType.class);
    }

    @SuppressWarnings("unchecked")
    protected <S extends MeasureType> S asMeasure(final NumericMeasure source, final Class<S> clz) {
        final S retval;
        if (source != null) {
            if (SpeedType.class.isAssignableFrom(clz)) {
                retval = (S) create(SpeedType.class);
            } else if (AngleWithNilReasonType.class.isAssignableFrom(clz)) {
                retval = (S) create(AngleWithNilReasonType.class);
            } else if (AngleType.class.isAssignableFrom(clz)) {
                retval = (S) create(AngleType.class);
            } else if (DistanceWithNilReasonType.class.isAssignableFrom(clz)) {
                retval = (S) create(DistanceWithNilReasonType.class);
            } else if (LengthWithNilReasonType.class.isAssignableFrom(clz)) {
                retval = (S) create(LengthWithNilReasonType.class);
            } else if (LengthType.class.isAssignableFrom(clz)) {
                retval = (S) create(LengthType.class);
            } else {
                retval = (S) create(MeasureType.class);
            }
            retval.setValue(source.getValue());
            retval.setUom(source.getUom());
        } else {
            throw new IllegalArgumentException("NumericMeasure is null");
        }
        return retval;
    }
}

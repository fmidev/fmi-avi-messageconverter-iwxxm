package fi.fmi.avi.converter.iwxxm.v2_1;

import aero.aixm511.AirportHeliportType;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.*;
import fi.fmi.avi.model.*;
import icao.iwxxm21.*;
import net.opengis.gml32.*;
import net.opengis.om20.OMObservationType;
import net.opengis.om20.TimeObjectPropertyType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractIWXXM21Serializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMAixm511WxSerializer<T, S> {

    private static IWXXMNamespaceContext nsCtx;

    public static Optional<PartialOrCompleteTimePeriod> getCompleteTimePeriod(final TimeObjectPropertyType timeObjectPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<AbstractTimeObjectType> to = resolveProperty(timeObjectPropertyType, "abstractTimeObject", AbstractTimeObjectType.class, refCtx);
        if (to.isPresent()) {
            if (TimePeriodType.class.isAssignableFrom(to.get().getClass())) {
                final TimePeriodType tp = (TimePeriodType) to.get();
                final PartialOrCompleteTimePeriod.Builder retval = PartialOrCompleteTimePeriod.builder();
                getStartTime(tp, refCtx).ifPresent(start -> retval.setStartTime(PartialOrCompleteTimeInstant.builder()//
                        .setCompleteTime(start).build()));

                getEndTime(tp, refCtx).ifPresent(end -> retval.setEndTime(PartialOrCompleteTimeInstant.builder()//
                        .setCompleteTime(end).build()));
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

    private static synchronized IWXXMNamespaceContext getNSContext() {
        if (nsCtx == null) {
            nsCtx = new IWXXMNamespaceContext();
        }
        return nsCtx;
    }

    protected static <E> E getFirstOrNull(final List<E> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @Override
    public XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(IWXXMSchemaResourceResolverAixm511Wx.getInstance(), F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(TAFType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
        schemaInfo.addSchematronRule(TAFType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/2.1", "https://schemas.wmo.int/iwxxm/2.1.1/iwxxm.xsd");
        schemaInfo.addSchemaLocation("http://def.wmo.int/metce/2013", "http://schemas.wmo.int/metce/1.2/metce.xsd");
        schemaInfo.addSchemaLocation("http://www.opengis.net/samplingSpatial/2.0", "http://schemas.opengis.net/samplingSpatial/2.0/spatialSamplingFeature.xsd");
        return schemaInfo;
    }

    @Override
    protected IWXXMNamespaceContext getNamespaceContext() {
        return getNSContext();
    }

    protected void setAerodromeData(final AirportHeliportType aerodrome, final Aerodrome input, final String aerodromeId) {
        setAerodromeData(aerodrome, input, aerodromeId, "aerodrome-", "point-");
    }

    @SuppressWarnings("unchecked")
    protected void updateSamplingFeature(final Aerodrome input, final OMObservationType target, final String foiId, final String aerodromeId,
            final ConversionResult<?> result) {
        if (input == null) {
            throw new IllegalArgumentException("Aerodrome info is null");
        }

        target.setFeatureOfInterest(
                create(FeaturePropertyType.class, prop -> prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, samsFeature -> {
                    samsFeature.setId(foiId);
                    samsFeature.setType(create(ReferenceType.class, ref -> {
                        ref.setHref("http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingPoint");
                        ref.setTitle("Sampling point");
                    }));

                    samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, samProp -> {
                        final AirportHeliportType aerodrome = create(AirportHeliportType.class);
                        this.setAerodromeData(aerodrome, input, aerodromeId);
                        samProp.setAbstractFeature(wrap(aerodrome, AirportHeliportType.class));
                    }));

                    if (input.getReferencePoint().isPresent()) {
                        samsFeature.setShape(create(ShapeType.class, shape -> {
                            final JAXBElement<?> wrapped = wrap(create(PointType.class, point -> {
                                final Optional<ElevatedPoint> inputPos = input.getReferencePoint();
                                if (inputPos.isPresent()) {
                                    point.setId("point-" + UUID.randomUUID());
                                    inputPos.get().getCrs().ifPresent(crs -> setCrsToType(point, crs));
                                    if (inputPos.get().getCoordinates() != null) {
                                        point.setSrsDimension(BigInteger.valueOf(inputPos.get().getCoordinates().size()));
                                        point.setPos(create(DirectPositionType.class, pos -> pos.getValue().addAll(inputPos.get().getCoordinates())));
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
            target.setId("cfct-" + UUID.randomUUID());
            source.getVerticalVisibility().ifPresent(measure -> {
                final LengthWithNilReasonType vvValue = asMeasure(measure, LengthWithNilReasonType.class);
                final QName eName = new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "verticalVisibility");
                target.setVerticalVisibility(new JAXBElement<>(eName, LengthWithNilReasonType.class, vvValue));
            });
            for (final CloudLayer layer : source.getLayers().orElse(Collections.emptyList())) {
                target.getLayer()
                        .add(create(AerodromeCloudForecastType.Layer.class,
                                l -> l.setCloudLayer(create(CloudLayerType.class, cl -> this.setForecastCloudLayerData(cl, layer)))));
            }
        }
    }

    protected void setForecastCloudLayerData(final CloudLayerType target, final CloudLayer source) {
        if (source != null) {
            source.getBase().ifPresent(numericMeasure -> target.setBase(asMeasure(numericMeasure, DistanceWithNilReasonType.class)));
            source.getAmount().ifPresent(cloudAmount -> target.setAmount(create(CloudAmountReportedAtAerodromeType.class, amt -> {
                amt.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + cloudAmount.getCode());
                amt.setTitle(cloudAmount.name() + ", from codelist " + AviationCodeListUser.CODELIST_CLOUD_AMOUNT_REPORTED_AT_AERODROME);
            })));
            source.getCloudType().ifPresent(value -> {
                final QName eName = new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "cloudType");
                final SigConvectiveCloudTypeType cloudType = create(SigConvectiveCloudTypeType.class, convCloud -> {
                    convCloud.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + value.getCode());
                    convCloud.setTitle(value.name() + ", from codelist " + AviationCodeListUser.CODELIST_SIGNIFICANT_CONVECTIVE_CLOUD_TYPE);
                });
                target.setCloudType(new JAXBElement<>(eName, SigConvectiveCloudTypeType.class, cloudType));
            });
        }
    }
}

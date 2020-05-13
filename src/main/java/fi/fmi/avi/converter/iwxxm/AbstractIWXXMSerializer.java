package fi.fmi.avi.converter.iwxxm;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import net.opengis.gml32.TimePrimitivePropertyType;
import net.opengis.om20.OMObservationType;
import net.opengis.om20.TimeObjectPropertyType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.AirportHeliportType;
import aero.aixm511.CodeAirportHeliportDesignatorType;
import aero.aixm511.CodeIATAType;
import aero.aixm511.CodeICAOType;
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.TextNameType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
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

/**
 * Common functionality for serializing aviation messages into IWXXM.
 */
public abstract class AbstractIWXXMSerializer extends IWXXMConverterBase {

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

    protected Document renderXMLDocument(final Object input, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
        final StringWriter sw = new StringWriter();
        try {
            final Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemaInfo.getSchemaLocations());
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new IWXXMNamespaceContext());
            marshaller.marshal(wrap(input, (Class<Object>) input.getClass()), sw);
            return asCleanedUpXML(sw.toString(), hints);
        } catch (final JAXBException e) {
            throw new ConversionException("Exception in rendering to DOM", e);
        }
    }

    protected String renderDOMToString(final Document source, final ConversionHints hints) throws ConversionException {
        if (source != null) {
            try {
                StringWriter sw = new StringWriter();
                Result output = new StreamResult(sw);
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer = tFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                DOMSource dsource = new DOMSource(source);
                transformer.transform(dsource, output);
                return sw.toString();
            } catch (TransformerException e) {
                throw new ConversionException("Exception in rendering to String", e);
            }
        }
        return null;
    }

    private Document asCleanedUpXML(final String input, final ConversionHints hints) throws ConversionException {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, F_SECURE_PROCESSING);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(input));
            final Document dom3Doc = db.parse(is);

            final DOMResult cleanedResult = new DOMResult();
            final TransformerFactory tFactory = TransformerFactory.newInstance();
            final Transformer transformer = tFactory.newTransformer(new DOMSource(db.parse(getCleanupTransformationStylesheet(hints))));
            final DOMSource dSource = new DOMSource(dom3Doc);
            transformer.transform(dSource, cleanedResult);
            return (Document) cleanedResult.getNode();
        } catch (final ParserConfigurationException | SAXException | IOException | TransformerException e) {
            throw new ConversionException("Exception in cleaning up", e);
        }
    }

    protected abstract InputStream getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException;

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
                                        point.setPos(create(DirectPositionType.class, (pos) -> pos.getValue().addAll(inputPos.get().getCoordinates())));
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

    protected void setAerodromeData(final AirportHeliportType aerodrome, final Aerodrome input, final String aerodromeId) {
        if (input == null) {
            return;
        }
        aerodrome.setId(aerodromeId);
        aerodrome.getTimeSlice()
                .add(create(AirportHeliportTimeSlicePropertyType.class,
                        (prop) -> prop.setAirportHeliportTimeSlice(create(AirportHeliportTimeSliceType.class, (timeSlice) -> {
                            timeSlice.setId("aerodrome-" + UUID.randomUUID().toString());
                            timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                            timeSlice.setInterpretation("SNAPSHOT");
                            timeSlice.setDesignator(
                                    create(CodeAirportHeliportDesignatorType.class, (designator) -> designator.setValue(input.getDesignator())));
                            input.getName()
                                    .ifPresent(
                                            inputName -> timeSlice.setPortName(create(TextNameType.class, (name) -> name.setValue(inputName.toUpperCase()))));
                            input.getLocationIndicatorICAO()
                                    .ifPresent(inputLocator -> timeSlice.setLocationIndicatorICAO(
                                            create(CodeICAOType.class, (locator) -> locator.setValue(inputLocator))));

                            input.getDesignatorIATA()
                                    .ifPresent(inputDesignator -> timeSlice.setDesignatorIATA(
                                            create(CodeIATAType.class, (designator) -> designator.setValue(inputDesignator))));
                            input.getFieldElevationValue()
                                    .ifPresent(inputElevation -> timeSlice.setFieldElevation(create(ValDistanceVerticalType.class, (elevation) -> {
                                        elevation.setValue(String.format("%.00f", inputElevation));
                                        if (input.getFieldElevationUom().isPresent()) {
                                            elevation.setUom(input.getFieldElevationUom().get());
                                        }
                                    })));

                            input.getReferencePoint()
                                    .ifPresent(inputPosition -> timeSlice.setARP(create(ElevatedPointPropertyType.class,
                                            (pointProp) -> pointProp.setElevatedPoint(create(ElevatedPointType.class, (point) -> {
                                                point.setId("point-" + UUID.randomUUID().toString());
                                                point.setSrsName(inputPosition.getCoordinateReferenceSystemId());
                                                if (inputPosition.getCoordinates() != null) {
                                                    point.setSrsDimension(BigInteger.valueOf(inputPosition.getCoordinates().size()));
                                                    point.setPos(
                                                            create(DirectPositionType.class, (pos) -> pos.getValue().addAll(inputPosition.getCoordinates())));
                                                }
                                                if (inputPosition.getElevationValue().isPresent() && inputPosition.getElevationUom().isPresent()) {
                                                    point.setElevation(create(ValDistanceVerticalType.class, (dist) -> {
                                                        inputPosition.getElevationValue().ifPresent(value -> dist.setValue(String.format("%.00f", value)));
                                                        inputPosition.getElevationUom().ifPresent(uom -> dist.setUom(uom.toUpperCase()));
                                                    }));
                                                }
                                            })))));
                        }))));
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
}

package fi.fmi.avi.converter.iwxxm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

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
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.GeoPosition;
import fi.fmi.avi.model.NumericMeasure;
import icao.iwxxm21.AerodromeCloudForecastType;
import icao.iwxxm21.AngleWithNilReasonType;
import icao.iwxxm21.CloudAmountReportedAtAerodromeType;
import icao.iwxxm21.CloudLayerType;
import icao.iwxxm21.DistanceWithNilReasonType;
import icao.iwxxm21.LengthWithNilReasonType;
import icao.iwxxm21.SigConvectiveCloudTypeType;

import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.AbstractGeometryType;
import net.opengis.gml32.AngleType;
import net.opengis.gml32.PointType;
import net.opengis.gml32.ReferenceType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.TimePrimitivePropertyType;
import net.opengis.om20.OMObservationType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

/**
 * Created by rinne on 20/07/17.
 */
public abstract class AerodromeMessageIWXXMSerializerBase<T> extends AbstractIWXXMSerializer<T> {

    public static final int MAX_CLOUD_LAYERS = 4;

    @SuppressWarnings("unchecked")
    protected void updateSamplingFeature(final Aerodrome input, final OMObservationType target, final String foiId, final String aerodromeId,
            final ConversionResult<?> result) {
        if (input == null) {
            throw new IllegalArgumentException("Aerodrome info is null");
        }

        target.setFeatureOfInterest(create(FeaturePropertyType.class, (prop) -> {
            prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, (samsFeature) -> {
                samsFeature.setId(foiId);
                samsFeature.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref("http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingPoint");
                    ref.setTitle("Sampling point");
                }));

                samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, (samProp) -> {
                    AirportHeliportType aerodrome = create(AirportHeliportType.class);
                    this.setAerodromeData(aerodrome, input, aerodromeId);
                    samProp.setAbstractFeature(wrap(aerodrome, AirportHeliportType.class));
                }));

                if (input.getReferencePoint() != null) {
                    samsFeature.setShape(create(ShapeType.class, (shape) -> {
                        JAXBElement<?> wrapped = wrap(create(PointType.class, (point) -> {
                            Optional<GeoPosition> inputPos = input.getReferencePoint();
                            if (inputPos.isPresent()) {
                                point.setId("point-" + UUID.randomUUID().toString());

                                point.setSrsName(inputPos.get().getCoordinateReferenceSystemId());
                                if (inputPos.get().getCoordinates() != null) {
                                    point.setSrsDimension(BigInteger.valueOf(inputPos.get().getCoordinates().length));
                                    point.setPos(create(DirectPositionType.class, (pos) -> pos.getValue().addAll(Arrays.asList(inputPos.get().getCoordinates()))));
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
            }));
        }));
    }

    protected void setAerodromeData(final AirportHeliportType aerodrome, final Aerodrome input, final String aerodromeId) {
        if (input == null) {
            return;
        }
        aerodrome.setId(aerodromeId);
        aerodrome.getTimeSlice().add(create(AirportHeliportTimeSlicePropertyType.class, (prop) -> {
            prop.setAirportHeliportTimeSlice(create(AirportHeliportTimeSliceType.class, (timeSlice) -> {
                timeSlice.setId("aerodrome-" + UUID.randomUUID().toString());
                timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                timeSlice.setInterpretation("SNAPSHOT");
                timeSlice.setDesignator(create(CodeAirportHeliportDesignatorType.class, (designator) -> {
                    designator.setValue(input.getDesignator());
                }));
                if (input.getName().isPresent()) {
                    timeSlice.setPortName(create(TextNameType.class, (name) -> name.setValue(input.getName().get().toUpperCase())));
                }
                if (input.getLocationIndicatorICAO().isPresent()) {
                    timeSlice.setLocationIndicatorICAO(create(CodeICAOType.class, (locator) -> locator.setValue(input.getLocationIndicatorICAO().get())));
                }

                if (input.getDesignatorIATA().isPresent()) {
                    timeSlice.setDesignatorIATA(create(CodeIATAType.class, (designator) -> designator.setValue(input.getDesignatorIATA().get())));
                }
                if (input.getFieldElevationValue().isPresent()) {
                    timeSlice.setFieldElevation(create(ValDistanceVerticalType.class, (elevation) -> {
                        elevation.setValue(String.format("%.00f", input.getFieldElevationValue().get()));
                        elevation.setUom("M");
                    }));
                }

                if (input.getReferencePoint().isPresent()) {
                    timeSlice.setARP(create(ElevatedPointPropertyType.class, (pointProp) -> {
                        pointProp.setElevatedPoint(create(ElevatedPointType.class, (point) -> {
                            Optional<GeoPosition> inputPos = input.getReferencePoint();
                            if (inputPos.isPresent()) {
                                point.setId("point-" + UUID.randomUUID().toString());
                                point.setSrsName(inputPos.get().getCoordinateReferenceSystemId());
                                if (inputPos.get().getCoordinates() != null) {
                                    point.setSrsDimension(BigInteger.valueOf(inputPos.get().getCoordinates().length));
                                    point.setPos(create(DirectPositionType.class, (pos) -> pos.getValue().addAll(Arrays.asList(inputPos.get().getCoordinates()))));
                                }
                                if (inputPos.get().getElevationValue().isPresent() && inputPos.get().getElevationUom().isPresent()) {
                                    point.setElevation(create(ValDistanceVerticalType.class, (dist) -> {
                                        dist.setValue(String.format("%.00f", inputPos.get().getElevationValue().get().doubleValue()));
                                        dist.setUom(inputPos.get().getElevationUom().get().toUpperCase());
                                    }));
                                }
                            }
                        }));
                    }));
                }
            }));
        }));
    }

    protected MeasureType asMeasure(final NumericMeasure source) {
        return asMeasure(source, MeasureType.class);
    }

    @SuppressWarnings("unchecked")
    protected <T extends MeasureType> T asMeasure(final NumericMeasure source, final Class<T> clz) {
        T retval = null;
        if (source != null) {
            if (SpeedType.class.isAssignableFrom(clz)) {
                retval = (T) create(SpeedType.class);
            } else if (AngleWithNilReasonType.class.isAssignableFrom(clz)) {
                retval = (T) create(AngleWithNilReasonType.class);
            } else if (AngleType.class.isAssignableFrom(clz)) {
                retval = (T) create(AngleType.class);
            } else if (DistanceWithNilReasonType.class.isAssignableFrom(clz)) {
                retval = (T) create(DistanceWithNilReasonType.class);
            } else if (LengthWithNilReasonType.class.isAssignableFrom(clz)) {
                retval = (T) create(LengthWithNilReasonType.class);
            } else if (LengthType.class.isAssignableFrom(clz)) {
                retval = (T) create(LengthType.class);
            } else {
                retval = (T) create(MeasureType.class);
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
            Optional<NumericMeasure> measure = source.getVerticalVisibility();
            if (measure.isPresent()) {
                target.setVerticalVisibility(wrap(asMeasure(measure.get(), LengthWithNilReasonType.class), LengthWithNilReasonType.class));
            }
            if (source.getLayers().isPresent()) {
                if (source.getLayers().get().size() <= MAX_CLOUD_LAYERS) {
                    for (CloudLayer layer : source.getLayers().get()) {
                        target.getLayer().add(create(AerodromeCloudForecastType.Layer.class, (l) -> {
                            l.setCloudLayer(create(CloudLayerType.class, (cl) -> this.setCloudLayerData(cl, layer)));
                        }));
                    }
                } else {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                            "Found " + source.getLayers().get().size() + " cloud forecast " + "layers, the maximum number in IWXXM is " + MAX_CLOUD_LAYERS));
                }
            }
        }

    }

    protected void setCloudLayerData(final CloudLayerType target, final CloudLayer source) {
        if (source != null) {
            if (source.getBase().isPresent()) {
                target.setBase(asMeasure(source.getBase().get(), DistanceWithNilReasonType.class));
            }
            target.setAmount(create(CloudAmountReportedAtAerodromeType.class, (amount) -> {
                amount.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + source.getAmount().getCode());
                amount.setTitle(source.getAmount().name() + ", from codelist " + AviationCodeListUser.CODELIST_CLOUD_AMOUNT_REPORTED_AT_AERODROME);
            }));
            Optional<AviationCodeListUser.CloudType> type = source.getCloudType();
            if (type.isPresent()) {
                QName eName = new QName("http://icao.int/iwxxm/2.1", "cloudType");
                SigConvectiveCloudTypeType cloudType = create(SigConvectiveCloudTypeType.class, (convCloud) -> {
                    convCloud.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + type.get().getCode());
                    convCloud.setTitle(type.get().name() + ", from codelist " + AviationCodeListUser.CODELIST_SIGNIFICANT_CONVECTIVE_CLOUD_TYPE);
                });
                target.setCloudType(new JAXBElement<SigConvectiveCloudTypeType>(eName, SigConvectiveCloudTypeType.class, cloudType));
            }
        }
    }

    protected static class IWXXMSchemaResourceResolver implements LSResourceResolver {
        
        //Singleton
        private static IWXXMSchemaResourceResolver instance;
        public synchronized static IWXXMSchemaResourceResolver getInstance() {
            if (instance == null) {
                instance = new IWXXMSchemaResourceResolver();
            }
            return instance;
        }
        
        private static Map<String, LSInput> cache = new HashMap<String, LSInput>();
        
        private IWXXMSchemaResourceResolver() {
        }
        
        @Override
        public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
            String cacheKey = namespaceURI + ":" + normalizeSystemId(systemId);
            synchronized(cache) {
                if (cache.containsKey(cacheKey)) {
                    return cache.get(cacheKey);
                }
            }

            Class<?> cls = null;
            String path = null;
            switch (namespaceURI) {
            case "http://www.w3.org/XML/1998/namespace":
                cls = null;
                path = "org/w3/2001/03/" + normalizeSystemId(systemId);
                break;
            case "http://www.w3.org/1999/xlink":
                cls = org.w3c.xlink11.ResourceType.class;
                path = "org/w3/xlink/1.1/" + normalizeSystemId(systemId);
                break;
            case "http://www.opengis.net/gml/3.2":
                cls = net.opengis.gml32.AbstractGMLType.class;
                path = "net/opengis/gml/3.2.1/" + normalizeSystemId(systemId);
                break;
            case "http://www.isotc211.org/2005/gts":
                cls = org.iso19139.ogc2007.gts.TMPrimitivePropertyType.class;
                path = "iso/19139/20070417/gts/" + normalizeSystemId(systemId);
                break;
            case "http://www.isotc211.org/2005/gsr":
                cls = org.iso19139.ogc2007.gsr.SCCRSPropertyType.class;
                path = "iso/19139/20070417/gsr/" + normalizeSystemId(systemId);
                break;
            case "http://www.isotc211.org/2005/gss":
                cls = org.iso19139.ogc2007.gss.GMObjectPropertyType.class;
                path = "iso/19139/20070417/gss/" + normalizeSystemId(systemId);
                break;
            case "http://www.isotc211.org/2005/gco":
                cls = org.iso19139.ogc2007.gco.AbstractObjectType.class;
                path = "iso/19139/20070417/gco/" + normalizeSystemId(systemId);
                break;
            case "http://www.isotc211.org/2005/gmd":
                cls = org.iso19139.ogc2007.gmd.AbstractDQElementType.class;
                path = "iso/19139/20070417/gmd/" + normalizeSystemId(systemId);
                break;
            case "http://www.opengis.net/om/2.0":
                cls = net.opengis.om20.OMObservationPropertyType.class;
                path = "net/opengis/om/2.0/" + normalizeSystemId(systemId);
                break;
            case "http://www.opengis.net/sampling/2.0":
                cls = net.opengis.sampling.SamplingFeatureComplexType.class;
                path = "net/opengis/sampling/2.0/" + normalizeSystemId(systemId);
                break;
            case "http://www.opengis.net/samplingSpatial/2.0":
                cls = net.opengis.sampling.spatial.SFSpatialSamplingFeatureType.class;
                path = "net/opengis/samplingSpatial/2.0/" + normalizeSystemId(systemId);
                break;
            case "http://www.aixm.aero/schema/5.1.1":
                cls = aero.aixm511.CodeICAOType.class;
                path = "aero/aixm/schema/5.1.1/" + normalizeSystemId(systemId);
                break;
            case "http://def.wmo.int/metce/2013":
                cls = wmo.metce2013.ProcessType.class;
                path = "int/wmo/metce/1.2/" + normalizeSystemId(systemId);
                break;
            case "http://def.wmo.int/opm/2013":
                cls = wmo.opm2013.AbstractObservablePropertyPropertyType.class;
                path = "int/wmo/opm/1.2/" + normalizeSystemId(systemId);
                break;
            case "http://icao.int/iwxxm/2.1":
                cls = icao.iwxxm21.TAFType.class;
                path = "int/icao/iwxxm/2.1/" + normalizeSystemId(systemId);

            }
            if (path != null) {
                synchronized(cache) {
                    cache.put(cacheKey, new ClassLoaderResourceInput(cls, path, publicId, systemId, baseURI));
                    return cache.get(cacheKey);
                }
            } else {
                return null;
            }
        }

        private String normalizeSystemId(final String systemId) {
            return systemId.substring(systemId.lastIndexOf('/') + 1);
        }
    }

    protected static class ConverterValidationEventHandler<V> implements ValidationEventHandler {
        private ConversionResult<V> result;
        private boolean errorsFound = false;
        private boolean fatalErrorsFound = false;

        ConverterValidationEventHandler(final ConversionResult<V> result) {
            this.result = result;
        }


        public boolean handleEvent(ValidationEvent event) {
            if (event.getSeverity() == ValidationEvent.ERROR) {
                this.errorsFound = true;
            } else if (event.getSeverity() == ValidationEvent.FATAL_ERROR) {
                this.fatalErrorsFound = true;
            }
            this.result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "XML Schema validation issue: " + event.getMessage()));
            return true;
        }

        public boolean errorsFound() {
            return this.errorsFound || this.fatalErrorsFound;
        }

        public boolean fatalErrorsFound() {
            return this.fatalErrorsFound;
        }
    }

    protected static class ClassLoaderResourceInput implements LSInput {
        private URL url;
        private String publicId;
        private String systemId;
        private String baseURI;
        private String cachedContent;

        public ClassLoaderResourceInput(final Class<?> cls, final String path, final String publicId, final String systemId, final String baseURI) throws IllegalArgumentException {
            if (cls != null) {
                this.url = cls.getClassLoader().getResource(path);
            } else {
                this.url = ClassLoaderResourceInput.class.getClassLoader().getResource(path);
            }
            if (this.url == null) {
                throw new IllegalArgumentException("Resource '" + path + "' not found in classpath");
            }
            this.publicId = publicId;
            this.systemId = systemId;
            this.baseURI = baseURI;
        }

        @Override
        public Reader getCharacterStream() {
            if (this.url == null) return null;
            try {
                return new BufferedReader(new InputStreamReader(this.url.openStream()));
            } catch (IOException e) {
               //Noop
            }
            return null;
        }

        @Override
        public void setCharacterStream(final Reader characterStream) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public InputStream getByteStream() {
            if (this.url == null) return null;
            try {
                return this.url.openStream();
            } catch (IOException e) {
               //NOOP
            }
            return null;
        }

        @Override
        public void setByteStream(final InputStream byteStream) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getStringData() {
            if (this.url == null) return null;
            if (this.cachedContent == null) {
                Reader input = this.getCharacterStream();
                if (input != null) {
                    try {
                        StringWriter sw = new StringWriter();
                        IOUtils.copy(input, sw);
                        this.cachedContent = sw.toString();
                    } catch (IOException e) {
                       //NOOP
                    }
                }
            }
            return this.cachedContent;
        }

        @Override
        public void setStringData(final String stringData) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getSystemId() {
            return this.systemId;
        }

        @Override
        public void setSystemId(final String systemId) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getPublicId() {
            return this.publicId;
        }

        @Override
        public void setPublicId(final String publicId) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getBaseURI() {
            return this.baseURI;
        }

        @Override
        public void setBaseURI(final String baseURI) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(final String encoding) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public boolean getCertifiedText() {
            return true;
        }

        @Override
        public void setCertifiedText(final boolean certifiedText) {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}

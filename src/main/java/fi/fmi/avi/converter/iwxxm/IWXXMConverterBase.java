package fi.fmi.avi.converter.iwxxm;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;
import net.opengis.om20.TimeObjectPropertyType;

import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.helpers.DefaultHandler;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import icao.iwxxm21.ReportType;
import wmo.collect2014.MeteorologicalBulletinType;

/**
 * Helpers for creating and handling JAXB generated content classes.
 */
public abstract class IWXXMConverterBase {
    /*
      XMLConstants.FEATURE_SECURE_PROCESSING flag value "true" forces the XML schema
      loader to check the allowed protocols using the system property "javax.xml.accessExternalSchema".
      On the other hand setting XMLConstants.FEATURE_SECURE_PROCESSING to "false" is not allowed
      when SecurityManager is enabled.

      The schema loading is done using class path resource loading
      mechanism for all the schemas anyway. Unfortunately the code in
      com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler#getSchemaDocument method
      checks the schema loading permissions based on the jar:file type systemID, and thus requires
      permission for using the "file" (and subsequently "http") protocol, even if the XML Schema
      contents in this case have already been loaded into memory at this stage by
      the IWXXMSchemaResourceResolver.

    */
    protected static final boolean F_SECURE_PROCESSING;
    static {
        if (System.getSecurityManager() != null) {
            F_SECURE_PROCESSING = true;
            //A bit dangerous, as this allows the entire application to use both file and http resources
            //when the code tries to load XML Schema files.
            System.setProperty("javax.xml.accessExternalSchema", "file,http");
        } else {
            F_SECURE_PROCESSING = false;
        }
    }
    private static JAXBContext jaxbCtx = null;
    private static final Map<String, Object> CLASS_TO_OBJECT_FACTORY = new HashMap<>();
    private static final Map<String, Object> OBJECT_FACTORY_MAP = new HashMap<>();

    /**
     * Singleton for accessing the shared JAXBContext for IWXXM JAXB handling.
     *
     * NOTE: this can take several seconds when done for the first time after JVM start,
     * needs to scan all the jars in classpath.
     *
     * @return the context
     * @throws JAXBException if the context cannot be created
     */
    public static synchronized JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbCtx == null) {
            jaxbCtx = JAXBContext.newInstance("icao.iwxxm21:aero.aixm511:net.opengis.gml32:org.iso19139.ogc2007.gmd:org.iso19139.ogc2007.gco:org"
                    + ".iso19139.ogc2007.gss:org.iso19139.ogc2007.gts:org.iso19139.ogc2007.gsr:net.opengis.om20:net.opengis.sampling:net.opengis.sampling"
                    + ".spatial:wmo.metce2013:wmo.opm2013:wmo.collect2014:org.w3c.xlink11");
        }
        return jaxbCtx;
    }

    public static <T> T create(final Class<T> clz) throws IllegalArgumentException {
        return create(clz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<T> clz, final Consumer<T> consumer) throws IllegalArgumentException {
        Object result = null;
        final Object objectFactory = getObjectFactory(clz);
        if (objectFactory != null) {
            String methodName = null;
            if (clz.getEnclosingClass() != null) {
                Class<?> encClass = clz.getEnclosingClass();
                final StringBuilder sb = new StringBuilder("create").append(encClass.getSimpleName().substring(0, 1).toUpperCase())
                        .append(encClass.getSimpleName().substring(1));
                while (encClass.getEnclosingClass() != null) {
                    sb.append(clz.getSimpleName());
                    encClass = encClass.getEnclosingClass();
                }
                methodName = sb.append(clz.getSimpleName()).toString();
            } else {
                methodName = new StringBuilder("create").append(clz.getSimpleName().substring(0, 1).toUpperCase())
                        .append(clz.getSimpleName().substring(1))
                        .toString();
            }
            try {
                final Method toCall = objectFactory.getClass().getMethod(methodName);
                result = toCall.invoke(objectFactory);
            } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InvocationTargetException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to create JAXB element object for type " + clz, e);
            }
            if (consumer != null) {
                consumer.accept((T) result);
            }
        } else {
            throw new IllegalArgumentException("Unable to find ObjectFactory for JAXB element type " + clz);
        }
        return (T) result;
    }

    public static <T> JAXBElement<T> createAndWrap(final Class<T> clz) {
        return createAndWrap(clz, null);
    }

    public static <T> JAXBElement<T> createAndWrap(final Class<T> clz, final Consumer<T> consumer) {
        final T element = create(clz);
        return wrap(element, clz, consumer);
    }

    public static <T> JAXBElement<T> wrap(final T element, final Class<T> clz) {
        return wrap(element, clz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> JAXBElement<T> wrap(final T element, final Class<T> clz, final Consumer<T> consumer) {
        Object result = null;
        final Object objectFactory = getObjectFactory(clz);
        if (objectFactory != null) {
            final String methodName = new StringBuilder("create").append(clz.getSimpleName().substring(0, 1).toUpperCase())
                    .append(clz.getSimpleName().substring(1, clz.getSimpleName().lastIndexOf("Type")))
                    .toString();
            try {
                final Method toCall = objectFactory.getClass().getMethod(methodName, clz);
                result = toCall.invoke(objectFactory, element);
            } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InvocationTargetException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to create JAXBElement wrapper", e);
            }
        } else {
            throw new IllegalArgumentException("Unable to find ObjectFactory for JAXB element type " + clz);
        }
        if (consumer != null) {
            consumer.accept(element);
        }
        return (JAXBElement<T>) result;
    }
    @SuppressWarnings("unchecked")
    protected static <S> void validateDocument(final S input, final Class<S> clz, final ConversionHints hints, final ValidationEventHandler eventHandler) {
        try {
            //XML Schema validation:
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
            schemaFactory.setResourceResolver(resolver);
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, F_SECURE_PROCESSING);
            final Source[] schemaSources;
            final String schemaLocation;
            if (MeteorologicalBulletinType.class.isAssignableFrom(clz)) {
                schemaSources = new Source[2];
                schemaSources[0] = new StreamSource(ReportType.class.getResource("/int/wmo/collect/1.2/collect.xsd").toExternalForm());
                schemaSources[1] = new StreamSource(ReportType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd").toExternalForm());
                schemaLocation = "http://icao.int/iwxxm/2.1 https://schemas.wmo.int/iwxxm/2.1.1/iwxxm.xsd "
                        + "http://def.wmo.int/metce/2013 http://schemas.wmo.int/metce/1.2/metce.xsd "
                        + "http://def.wmo.int/collect/2014 http://schemas.wmo.int/collect/1.2/collect.xsd "
                        + "http://www.opengis.net/samplingSpatial/2.0 http://schemas.opengis.net/samplingSpatial/2.0/spatialSamplingFeature.xsd";
            } else {
                schemaSources = new Source[1];
                schemaSources[0] = new StreamSource(ReportType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd").toExternalForm());
                schemaLocation = "http://icao.int/iwxxm/2.1 https://schemas.wmo.int/iwxxm/2.1.1/iwxxm.xsd "
                        + "http://def.wmo.int/metce/2013 http://schemas.wmo.int/metce/1.2/metce.xsd "
                        + "http://www.opengis.net/samplingSpatial/2.0 http://schemas.opengis.net/samplingSpatial/2.0/spatialSamplingFeature.xsd";
            }
            final Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation);
            marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new IWXXMNamespaceContext());

            marshaller.setSchema(schemaFactory.newSchema(schemaSources));
            marshaller.setEventHandler(eventHandler);
            //Marshall to run the validation:
            marshaller.marshal(wrap(input, clz), new DefaultHandler());
        } catch (final Exception e) {
            throw new RuntimeException("Error in validating document", e);
        }
    }

    private static Object getObjectFactory(final Class<?> clz) {
        Object objectFactory = null;
        try {
            synchronized (OBJECT_FACTORY_MAP) {

                objectFactory = CLASS_TO_OBJECT_FACTORY.get(clz.getCanonicalName());
                if (objectFactory == null) {
                    String objectFactoryPath = clz.getPackage().getName();
                    String objectFactoryName = null;
                    Class<?> ofClass = null;
                    while (objectFactory == null && objectFactoryPath != null) {
                        objectFactoryName = objectFactoryPath + ".ObjectFactory";
                        objectFactory = OBJECT_FACTORY_MAP.get(objectFactoryName);
                        if (objectFactory == null) {
                            try {
                                ofClass = IWXXMConverterBase.class.getClassLoader().loadClass(objectFactoryName);
                                break;
                            } catch (final ClassNotFoundException cnfe) {
                                final int nextDot = objectFactoryPath.lastIndexOf('.');
                                if (nextDot == -1) {
                                    objectFactoryPath = null;
                                } else {
                                    objectFactoryPath = objectFactoryPath.substring(0, nextDot);
                                }
                            }
                        }
                    }
                    if (ofClass != null) {
                        final Constructor<?> c = ofClass.getConstructor();
                        objectFactory = c.newInstance();
                        OBJECT_FACTORY_MAP.put(objectFactoryName, objectFactory);
                    }
                    CLASS_TO_OBJECT_FACTORY.put(clz.getCanonicalName(), objectFactory);
                }
            }
            return objectFactory;
        } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to get ObjectFactory for " + clz.getCanonicalName(), e);
        }
    }
    public static <T> Optional<T> resolveProperty(final Object prop, final Class<T> clz, final ReferredObjectRetrievalContext refCtx) {
        return resolveProperty(prop, null, clz, refCtx);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> resolveProperty(final Object prop, final String propertyName, final Class<T> clz, final ReferredObjectRetrievalContext refCtx) {
        if (prop == null) {
            return Optional.empty();
        }
        try {
            //First try resolving the href reference (if it exists):
            try {
                final Method getHref = prop.getClass().getMethod("getHref", (Class<?>[]) null);
                if (String.class.isAssignableFrom(getHref.getReturnType())) {
                    String id = (String) getHref.invoke(prop, (Object[]) null);
                    if (id != null) {
                        if (id.startsWith("#")) {
                            id = id.substring(1);
                        }
                        return refCtx.getReferredObject(id, clz);
                    }
                }
            } catch (final NoSuchMethodException nsme) {
                //NOOP
            }

            //Then try to return embedded property value:
            String getterCandidate = null;
            if (propertyName != null) {
                getterCandidate = "get" + StringUtils.capitalize(propertyName);
            } else if (clz.getSimpleName().endsWith("Type")) {
                getterCandidate = "get" + clz.getSimpleName().substring(0, clz.getSimpleName().length() - 4);
            }
            if (getterCandidate != null) {
                Method getObject;
                try {
                    getObject = prop.getClass().getMethod(getterCandidate, (Class<?>[]) null);
                    if (clz.isAssignableFrom(getObject.getReturnType())) {
                        return (Optional<T>) Optional.ofNullable(getObject.invoke(prop, (Object[]) null));
                    } else if (JAXBElement.class.isAssignableFrom(getObject.getReturnType())) {
                        final JAXBElement<?> wrapped = (JAXBElement<?>) getObject.invoke(prop, (Object[]) null);
                        if (wrapped != null) {
                            final Object value = wrapped.getValue();
                            if (value != null) {
                                if (clz.isAssignableFrom(value.getClass())) {
                                    return (Optional<T>) Optional.of(value);
                                }
                            }
                        }
                    }
                } catch (final NoSuchMethodException nsme) {
                    try {
                        getObject = prop.getClass().getMethod("getAny", (Class<?>[]) null);
                        final Object wrapper = getObject.invoke(prop, (Object[]) null);
                        if (wrapper != null && JAXBElement.class.isAssignableFrom(wrapper.getClass())) {
                            final Object value = ((JAXBElement) wrapper).getValue();
                            return (Optional<T>) Optional.of(value);
                        }
                    } catch (final NoSuchMethodException nsme2) {
                        //NOOP
                    }
                }
            }
        } catch (final IllegalAccessException | InvocationTargetException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    protected static Optional<PartialOrCompleteTimePeriod> getCompleteTimePeriod(final TimeObjectPropertyType timeObjectPropertyType,
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

    protected static Optional<PartialOrCompleteTimePeriod> getCompleteTimePeriod(final TimePeriodPropertyType timePeriodPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<TimePeriodType> tp = resolveProperty(timePeriodPropertyType, TimePeriodType.class, refCtx);
        if (tp.isPresent()) {
            final PartialOrCompleteTimePeriod.Builder retval = PartialOrCompleteTimePeriod.builder();
            getStartTime(tp.get(), refCtx).ifPresent((start) -> {
                retval.setStartTime(PartialOrCompleteTimeInstant.builder()//
                        .setCompleteTime(start).build());
            });

            getEndTime(tp.get(), refCtx).ifPresent((end) -> {
                retval.setEndTime(PartialOrCompleteTimeInstant.builder()//
                        .setCompleteTime(end).build());
            });
            return Optional.of(retval.build());
        }
        return Optional.empty();
    }

    protected static Optional<PartialOrCompleteTimeInstant> getCompleteTimeInstant(final TimeObjectPropertyType timeObjectPropertyType,
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

    protected static Optional<PartialOrCompleteTimeInstant> getCompleteTimeInstant(final TimeInstantPropertyType timeInstantPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<ZonedDateTime> time = getTime(timeInstantPropertyType, refCtx);
        if (time.isPresent()) {
            return Optional.of(PartialOrCompleteTimeInstant.builder().setCompleteTime(time.get()).build());
        }
        return Optional.empty();
    }

    protected static Optional<ZonedDateTime> getStartTime(final TimePeriodType period, final ReferredObjectRetrievalContext ctx) {
        Optional<ZonedDateTime> retval = Optional.empty();
        if (period.getBegin() != null) {
            retval = getTime(period.getBegin(), ctx);
        } else if (period.getBeginPosition() != null) {
            retval = getTime(period.getBeginPosition());
        }
        return retval;
    }

    protected static Optional<ZonedDateTime> getEndTime(final TimePeriodType period, final ReferredObjectRetrievalContext ctx) {
        Optional<ZonedDateTime> retval = Optional.empty();
        if (period.getEnd() != null) {
            retval = getTime(period.getEnd(), ctx);
        } else if (period.getEndPosition() != null) {
            retval = getTime(period.getEndPosition());
        }
        return retval;
    }

    protected static Optional<ZonedDateTime> getTime(final TimeInstantPropertyType tiProp, final ReferredObjectRetrievalContext ctx) {
        final Optional<TimeInstantType> ti = resolveProperty(tiProp, TimeInstantType.class, ctx);
        if (ti.isPresent()) {
            return getTime(ti.get().getTimePosition());
        } else {
            return Optional.empty();
        }
    }

    protected static Optional<ZonedDateTime> getTime(final TimePositionType tp) {
        if (tp != null && tp.getValue() != null && !tp.getValue().isEmpty()) {
            return Optional.of(ZonedDateTime.parse(tp.getValue().get(0), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } else {
            return Optional.empty();
        }
    }

    protected static Document parseStringToDOM(final String input) throws ConversionException {
        Document retval = null;
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, F_SECURE_PROCESSING);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
            retval = db.parse(bais);
        } catch (final Exception e) {
            throw new ConversionException("Error in parsing input as to an XML document", e);
        }
        return retval;
    }
}

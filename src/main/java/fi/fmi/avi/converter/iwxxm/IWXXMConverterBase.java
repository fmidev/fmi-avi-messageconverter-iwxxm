package fi.fmi.avi.converter.iwxxm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

/**
 * Helpers for creating and handling JAXB generated content classes.
 */
public abstract class IWXXMConverterBase {
    public static final String UUID_PREFIX = "uuid.";
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

    private static final Logger LOG = LoggerFactory.getLogger(IWXXMConverterBase.class);
    private static final Map<String, Object> CLASS_TO_OBJECT_FACTORY = new HashMap<>();
    private static final Map<String, Object> OBJECT_FACTORY_MAP = new HashMap<>();
    private static final HashMap<String, Templates> IWXXM_TEMPLATES = new HashMap<>();

    private static JAXBContext jaxbCtx = null;

    static {
        if (System.getSecurityManager() != null) {
            F_SECURE_PROCESSING = true;
            //A bit dangerous, as this allows the entire application to use both file and http resources
            //when the code tries to load XML Schema files.
            LOG.info("SecurityManager detected, setting system property 'javax.xml.accessExternalSchema' to 'file,http,https' to allow loading of XML Schemas");
            System.setProperty("javax.xml.accessExternalSchema", "file,http,https");
        } else {
            F_SECURE_PROCESSING = false;
        }
    }

    /**
     * Singleton for accessing the shared JAXBContext for IWXXM JAXB handling.
     *
     * NOTE: this can take several seconds when done for the first time after JVM start,
     * needs to scan all the jars in classpath.
     *
     * @return the context
     *
     * @throws JAXBException
     *         if the context cannot be created
     */
    public static synchronized JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbCtx == null) {
            jaxbCtx = JAXBContext.newInstance("icao.iwxxm21:icao.iwxxm30:aero.aixm511:net.opengis.gml32:org.iso19139.ogc2007.gmd:org.iso19139.ogc2007.gco:org"
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
        final Object result;
        final Object objectFactory = getObjectFactory(clz);
        final String methodName;
        if (clz.getEnclosingClass() != null) {
            Class<?> encClass = clz.getEnclosingClass();
            final StringBuilder sb = new StringBuilder("create").append(encClass.getSimpleName().substring(0, 1).toUpperCase(Locale.US))
                    .append(encClass.getSimpleName().substring(1));
            while (encClass.getEnclosingClass() != null) {
                sb.append(clz.getSimpleName());
                encClass = encClass.getEnclosingClass();
            }
            methodName = sb.append(clz.getSimpleName()).toString();
        } else {
            methodName = "create" + clz.getSimpleName().substring(0, 1).toUpperCase(Locale.US) + clz.getSimpleName().substring(1);
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
        return (T) result;
    }

    public static <T> JAXBElement<T> createAndWrap(final Class<T> clz) {
        return createAndWrap(clz, null);
    }

    public static <T> JAXBElement<T> createAndWrap(final Class<T> clz, final Consumer<T> consumer) {
        final T element = create(clz);
        return wrap(element, clz, consumer);
    }

    public static <T> JAXBElement<T> createAndWrap(final Class<T> clz, final String methodName, final Consumer<T> consumer) {
        final T element = create(clz);
        return wrap(element, clz, methodName, consumer);
    }

    public static <T> JAXBElement<T> wrap(final T element, final Class<T> clz) {
        return wrap(element, clz, null);
    }

    public static <T> JAXBElement<T> wrap(final T element, final Class<T> clz, final Consumer<T> consumer) {
        final String methodName = "create" + clz.getSimpleName().substring(0, 1).toUpperCase(Locale.US) + clz.getSimpleName()
                .substring(1, clz.getSimpleName().lastIndexOf("Type"));
        return wrap(element, clz, methodName, consumer);
    }

    @SuppressWarnings("unchecked")
    public static <T> JAXBElement<T> wrap(final T element, final Class<T> clz, final String methodName, final Consumer<T> consumer) {
        final Object result;
        final Object objectFactory = getObjectFactory(clz);
        try {
            final Method toCall = objectFactory.getClass().getMethod(methodName, clz);
            result = toCall.invoke(objectFactory, element);
        } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InvocationTargetException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to create JAXBElement wrapper", e);
        }
        if (consumer != null) {
            consumer.accept(element);
        }
        return (JAXBElement<T>) result;
    }

    protected static IssueList validateAgainstSchema(final Source input, final XMLSchemaInfo schemaInfo, final ConversionHints hints)
            throws ConversionException {
        final IssueList retval = new IssueList();
        try {
            final Validator validator = schemaInfo.getSchema().newValidator();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(final SAXParseException exception) throws SAXException {
                    retval.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                            "XML Schema validation warning at line " + exception.getLineNumber() + ", column " + exception.getColumnNumber() + ":"
                                    + exception.getMessage(), exception);
                }

                @Override
                public void error(final SAXParseException exception) throws SAXException {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "XML Schema validation error at line " + exception.getLineNumber() + ", column " + exception.getColumnNumber() + ":"
                                    + exception.getMessage(), exception);
                    throw exception;
                }

                @Override
                public void fatalError(final SAXParseException exception) throws SAXException {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "Fatal XML Schema validation error at line " + exception.getLineNumber() + ", column " + exception.getColumnNumber() + ":"
                                    + exception.getMessage(), exception);
                    throw exception;
                }
            });
            try {
                validator.validate(input);
            } catch (final SAXException | IOException e) {
                //noop, issues have already been collected by the error handler
            }
        } catch (final RuntimeException | SAXException e) {
            throw new ConversionException("Error in validating document", e);
        }
        return retval;
    }

    protected static <S> IssueList validateJAXBObjectAgainstSchemaAndSchematron(final S input, final Class<S> clz, final XMLSchemaInfo schemaInfo,
            final ConversionHints hints) {
        final IssueList retval = new IssueList();
        try {
            final Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemaInfo.getCombinedSchemaLocations());
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new IWXXMNamespaceContext());

            marshaller.setSchema(schemaInfo.getSchema());
            final ConverterValidationEventHandler eventHandler = new ConverterValidationEventHandler(retval);
            marshaller.setEventHandler(eventHandler);

            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document dom = db.newDocument();

            //Marshall to run the validation:
            marshaller.marshal(wrap(input, clz), dom);

            retval.addAll(eventHandler.getIssues());

            //Schematron validation:
            retval.addAll(validateAgainstIWXXMSchematron(dom, schemaInfo, hints));
        } catch (final RuntimeException | JAXBException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException("Error in validating document", e);
        }
        return retval;
    }

    protected static IssueList validateDOMAgainstSchemaAndSchematron(final Document input, final XMLSchemaInfo schemaInfo, final ConversionHints hints)
            throws ConversionException {
        final IssueList retval = new IssueList();
        retval.addAll(validateAgainstSchema(new DOMSource(input.getDocumentElement()), schemaInfo, hints));
        retval.addAll(validateAgainstIWXXMSchematron(input, schemaInfo, hints));
        return retval;
    }

    protected static IssueList validateStringAgainstSchemaAndSchematron(final String input, final XMLSchemaInfo schemaInfo, final ConversionHints hints)
            throws ConversionException {
        final IssueList retval = new IssueList();
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document dom = db.parse(new InputSource(new StringReader(input)));
            retval.addAll(validateAgainstSchema(new StreamSource(new StringReader(input)), schemaInfo, hints));
            retval.addAll(validateAgainstIWXXMSchematron(dom, schemaInfo, hints));
        } catch (final ParserConfigurationException | SAXException | IOException e) {
            throw new ConversionException("Error validating produced bulletin", e);
        }
        return retval;
    }

    /**
     * Checks the DOM Document against the official IWXXM Schematron validation rules.
     * Uses a pre-generated XLS transformation files producing Schematron SVRL reports.
     *
     * @param input
     *         IWXXM message Document
     * @param hints
     *         conversion hints to guide the validaton
     *
     * @return the list of Schematron validation issues (failed asserts)
     */
    protected static IssueList validateAgainstIWXXMSchematron(final Document input, final XMLSchemaInfo schemaInfo, final ConversionHints hints) {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new IWXXMNamespaceContext());
        final IssueList retval = new IssueList();
        try {
            final DOMSource dSource = new DOMSource(input);
            final URIResolver localResolver = (href, base) -> {
                try {
                    final URI maybeRelative = URI.create(href);
                    if (!maybeRelative.isAbsolute()) {
                        final Optional<URL> baseURL = schemaInfo.getSchematronRules().stream().filter(url -> url.toExternalForm().equals(base)).findAny();
                        if (baseURL.isPresent()) {
                            final String ruleBase = baseURL.get().toExternalForm();
                            return new StreamSource(new URL(ruleBase.substring(0, ruleBase.lastIndexOf('/') + 1) + href).openStream());
                        } else {
                            return new StreamSource(maybeRelative.toURL().openStream());
                        }
                    } else {
                        return new StreamSource(maybeRelative.toURL().openStream());
                    }
                } catch (final IOException e) {
                    throw new TransformerException("Unable to resolve XSL resource '" + href + "'", e);
                }
            };
            DOMResult schematronOutput;
            Transformer transformer;
            NodeList failedAsserts;
            for (final Templates xsl : getIwxxmTemplates(schemaInfo)) {
                schematronOutput = new DOMResult();
                transformer = xsl.newTransformer();
                //Try to resolve the relative XSL external references, such as the ones provided using the 'document()' function, to the jar resources to URLs in
                // the same directory where the xsl file is contained in. Fallback to opening the URL as-is as a stream:
                transformer.setURIResolver(localResolver);
                transformer.transform(dSource, schematronOutput);
                failedAsserts = (NodeList) xPath.evaluate("//svrl:failed-assert/svrl:text", schematronOutput.getNode(), XPathConstants.NODESET);
                if (failedAsserts != null) {
                    for (int i = 0; i < failedAsserts.getLength(); i++) {
                        final Node node = failedAsserts.item(i).getFirstChild();
                        retval.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX, "Failed Schematron assertation: " + node.getNodeValue());
                    }
                }
            }
        } catch (final TransformerException | XPathExpressionException e) {
            throw new RuntimeException("Unable to apply XSLT pre-compiled Schematron validation rules to the document to validate", e);
        }
        return retval;
    }

    /*
       Performance optimization: use a pre-compiled the Templates object
       for running the XSL transformations required for IWXXM Schematron
       validation. This makes each validation 3-4 times faster.
   */
    private static List<Templates> getIwxxmTemplates(final XMLSchemaInfo schemaInfo) throws TransformerException {
        if (schemaInfo.getSchematronRules() == null) {
            throw new TransformerException("No Schematron rules source available in XMLSchemaInfo");
        }
        final List<Templates> retval = new ArrayList<>();
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        for (final URL ruleURL : schemaInfo.getSchematronRules()) {
            final String ruleURLString = ruleURL.toString();
            synchronized (IWXXM_TEMPLATES) {
                if (!IWXXM_TEMPLATES.containsKey(ruleURLString)) {
                    try (InputStream inputStream = ruleURL.openStream()) {
                        IWXXM_TEMPLATES.put(ruleURLString, transformerFactory.newTemplates(new StreamSource(inputStream, ruleURLString)));
                    } catch (final IOException e) {
                        LOG.warn("Unable to create StreamSource for the schematron rule from '{}'", ruleURL.toExternalForm(), e);
                    }
                }
                retval.add(IWXXM_TEMPLATES.get(ruleURLString));
            }
        }
        return toUnmodifiableList(retval);
    }

    private static Object getObjectFactory(final Class<?> clz) {
        Object objectFactory;
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
        } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | IllegalAccessError | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to get ObjectFactory for " + clz.getCanonicalName(), e);
        }
        if (objectFactory == null) {
            throw new IllegalArgumentException("Unable to find ObjectFactory for JAXB element type " + clz);
        }
        return objectFactory;
    }

    public static <T> Optional<T> resolveProperty(final Object prop, final Class<T> clz, final ReferredObjectRetrievalContext refCtx) {
        return resolveProperty(prop, null, clz, refCtx);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> resolveProperty(final Object prop, final String propertyName, final Class<T> clz,
            final ReferredObjectRetrievalContext refCtx) {
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
                            return Optional.of(((JAXBElement<T>) wrapper).getValue());
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

    protected static Optional<PartialOrCompleteTimePeriod> getCompleteTimePeriod(final TimePeriodPropertyType timePeriodPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<TimePeriodType> tp = resolveProperty(timePeriodPropertyType, TimePeriodType.class, refCtx);
        if (tp.isPresent()) {
            final PartialOrCompleteTimePeriod.Builder retval = PartialOrCompleteTimePeriod.builder();
            getStartTime(tp.get(), refCtx).ifPresent(start -> retval.setStartTime(PartialOrCompleteTimeInstant.builder()//
                    .setCompleteTime(start).build()));

            getEndTime(tp.get(), refCtx).ifPresent(end -> retval.setEndTime(PartialOrCompleteTimeInstant.builder()//
                    .setCompleteTime(end).build()));
            return Optional.of(retval.build());
        }
        return Optional.empty();
    }

    protected static Optional<PartialOrCompleteTimeInstant> getCompleteTimeInstant(final TimeInstantPropertyType timeInstantPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        return getTime(timeInstantPropertyType, refCtx)//
                .map(zonedDateTime -> PartialOrCompleteTimeInstant.builder().setCompleteTime(zonedDateTime).build());
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
        final Document retval;
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, F_SECURE_PROCESSING);
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
                retval = documentBuilder.parse(inputStream);
            }
        } catch (final RuntimeException | ParserConfigurationException | IOException | SAXException e) {
            throw new ConversionException("Error in parsing input as to an XML document", e);
        }
        return retval;
    }

    protected static String renderDOMToString(final Document source, final ConversionHints hints) throws ConversionException {
        if (source != null) {
            try {
                final StringWriter sw = new StringWriter();
                final Result output = new StreamResult(sw);
                final TransformerFactory tFactory = TransformerFactory.newInstance();
                final Transformer transformer = tFactory.newTransformer();

                //TODO: switch these on based on the ConversionHints:
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                final DOMSource dsource = new DOMSource(source);
                transformer.transform(dsource, output);
                return sw.toString();
            } catch (final TransformerException e) {
                throw new ConversionException("Exception in rendering to String", e);
            }
        }
        return null;
    }

    protected static <E> List<E> toUnmodifiableList(final List<E> list) {
        return list.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    protected static <E> Collector<E, ?, List<E>> toImmutableList() {
        return Collectors.collectingAndThen(Collectors.<E> toList(), IWXXMConverterBase::toUnmodifiableList);
    }

    protected static Document copyAsDocument(final Element sourceElement) throws ParserConfigurationException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, F_SECURE_PROCESSING);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        document.appendChild(document.importNode(sourceElement, true));
        final Element documentElement = document.getDocumentElement();
        final Set<String> referredNamespaces = scanReferredNamespaces(documentElement);
        copyXmlnsAttributes(sourceElement, documentElement, referredNamespaces);
        mergeSchemaLocationAttribute(sourceElement, documentElement, referredNamespaces);
        return document;
    }

    /**
     * Scans provided {@code nodeToScan}, its attributes and child nodes recursively and returns a set of found namespace URIs.
     *
     * @param nodeToScan
     *         node to scan
     *
     * @return set of found namespace URIs
     */
    private static Set<String> scanReferredNamespaces(final Node nodeToScan) {
        final Set<String> scannedNamespaces = new LinkedHashSet<>();
        scanReferredNamespaces(scannedNamespaces, nodeToScan);
        return scannedNamespaces;
    }

    private static void scanReferredNamespaces(final Set<String> scannedNamespaces, final Node nodeToScan) {
        final String namespaceURI = nodeToScan.getNamespaceURI();
        if (namespaceURI != null) {
            scannedNamespaces.add(namespaceURI);
        }
        if (nodeToScan.hasAttributes()) {
            final NamedNodeMap attributes = nodeToScan.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                scanReferredNamespaces(scannedNamespaces, attributes.item(i));
            }
        }
        if (nodeToScan.hasChildNodes()) {
            final NodeList childNodes = nodeToScan.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                scanReferredNamespaces(scannedNamespaces, childNodes.item(i));
            }
        }
    }

    private static void copyXmlnsAttributes(final Element sourceElement, final Element targetElement, final Set<String> includeNamespaces) {
        parentsStream(sourceElement, false)//
                .filter(Node::hasAttributes)//
                .flatMap(node -> streamOf(node.getAttributes()))//
                .map(node -> (Attr) node)//
                .filter(attr -> XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getPrefix())//
                        && includeNamespaces.contains(attr.getValue())//
                        && sourceElement.getAttributes().getNamedItemNS(attr.getNamespaceURI(), attr.getLocalName()) == null)//
                .collect(Collectors.toMap(Attr::getLocalName, Function.identity(),
                        // on duplicate local name (namespace prefix) retain value closest to sourceElement
                        (oldValue, newValue) -> oldValue))//
                .values()//
                .forEach(attr -> targetElement.setAttributeNodeNS((Attr) targetElement.getOwnerDocument().importNode(attr, true)));
    }

    private static Stream<Node> parentsStream(final Node leafNode, final boolean includeLeaf) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Node>() {
            private Node next = includeLeaf ? leafNode : leafNode.getParentNode();

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Node next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final Node current = next;
                next = current.getParentNode();
                return current;
            }
        }, Spliterator.NONNULL), false);
    }

    private static Stream<Node> streamOf(final NamedNodeMap namedNodeMap) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Node>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < namedNodeMap.getLength();
            }

            @Override
            public Node next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return namedNodeMap.item(index++);
            }
        }, Spliterator.NONNULL), false);
    }

    private static void mergeSchemaLocationAttribute(final Element sourceElement, final Element targetElement, final Set<String> includeNamespaces) {
        final String originalSchemaLocation = targetElement.getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                XMLSchemaInfo.SCHEMA_LOCATION_ATTRIBUTE);
        final Map<String, String> originalSchemaLocationMap = XMLSchemaInfo.decodeSchemaLocation(originalSchemaLocation);
        // no need to include leaf in parentsStream: source and target elements are expected to be equal
        final Map<String, String> schemaLocationsToMerge = parentsStream(sourceElement, false)//
                .filter(node -> node instanceof Element && node.hasAttributes())//
                .map(node -> XMLSchemaInfo.decodeSchemaLocation(
                        ((Element) node).getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, XMLSchemaInfo.SCHEMA_LOCATION_ATTRIBUTE)).entrySet())
                .filter(entries -> !entries.isEmpty())//
                .collect(Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), list -> {
                    Collections.reverse(list);
                    return list;
                }))//
                .stream()//
                .flatMap(Collection::stream)//
                .filter(entry -> includeNamespaces.contains(entry.getKey()) && !originalSchemaLocationMap.containsKey(entry.getKey()))//
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        // on duplicate namespace, retain location closest to sourceElement
                        (oldValue, newValue) -> newValue, LinkedHashMap::new));
        if (!schemaLocationsToMerge.isEmpty()) {
            final String schemaLocation = (originalSchemaLocation.isEmpty() ? "" : originalSchemaLocation + " ") //
                    + XMLSchemaInfo.encodeSchemaLocation(schemaLocationsToMerge);
            // Looking for prefix in sourceElement belonging to a complete document, targetElement is expected to be a subtree copy of sourceElement.
            final String prefix = Optional.ofNullable(sourceElement.lookupPrefix(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)).orElse("xsi");
            targetElement.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, prefix + ":" + XMLSchemaInfo.SCHEMA_LOCATION_ATTRIBUTE, schemaLocation);
        }
    }

    protected static class ConverterValidationEventHandler implements ValidationEventHandler {
        private final IssueList issues;
        private boolean errorsFound = false;
        private boolean fatalErrorsFound = false;

        public ConverterValidationEventHandler(final IssueList issues) {
            this.issues = issues;
        }

        public ConverterValidationEventHandler() {
            this.issues = new IssueList();
        }

        @Override
        public boolean handleEvent(final ValidationEvent event) {
            final ConversionIssue.Severity severity;
            if (event.getSeverity() == ValidationEvent.ERROR) {
                this.errorsFound = true;
                severity = ConversionIssue.Severity.ERROR;
            } else if (event.getSeverity() == ValidationEvent.FATAL_ERROR) {
                this.fatalErrorsFound = true;
                severity = ConversionIssue.Severity.ERROR;
            } else if (event.getSeverity() == ValidationEvent.WARNING) {
                severity = ConversionIssue.Severity.WARNING;
            } else {
                severity = ConversionIssue.Severity.INFO;
            }
            this.issues.add(new ConversionIssue(severity, ConversionIssue.Type.SYNTAX, "XML Schema validation issue: " + event.getMessage()));
            return true;
        }

        public boolean errorsFound() {
            return this.errorsFound || this.fatalErrorsFound;
        }

        public boolean fatalErrorsFound() {
            return this.fatalErrorsFound;
        }

        public IssueList getIssues() {
            return this.issues;
        }
    }
}

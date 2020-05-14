package fi.fmi.avi.converter.iwxxm;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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

import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.TimePrimitivePropertyType;

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
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;

/**
 * Common functionality for serializing aviation messages into IWXXM.
 */
public abstract class AbstractIWXXMSerializer<T extends AviationWeatherMessageOrCollection, S> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<T, S>  {

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

                //TODO: switch these on based on the ConversionHints:
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

    private Document asCleanedUpXML(final String input , final ConversionHints hints) throws ConversionException {
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

    protected abstract InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException;

    protected abstract XMLSchemaInfo getSchemaInfo();

        public ConversionResult<?> getResult() {
            return result;
        }
    }
}

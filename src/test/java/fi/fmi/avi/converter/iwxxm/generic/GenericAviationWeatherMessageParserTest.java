package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public final class GenericAviationWeatherMessageParserTest extends XMLTestCase implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private Document readDocumentFromResource(final String name) throws Exception {
        try (final InputStream inputStream = getClass().getResourceAsStream(name)) {
            requireNonNull(inputStream, name);
            return readDocument(inputStream);
        }
    }

    private Document readDocumentFromString(final String xmlString) throws Exception {
        try (final InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))) {
            return readDocument(inputStream);
        }
    }

    private Document readDocument(final InputStream inputStream) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(inputStream);
    }

    @Test
    public void specificationTest() {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO)).isTrue();
        assertThat(converter.isSpecificationSupported(IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO)).isTrue();
    }

    @Test
    public void namespacesAreCopiedFromCollectToMessage() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-collect.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-collect.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    private void testNamespaceDeclarations(final String bulletinResourceName, final String expectedResultResourceName)
            throws Exception {
        final String input = readResourceToString(bulletinResourceName);

        final ConversionResult<GenericMeteorologicalBulletin> result = converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_STRING_TO_GENERIC_BULLETIN_POJO);
        assertThat(result.getConversionIssues()).isEmpty();

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        final List<String> messages = result.getConvertedMessage()
                .map(bulletin -> bulletin.getMessages().stream()
                        .map(GenericAviationWeatherMessage::getOriginalMessage)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        assertThat(messages).hasSize(1);

        final Document expectedMessage1 = readDocumentFromResource(expectedResultResourceName);
        final Document actualDocument = readDocumentFromString(messages.get(0));
        assertXMLIdentical(XMLUnit.compareXML(expectedMessage1, actualDocument), true);
    }

    @Test
    public void namespacesAreRetainedInMessage() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-message.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-message.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void extraNamespacesAreRetainedInMessage() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-extra.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-extra.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void namespacesAreMergedIfNeeded() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-mixed.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-mixed.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }
}


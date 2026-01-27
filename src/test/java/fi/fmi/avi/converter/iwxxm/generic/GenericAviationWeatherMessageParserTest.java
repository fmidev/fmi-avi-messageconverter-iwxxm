package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.List;
import java.util.stream.Collectors;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public final class GenericAviationWeatherMessageParserTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void specificationTest() {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO)).isTrue();
        assertThat(converter.isSpecificationSupported(IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO)).isTrue();
    }

    @Test
    public void namespacesAreCopiedFromCollectToMessage() throws Exception {
        final String bulletinResourceName = "taf/iwxxm-30-taf-bulletin-namespaces-collect.xml";
        final String expectedResultResourceName = "taf/iwxxm-30-taf-namespaces-collect.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    private void testNamespaceDeclarations(final String bulletinResourceName, final String expectedResultResourceName)
            throws Exception {
        final String input = readResourceToString(bulletinResourceName);

        final ConversionResult<GenericMeteorologicalBulletin> result = converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_STRING_TO_GENERIC_BULLETIN_POJO);
        final GenericMeteorologicalBulletin bulletin = assertConversionResult(result).isSuccessful();

        final List<String> messages = bulletin.getMessages().stream()
                .map(GenericAviationWeatherMessage::getOriginalMessage)
                .collect(Collectors.toList());
        assertThat(messages).hasSize(1);

        final String expectedMessageXml = readResourceToString(expectedResultResourceName);
        IWXXMConverterTests.assertXMLEqualsIgnoringVariables(expectedMessageXml, messages.get(0));
    }

    @Test
    public void namespacesAreRetainedInMessage() throws Exception {
        final String bulletinResourceName = "taf/iwxxm-30-taf-bulletin-namespaces-message.xml";
        final String expectedResultResourceName = "taf/iwxxm-30-taf-namespaces-message.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void extraNamespacesAreRetainedInMessage() throws Exception {
        final String bulletinResourceName = "taf/iwxxm-30-taf-bulletin-namespaces-extra.xml";
        final String expectedResultResourceName = "taf/iwxxm-30-taf-namespaces-extra.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void namespacesAreMergedIfNeeded() throws Exception {
        final String bulletinResourceName = "taf/iwxxm-30-taf-bulletin-namespaces-mixed.xml";
        final String expectedResultResourceName = "taf/iwxxm-30-taf-namespaces-mixed.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }
}

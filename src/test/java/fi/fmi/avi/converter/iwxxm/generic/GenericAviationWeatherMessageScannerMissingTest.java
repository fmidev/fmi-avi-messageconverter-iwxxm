package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = GenericAviationWeatherMessageScannerMissingTest.Config.class, loader = AnnotationConfigContextLoader.class)
public class GenericAviationWeatherMessageScannerMissingTest implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testParserWithTAF30() throws IOException, ParserConfigurationException, SAXException {
        final Document input = readDocumentFromResource("taf/iwxxm-30-taf-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.ERROR
                        && issue.getMessage().contains("Unsupported message type"));
    }

    @Configuration
    @Import(IWXXMTestConfiguration.class)
    public static class Config {
        @Bean
        @Qualifier("genericAviationMessageScannerMap")
        public Map<IWXXMMessageType, GenericAviationWeatherMessageScanner> genericAviationMessageScannerMap() {
            return Collections.unmodifiableMap(new EnumMap<>(IWXXMMessageType.class));
        }
    }
}

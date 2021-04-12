package fi.fmi.avi.converter.iwxxm;

import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.assertXMLEqualsIgnoringVariables;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.model.AviationWeatherMessage;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public abstract class AbstractIWXXMReserializeTest<T extends AviationWeatherMessage> implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;

    protected void testReserialize(final String fileName) throws IOException, SAXException {
        final String input = readResourceToString(fileName);

        final T parseResult = convertAndAssert(input, getIwxxmToPojoConversionSpecification());
        final String serializationResult = convertAndAssert(parseResult, getPojoToIwxxmConversionSpecification());
        assertXMLEqualsIgnoringVariables(input, serializationResult);
    }

    private <I, R> R convertAndAssert(final I input, final ConversionSpecification<I, R> conversionSpecification) {
        final ConversionResult<R> result = converter.convertMessage(input, conversionSpecification, ConversionHints.EMPTY);
        final String issues = result.getConversionIssues().stream()//
                .map(Object::toString)//
                .collect(Collectors.joining("\n", String.format(Locale.ROOT, "Conversion issues during %s:%n", conversionSpecification), "\n"));
        assertSame(issues, ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(issues, result.getConversionIssues().isEmpty());
        return result.getConvertedMessage().orElseThrow(() -> new AssertionError("Expected result of conversion to exist."));
    }

    protected abstract ConversionSpecification<T, String> getPojoToIwxxmConversionSpecification();

    protected abstract ConversionSpecification<String, T> getIwxxmToPojoConversionSpecification();
}

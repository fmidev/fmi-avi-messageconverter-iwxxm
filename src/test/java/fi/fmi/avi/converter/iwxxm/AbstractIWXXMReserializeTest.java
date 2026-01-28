package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.model.AviationWeatherMessage;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.xml.sax.SAXException;

import java.io.IOException;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public abstract class AbstractIWXXMReserializeTest<T extends AviationWeatherMessage> implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;

    protected void testReserialize(final String fileName) throws IOException, SAXException {
        final String input = readResourceToString(fileName);

        final T parseResult = convertAndAssert(input, getIwxxmToPojoConversionSpecification());
        final ConversionResult<String> serializationResult = converter.convertMessage(parseResult, getPojoToIwxxmConversionSpecification(), ConversionHints.EMPTY);
        assertThatConversionResult(serializationResult).isSuccessful().hasXmlMessageEqualTo(input);
    }

    private <I, R> R convertAndAssert(final I input, final ConversionSpecification<I, R> conversionSpecification) {
        final ConversionResult<R> result = converter.convertMessage(input, conversionSpecification, ConversionHints.EMPTY);
        return assertThatConversionResult(result).isSuccessful().getMessage();
    }

    protected abstract ConversionSpecification<T, String> getPojoToIwxxmConversionSpecification();

    protected abstract ConversionSpecification<String, T> getIwxxmToPojoConversionSpecification();
}

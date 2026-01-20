package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMMessageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class IWXXMGenericAviationWeatherMessageConverterTest {
    @Autowired
    private IWXXMGenericAviationWeatherMessageConverter iwxxmGenericAviationWeatherMessageConverter;

    @Test
    public void scannerIsConfiguredForAllKnownIWXXMMessageTypes() {
        final Map<IWXXMMessageType, GenericAviationWeatherMessageScanner> scanners = iwxxmGenericAviationWeatherMessageConverter.genericAviationMessageScannerMap();
        assertThat(scanners.keySet())
                .containsExactlyInAnyOrder(IWXXMMessageType.values());
        assertThat(scanners)
                .doesNotContainValue(null);
    }
}

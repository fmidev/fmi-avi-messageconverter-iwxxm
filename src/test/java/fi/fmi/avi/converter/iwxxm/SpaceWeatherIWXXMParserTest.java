package fi.fmi.avi.converter.iwxxm;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMParserTest extends DOMParsingTestBase {
    @Autowired
    private AviMessageConverter converter;

//    @Ignore
    @Test
    public void testString3Parser() throws Exception {
        //TODO: Fix resource loading
        //fi.fmi.avi.converter.iwxxm
        InputStream is = SpaceWeatherIWXXMParserTest.class.getResourceAsStream("spacewx-A2-3.xml");

        if(is == null) {
            String path = System.getProperty("user.dir") + "/src/test/resources/fi/fmi/avi/converter/iwxxm/spacewx-A2-3.xml";
            File file = new File(path);
            if(file.exists()) {
                is = new FileInputStream(file);
            } else {
                throw new FileNotFoundException("could not locate file as resource or using absolute path " + path);
            }
        }
        Objects.requireNonNull(is);
        String input = IOUtils.toString(is,"UTF-8");
        is.close();
        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Ignore
    @Test
    public void testString4Parser() throws Exception {
        //TODO: Fix resource loading
        //fi.fmi.avi.converter.iwxxm
        InputStream is = SpaceWeatherIWXXMParserTest.class.getResourceAsStream("spacewx-A2-4.xml");

        if(is == null) {
            String path = System.getProperty("user.dir") + "/src/test/resources/fi/fmi/avi/converter/iwxxm/spacewx-A2-4.xml";
            File file = new File(path);
            if(file.exists()) {
                is = new FileInputStream(file);
            } else {
                throw new FileNotFoundException("could not locate file as resource or using absolute path " + path);
            }
        }
        Objects.requireNonNull(is);
        String input = IOUtils.toString(is,"UTF-8");
        is.close();
        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);
        for(ConversionIssue issue : result.getConversionIssues()) {
            System.out.println(issue.getMessage());
        }
        System.out.println();
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }
}

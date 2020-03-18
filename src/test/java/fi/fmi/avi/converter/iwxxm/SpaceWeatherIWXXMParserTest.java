package fi.fmi.avi.converter.iwxxm;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

    private String getInput(String fileName) throws IOException {
        InputStream is = null;
        try {
            String path = System.getProperty("user.dir") + "/src/test/resources/fi/fmi/avi/converter/iwxxm/" + fileName;
            File file = new File(path);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                throw new FileNotFoundException("could not locate file as resource or using path " + path);
            }
            Objects.requireNonNull(is);

            return IOUtils.toString(is, "UTF-8");
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }

    @Test
    public void testParser_A2_3() throws Exception {
        String input = getInput("spacewx-A2-3.xml");

        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);

        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    //TODO: does not work
    //XML Schema validation issue: unexpected element (uri:"http://www.aixm.aero/schema/5.1.1", local:"maximumLimit"). Expected elements are <{http://www.aixm.aero/schema/5.1.1}extension>,<{http://www.opengis.net/gml/3.2}descriptionReference>,<{http://www.aixm.aero/schema/5.1.1}lowerLimitReference>,<{http://www.opengis.net/gml/3.2}description>,<{http://www.aixm.aero/schema/5.1.1}horizontalProjection>,<{http://www.opengis.net/gml/3.2}name>,<{http://www.aixm.aero/schema/5.1.1}upperLimit>,<{http://www.aixm.aero/schema/5.1.1}lowerLimit>,<{http://www.opengis.net/gml/3.2}identifier>,<{http://www.aixm.aero/schema/5.1.1}upperLimitReference>,<{http://www.opengis.net/gml/3.2}metaDataProperty>
    @Ignore
    @Test
    public void testParser_A2_4() throws Exception {
        String input = getInput("spacewx-A2-4.xml");

        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);
        for(ConversionIssue con : result.getConversionIssues()) {
            //System.out.println(con.getCause());
            System.out.println(con.getMessage());
        }
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testParser_A2_5() throws Exception {
        String input = getInput("spacewx-A2-5.xml");

        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }
}

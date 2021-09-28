# fmi-avi-messageconverter-iwxxm
fmi-avi-messageconverter module for IWXXM messages

This project provides conversions between the aviation weather message Java domain model objects defined
in [fmi-avi-messageconverter](https://github.com/fmidev/fmi-avi-messageconverter) and 
the ICAO Meteorological Information Exchange Model (IWXXM) encoded XML documents.

## Get started
Release artifacts of project are available as maven dependencies in the FMI OS maven repository. To access them, 
add this repository to your project pom, or in your settings:

```xml
<repositories>
  <repository>
    <id>fmi-os-mvn-release-repo</id>
    <url>https://raw.githubusercontent.com/fmidev/fmi-os-mvn-repo/master</url>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
    <releases>
      <enabled>true</enabled>
      <updatePolicy>daily</updatePolicy>
    </releases>
  </repository>
</repositories>
```

Maven dependency:

```xml
<dependency>
  <groupId>fi.fmi.avi.converter</groupId>
  <artifactId>fmi-avi-messageconverter-iwxxm</artifactId>
  <version>[the release version]</version>
</dependency>
```

The recommended way to using the IWXXM message conversions provided by this project is to inject the conversion 
functionality to the AviMessageParser instance using Spring:

```java
package my.stuff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.model.taf.TAF;

@Configuration
@Import(fi.fmi.avi.converter.iwxxm.IWXXMConverter.class)
public class MyMessageConverterConfig {

    @Autowired
    private AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<Document, TAF> tafIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafIWXXMStringParser;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        final AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM, tafIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING, tafIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, tafIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, tafIWXXMDOMParser);
        return p;
    }

}
```

If you want to be able to convert to/from other message encodings (such at TAC) too, include the conversion 
modules for them as maven dependencies and add the required converters to the AviMessageConverter configuration.
See [fmi-avi-messageconverter](https://github.com/fmidev/fmi-avi-messageconverter) for more information.


## Supported message conversions

Identifier                                                          | Input                             | Output
--------------------------------------------------------------------|-----------------------------------|-------
fi.fmi.avi.converter.iwxxm.IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING | instance of fi.fmi.avi.model.TAF | IWXXM TAF report as a String
fi.fmi.avi.converter.iwxxm.IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM | instance of fi.fmi.avi.model.TAF | IWXXM TAF report as a DOM Document
fi.fmi.avi.converter.iwxxm.IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO | IWXXM TAF report as a String | instance of fi.fmi.avi.model.TAF
fi.fmi.avi.converter.iwxxm.IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO | IWXXM TAF report as a DOM Document | instance of fi.fmi.avi.model.TAF 

Currently only conversions between TAF Java objects and the IWXXM 2.1 TAF messages are supported, but it's expected that the METAR, 
SPECI, SIGMET and AIRMET support will be added as the project becomes more mature.

## Examples

Converting from TAF object to IWXXM TAF Report as String:

```java
TAF pojo = getTAF();
ConversionResult<String> result = converter.convertMessage(pojo, IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING);
if (ConversionResult.Status.SUCCESS == result.getStatus()) {
    System.out.println(result.getConvertedMessage().get());
} else {
    for (ConversionIssue issue:result.getConversionIssues()) {
        System.err.println(issue);
    }
}

```




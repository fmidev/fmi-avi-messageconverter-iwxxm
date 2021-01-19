# IWXXM Converter

## Basic concept

The IWXXM converter converts XML messages to POJOS or POJOS to XML messages. The basic conversion is a simple JAXB transformation. The classes created by JAXB
are very complex multilayered objects. The main aim of this converter is to move the data from the overly complex object to a simpler, more usable data model.
The serializer and parser classes are split into version specific packages.

## Serializer

The IWXXM serializer converts a `fi.fmi.avi.model.CLASS` class into a `icao.iwxxm30.CLASS`. Once that conversion is complete JAXB is used to serialize the
message into a IWXXM XML string. The xml string is then clean up using an xslt transformation. The XSL is placed in the resource folder and is referred to in
the `getCleanupTransformationStylesheet` method which in turn is inherited from `fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer`.

### Adding a New Serializer

1. Create a new package for the new conversion type under `fi.fmi.avi.converter.iwxxm.v3_0`.
2. Create a serializer class.

* Serializer class should extend `fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer`.

3. Add XSL as `resources.CONVERSION_OBJECT.OBJECT_NAMECleanUp.xsl`.

* This XSL can be used to clean up unwanted namespaces or elements.
* At a bare minimum the transformation should return the XML as is.

## Parser

When an IWXXM message is being parsed after the JAXB conversion the data extraction is handled in a Scanner class. This seeks to avoid unnecessary crashes and
to report any errors that might be found. The scanner creates a property class that contains all the necessary properties from the XML object. The properties
are then placed into the model class by the main parser class

### Adding a New Parser

1. Create a new package for the new conversion type under `fi.fmi.avi.converter.iwxxm.v3_0`.
2. Create a property classes that contain an enumeration entry for each data field that needs to be extracted.

* Property class should extend `fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer`.

3. Create `IWXXMScanner` class. The scanner should extend an IWXXM version specific extension of `fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner`.

* The scanner should also contain a method that returns a `List` of `fi.fmi.avi.converter.ConversionIssues` and modifies property classes as a side effect.

4. Create serializer class that extends `fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer`.

### Spring Configuration instructions

1. Create `AviMessageSpecificConverter` beans in a `Converter` class in the `fi.fmi.avi.converter.iwxxm.conf` package.
2. Add the class with the beans to the import annotation in `fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter`.
3. Add `ConversionSpecification` in `fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter`.
4. Autowire the `AviMessageSpecificConverter` bean in your `Configuration` class.
5. Create `AviMessageConverter` bean in your configuration class and set the autowired message specific converter.

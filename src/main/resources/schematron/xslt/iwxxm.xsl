<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet version="2.0" xmlns:aixm="http://www.aixm.aero/schema/5.1.1" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:iwxxm="http://icao.int/iwxxm/2.1"
  xmlns:om="http://www.opengis.net/om/2.0" xmlns:sams="http://www.opengis.net/samplingSpatial/2.0"
  xmlns:sf="http://www.opengis.net/sampling/2.0" xmlns:svrl="http://purl.oclc.org/dsdl/svrl" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--Implementers: please note that overriding process-prolog or process-root is
      the preferred method for meta-stylesheets to use where possible. -->

  <xsl:param name="archiveDirParameter" />
  <xsl:param name="archiveNameParameter" />
  <xsl:param name="fileNameParameter" />
  <xsl:param name="fileDirParameter" />
  <xsl:variable name="document-uri">
    <xsl:value-of select="document-uri(/)" />
  </xsl:variable>

  <!--PHASES-->


  <!--PROLOG-->
  <xsl:output indent="yes" method="xml" omit-xml-declaration="no" standalone="yes" />

  <!--XSD TYPES FOR XSLT2-->


  <!--KEYS AND FUNCTIONS-->


  <!--DEFAULT RULES-->


  <!--MODE: SCHEMATRON-SELECT-FULL-PATH-->
  <!--This mode can be used to generate an ugly though full XPath for locators-->
  <xsl:template match="*" mode="schematron-select-full-path">
    <xsl:apply-templates mode="schematron-get-full-path" select="." />
  </xsl:template>

  <!--MODE: SCHEMATRON-FULL-PATH-->
  <!--This mode can be used to generate an ugly though full XPath for locators-->
  <xsl:template match="*" mode="schematron-get-full-path">
    <xsl:apply-templates mode="schematron-get-full-path" select="parent::*" />
    <xsl:text>/</xsl:text>
    <xsl:choose>
      <xsl:when test="namespace-uri()=''">
        <xsl:value-of select="name()" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>*:</xsl:text>
        <xsl:value-of select="local-name()" />
        <xsl:text>[namespace-uri()='</xsl:text>
        <xsl:value-of select="namespace-uri()" />
        <xsl:text>']</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:variable name="preceding"
      select="count(preceding-sibling::*[local-name()=local-name(current())                                   and namespace-uri() = namespace-uri(current())])" />
    <xsl:text>[</xsl:text>
    <xsl:value-of select="1+ $preceding" />
    <xsl:text>]</xsl:text>
  </xsl:template>
  <xsl:template match="@*" mode="schematron-get-full-path">
    <xsl:apply-templates mode="schematron-get-full-path" select="parent::*" />
    <xsl:text>/</xsl:text>
    <xsl:choose>
      <xsl:when test="namespace-uri()=''">@<xsl:value-of select="name()" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>@*[local-name()='</xsl:text>
        <xsl:value-of select="local-name()" />
        <xsl:text>' and namespace-uri()='</xsl:text>
        <xsl:value-of select="namespace-uri()" />
        <xsl:text>']</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--MODE: SCHEMATRON-FULL-PATH-2-->
  <!--This mode can be used to generate prefixed XPath for humans-->
  <xsl:template match="node() | @*" mode="schematron-get-full-path-2">
    <xsl:for-each select="ancestor-or-self::*">
      <xsl:text>/</xsl:text>
      <xsl:value-of select="name(.)" />
      <xsl:if test="preceding-sibling::*[name(.)=name(current())]">
        <xsl:text>[</xsl:text>
        <xsl:value-of select="count(preceding-sibling::*[name(.)=name(current())])+1" />
        <xsl:text>]</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:if test="not(self::*)">
      <xsl:text />/@<xsl:value-of select="name(.)" />
    </xsl:if>
  </xsl:template>
  <!--MODE: SCHEMATRON-FULL-PATH-3-->
  <!--This mode can be used to generate prefixed XPath for humans
    (Top-level element has index)-->

  <xsl:template match="node() | @*" mode="schematron-get-full-path-3">
    <xsl:for-each select="ancestor-or-self::*">
      <xsl:text>/</xsl:text>
      <xsl:value-of select="name(.)" />
      <xsl:if test="parent::*">
        <xsl:text>[</xsl:text>
        <xsl:value-of select="count(preceding-sibling::*[name(.)=name(current())])+1" />
        <xsl:text>]</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:if test="not(self::*)">
      <xsl:text />/@<xsl:value-of select="name(.)" />
    </xsl:if>
  </xsl:template>

  <!--MODE: GENERATE-ID-FROM-PATH -->
  <xsl:template match="/" mode="generate-id-from-path" />
  <xsl:template match="text()" mode="generate-id-from-path">
    <xsl:apply-templates mode="generate-id-from-path" select="parent::*" />
    <xsl:value-of select="concat('.text-', 1+count(preceding-sibling::text()), '-')" />
  </xsl:template>
  <xsl:template match="comment()" mode="generate-id-from-path">
    <xsl:apply-templates mode="generate-id-from-path" select="parent::*" />
    <xsl:value-of select="concat('.comment-', 1+count(preceding-sibling::comment()), '-')" />
  </xsl:template>
  <xsl:template match="processing-instruction()" mode="generate-id-from-path">
    <xsl:apply-templates mode="generate-id-from-path" select="parent::*" />
    <xsl:value-of select="concat('.processing-instruction-', 1+count(preceding-sibling::processing-instruction()), '-')" />
  </xsl:template>
  <xsl:template match="@*" mode="generate-id-from-path">
    <xsl:apply-templates mode="generate-id-from-path" select="parent::*" />
    <xsl:value-of select="concat('.@', name())" />
  </xsl:template>
  <xsl:template match="*" mode="generate-id-from-path" priority="-0.5">
    <xsl:apply-templates mode="generate-id-from-path" select="parent::*" />
    <xsl:text>.</xsl:text>
    <xsl:value-of select="concat('.',name(),'-',1+count(preceding-sibling::*[name()=name(current())]),'-')" />
  </xsl:template>

  <!--MODE: GENERATE-ID-2 -->
  <xsl:template match="/" mode="generate-id-2">U</xsl:template>
  <xsl:template match="*" mode="generate-id-2" priority="2">
    <xsl:text>U</xsl:text>
    <xsl:number count="*" level="multiple" />
  </xsl:template>
  <xsl:template match="node()" mode="generate-id-2">
    <xsl:text>U.</xsl:text>
    <xsl:number count="*" level="multiple" />
    <xsl:text>n</xsl:text>
    <xsl:number count="node()" />
  </xsl:template>
  <xsl:template match="@*" mode="generate-id-2">
    <xsl:text>U.</xsl:text>
    <xsl:number count="*" level="multiple" />
    <xsl:text>_</xsl:text>
    <xsl:value-of select="string-length(local-name(.))" />
    <xsl:text>_</xsl:text>
    <xsl:value-of select="translate(name(),':','.')" />
  </xsl:template>
  <!--Strip characters-->
  <xsl:template match="text()" priority="-1" />

  <!--SCHEMA SETUP-->
  <xsl:template match="/">
    <svrl:schematron-output schemaVersion="" title="Schematron validation">
      <xsl:comment>
        <xsl:value-of select="$archiveDirParameter" />  
        <xsl:value-of select="$archiveNameParameter" />  
        <xsl:value-of select="$fileNameParameter" />  
        <xsl:value-of select="$fileDirParameter" />
      </xsl:comment>
      <svrl:ns-prefix-in-attribute-values prefix="iwxxm" uri="http://icao.int/iwxxm/2.1" />
      <svrl:ns-prefix-in-attribute-values prefix="sf" uri="http://www.opengis.net/sampling/2.0" />
      <svrl:ns-prefix-in-attribute-values prefix="sams" uri="http://www.opengis.net/samplingSpatial/2.0" />
      <svrl:ns-prefix-in-attribute-values prefix="xlink" uri="http://www.w3.org/1999/xlink" />
      <svrl:ns-prefix-in-attribute-values prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance" />
      <svrl:ns-prefix-in-attribute-values prefix="om" uri="http://www.opengis.net/om/2.0" />
      <svrl:ns-prefix-in-attribute-values prefix="gml" uri="http://www.opengis.net/gml/3.2" />
      <svrl:ns-prefix-in-attribute-values prefix="aixm" uri="http://www.aixm.aero/schema/5.1.1" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ARS1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ARS1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M9" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ARS2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ARS2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M10" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ARVR1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ARVR1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M11" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORep2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORep2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M12" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORep1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORep1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M13" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORep6</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORep6</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M14" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORep3</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORep3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M15" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORep7</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORep7</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M16" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORep4</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORep4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M17" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORep5</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORep5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M18" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASS1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASS1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M19" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASS3</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASS3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M20" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASS4</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASS4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M21" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASS2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASS2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M22" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASS5</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASS5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M23" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.AWS1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.AWS1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M24" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MATFR5</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MATFR5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M25" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MATFR1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MATFR1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M26" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MATFR2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MATFR2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M27" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MATFR4</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MATFR4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M28" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MATFR3</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MATFR3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M29" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec6</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec6</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M30" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec4</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M31" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec3</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M32" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M33" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M34" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec7</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec7</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M35" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec8</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec8</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M36" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.MAORec5</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.MAORec5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M37" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.AOC1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.AOC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M38" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.AOC2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.AOC2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M39" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASW3</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASW3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M40" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASW4</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASW4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M41" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASW5</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASW5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M42" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASW6</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASW6</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M43" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASW2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASW2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M44" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASW7</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASW7</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M45" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.ASW1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.ASW1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M46" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.AHV1</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.AHV1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M47" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.AHV2</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.AHV2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M48" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">METAR_SPECI.AHV3</xsl:attribute>
        <xsl:attribute name="name">METAR_SPECI.AHV3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M49" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.MAFR2</xsl:attribute>
        <xsl:attribute name="name">TAF.MAFR2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M50" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.MAFR1</xsl:attribute>
        <xsl:attribute name="name">TAF.MAFR1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M51" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.MAFR3</xsl:attribute>
        <xsl:attribute name="name">TAF.MAFR3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M52" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.MAFR4</xsl:attribute>
        <xsl:attribute name="name">TAF.MAFR4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M53" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.AATF1</xsl:attribute>
        <xsl:attribute name="name">TAF.AATF1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M54" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.AATF2</xsl:attribute>
        <xsl:attribute name="name">TAF.AATF2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M55" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF1</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M56" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF18</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF18</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M57" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF3</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M58" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF4</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M59" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF5</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M60" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF9</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF9</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M61" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF2</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M62" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF11</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF11</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M63" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF8</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF8</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M64" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF14</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF14</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M65" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF16</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF16</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M66" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF12</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF12</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M67" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF15</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF15</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M68" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF17</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF17</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M69" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF13</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF13</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M70" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TAF.TAF6</xsl:attribute>
        <xsl:attribute name="name">TAF.TAF6</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M71" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET9</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET9</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M72" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET1</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M73" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET2</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M74" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET10</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET10</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M75" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET4</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M76" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET7</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET7</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M77" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET3</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M78" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET8</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET8</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M79" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SIGMET5</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SIGMET5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M80" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SEC1</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SEC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M81" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SEC3</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SEC3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M82" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SEC4</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SEC4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M83" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SEC2</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SEC2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M84" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SECC3</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SECC3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M85" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SECC2</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SECC2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M86" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SECC1</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SECC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M87" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">SIGMET.SPC1</xsl:attribute>
        <xsl:attribute name="name">SIGMET.SPC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M88" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AECC2</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AECC2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M89" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AECC1</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AECC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M90" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC1</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M91" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC2</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M92" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC3</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M93" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC9</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC9</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M94" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC10</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC10</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M95" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC4</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M96" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC5</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M97" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC7</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC7</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M98" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC6</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC6</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M99" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AEC8</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AEC8</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M100" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AIRMET5</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AIRMET5</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M101" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AIRMET2</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AIRMET2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M102" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AIRMET3</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AIRMET3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M103" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AIRMET4</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AIRMET4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M104" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">AIRMET.AIRMET1</xsl:attribute>
        <xsl:attribute name="name">AIRMET.AIRMET1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M105" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCFC1</xsl:attribute>
        <xsl:attribute name="name">TCA.TCFC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M106" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCA4</xsl:attribute>
        <xsl:attribute name="name">TCA.TCA4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M107" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCA2</xsl:attribute>
        <xsl:attribute name="name">TCA.TCA2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M108" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCA3</xsl:attribute>
        <xsl:attribute name="name">TCA.TCA3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M109" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCA1</xsl:attribute>
        <xsl:attribute name="name">TCA.TCA1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M110" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCOC1</xsl:attribute>
        <xsl:attribute name="name">TCA.TCOC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M111" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCOC2</xsl:attribute>
        <xsl:attribute name="name">TCA.TCOC2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M112" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCOC3</xsl:attribute>
        <xsl:attribute name="name">TCA.TCOC3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M113" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">TCA.TCOC4</xsl:attribute>
        <xsl:attribute name="name">TCA.TCOC4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M114" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">VAA.VAC1</xsl:attribute>
        <xsl:attribute name="name">VAA.VAC1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M115" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">VAA.VAC2</xsl:attribute>
        <xsl:attribute name="name">VAA.VAC2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M116" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">VAA.VAC3</xsl:attribute>
        <xsl:attribute name="name">VAA.VAC3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M117" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">VAA.VAC4</xsl:attribute>
        <xsl:attribute name="name">VAA.VAC4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M118" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">VAA.VAA2</xsl:attribute>
        <xsl:attribute name="name">VAA.VAA2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M119" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">VAA.VAA1</xsl:attribute>
        <xsl:attribute name="name">VAA.VAA1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M120" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.CL1</xsl:attribute>
        <xsl:attribute name="name">COMMON.CL1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M121" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.Report4</xsl:attribute>
        <xsl:attribute name="name">COMMON.Report4</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M122" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.Report2</xsl:attribute>
        <xsl:attribute name="name">COMMON.Report2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M123" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.Report1</xsl:attribute>
        <xsl:attribute name="name">COMMON.Report1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M124" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.Report3</xsl:attribute>
        <xsl:attribute name="name">COMMON.Report3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M125" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.ACF1</xsl:attribute>
        <xsl:attribute name="name">COMMON.ACF1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M126" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.ACF2</xsl:attribute>
        <xsl:attribute name="name">COMMON.ACF2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M127" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.ASWF1</xsl:attribute>
        <xsl:attribute name="name">COMMON.ASWF1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M128" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.ASWTF1</xsl:attribute>
        <xsl:attribute name="name">COMMON.ASWTF1</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M129" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.ASWTF2</xsl:attribute>
        <xsl:attribute name="name">COMMON.ASWTF2</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M130" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">COMMON.ASWTF3</xsl:attribute>
        <xsl:attribute name="name">COMMON.ASWTF3</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M131" select="/" />
      <svrl:active-pattern>
        <xsl:attribute name="document">
          <xsl:value-of select="document-uri(/)" />
        </xsl:attribute>
        <xsl:attribute name="id">IWXXM.ExtensionAlwaysLast</xsl:attribute>
        <xsl:attribute name="name">IWXXM.ExtensionAlwaysLast</xsl:attribute>
        <xsl:apply-templates />
      </svrl:active-pattern>
      <xsl:apply-templates mode="M132" select="/" />
    </svrl:schematron-output>
  </xsl:template>

  <!--SCHEMATRON PATTERNS-->
  <svrl:text>Schematron validation</svrl:text>

  <!--PATTERN METAR_SPECI.ARS1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeRunwayState" mode="M9" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeRunwayState" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@allRunways eq 'true') then( empty(iwxxm:runway) ) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@allRunways eq 'true') then( empty(iwxxm:runway) ) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ARS1: When all runways are being reported upon, no specific Runway should be reported</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M9" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M9" priority="-1" />
  <xsl:template match="@*|node()" mode="M9" priority="-2">
    <xsl:apply-templates mode="M9" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ARS2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeRunwayState" mode="M10" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeRunwayState" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( exists(iwxxm:runway) ) then( empty(@allRunways) or (@allRunways eq 'false') ) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( exists(iwxxm:runway) ) then( empty(@allRunways) or (@allRunways eq 'false') ) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ARS2: When a single Runway is reported upon, the allRunways flag should be missing or false</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M10" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M10" priority="-1" />
  <xsl:template match="@*|node()" mode="M10" priority="-2">
    <xsl:apply-templates mode="M10" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ARVR1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeRunwayVisualRange" mode="M11" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeRunwayVisualRange" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:meanRVR) and (not(exists(iwxxm:meanRVR/@xsi:nil)) or iwxxm:meanRVR/@xsi:nil != 'true')) then (iwxxm:meanRVR/@uom = 'm') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:meanRVR) and (not(exists(iwxxm:meanRVR/@xsi:nil)) or iwxxm:meanRVR/@xsi:nil != 'true')) then (iwxxm:meanRVR/@uom = 'm') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ARVR1: meanRVR shall be reported in metres (m).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M11" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M11" priority="-1" />
  <xsl:template match="@*|node()" mode="M11" priority="-2">
    <xsl:apply-templates mode="M11" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORep2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:METAR|//iwxxm:SPECI" mode="M12" priority="1000">
    <svrl:fired-rule context="//iwxxm:METAR|//iwxxm:SPECI" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if( empty(iwxxm:observation//iwxxm:cloud/iwxxm:AerodromeObservedClouds) and ends-with(iwxxm:observation//iwxxm:cloud/@nilReason, 'notDetectedByAutoSystem') ) then(@automatedStation eq 'true') else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if( empty(iwxxm:observation//iwxxm:cloud/iwxxm:AerodromeObservedClouds) and ends-with(iwxxm:observation//iwxxm:cloud/@nilReason, 'notDetectedByAutoSystem') ) then(@automatedStation eq 'true') else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORep2: When no clouds are detected by the auto system, this report must be an auto report</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M12" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M12" priority="-1" />
  <xsl:template match="@*|node()" mode="M12" priority="-2">
    <xsl:apply-templates mode="M12" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORep1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:METAR|//iwxxm:SPECI" mode="M13" priority="1000">
    <svrl:fired-rule context="//iwxxm:METAR|//iwxxm:SPECI" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(@status eq 'MISSING') then( exists(iwxxm:observation//om:result/@nilReason) and ((empty(@automatedStation) or (@automatedStation eq 'false')) and empty(iwxxm:trendForecast)) ) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(@status eq 'MISSING') then( exists(iwxxm:observation//om:result/@nilReason) and ((empty(@automatedStation) or (@automatedStation eq 'false')) and empty(iwxxm:trendForecast)) ) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORep1: Missing reports only include identifying information (time, aerodrome) and no other information</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M13" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M13" priority="-1" />
  <xsl:template match="@*|node()" mode="M13" priority="-2">
    <xsl:apply-templates mode="M13" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORep6-->


  <!--RULE -->
  <xsl:template match="//iwxxm:METAR|//iwxxm:SPECI" mode="M14" priority="1000">
    <svrl:fired-rule context="//iwxxm:METAR|//iwxxm:SPECI" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(.//iwxxm:trendForecast) and not((count(.//iwxxm:trendForecast) eq 1) and (.//iwxxm:trendForecast = ''))) then(empty(distinct-values(for $trend-forecast in .//iwxxm:trendForecast return((deep-equal(.//iwxxm:observation/om:OM_Observation/om:featureOfInterest//sf:sampledFeature,$trend-forecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature)) or (concat('#', current()//iwxxm:observation/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/@gml:id)=$trend-forecast/om:OM_Observation/om:featureOfInterest/@xlink:href)))[.=false()])) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(.//iwxxm:trendForecast) and not((count(.//iwxxm:trendForecast) eq 1) and (.//iwxxm:trendForecast = ''))) then(empty(distinct-values(for $trend-forecast in .//iwxxm:trendForecast return((deep-equal(.//iwxxm:observation/om:OM_Observation/om:featureOfInterest//sf:sampledFeature,$trend-forecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature)) or (concat('#', current()//iwxxm:observation/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/@gml:id)=$trend-forecast/om:OM_Observation/om:featureOfInterest/@xlink:href)))[.=false()])) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORep6: The sampled feature should be equal in observation and trendForecast</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M14" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M14" priority="-1" />
  <xsl:template match="@*|node()" mode="M14" priority="-2">
    <xsl:apply-templates mode="M14" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORep3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:METAR|//iwxxm:SPECI" mode="M15" priority="1000">
    <svrl:fired-rule context="//iwxxm:METAR|//iwxxm:SPECI" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(((exists(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:AirportHeliport)) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'aerodrome')) ) and ( if(exists(.//om:OM_Observation/om:featureOfInterest/@xlink:href)) then(concat( '#', current()//om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id ) = .//om:OM_Observation/om:featureOfInterest/@xlink:href) else(true())))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(((exists(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:AirportHeliport)) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'aerodrome')) ) and ( if(exists(.//om:OM_Observation/om:featureOfInterest/@xlink:href)) then(concat( '#', current()//om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id ) = .//om:OM_Observation/om:featureOfInterest/@xlink:href) else(true())))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORep3: The sampled feature for a METAR/SPECI observation is an aerodrome</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M15" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M15" priority="-1" />
  <xsl:template match="@*|node()" mode="M15" priority="-2">
    <xsl:apply-templates mode="M15" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORep7-->


  <!--RULE -->
  <xsl:template match="//iwxxm:METAR|//iwxxm:SPECI" mode="M16" priority="1000">
    <svrl:fired-rule context="//iwxxm:METAR|//iwxxm:SPECI" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORep7: The procedure of a METAR/SPECI observation should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M16" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M16" priority="-1" />
  <xsl:template match="@*|node()" mode="M16" priority="-2">
    <xsl:apply-templates mode="M16" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORep4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:METAR|//iwxxm:SPECI" mode="M17" priority="1000">
    <svrl:fired-rule context="//iwxxm:METAR|//iwxxm:SPECI" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:result/*[name() != 'iwxxm:MeteorologicalAerodromeObservationRecord']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:result/*[name() != 'iwxxm:MeteorologicalAerodromeObservationRecord']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORep4: The result of a METAR/SPECI observation should be a MeteorologicalAerodromeObservationRecord</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M17" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M17" priority="-1" />
  <xsl:template match="@*|node()" mode="M17" priority="-2">
    <xsl:apply-templates mode="M17" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORep5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:METAR|//iwxxm:SPECI" mode="M18" priority="1000">
    <svrl:fired-rule context="//iwxxm:METAR|//iwxxm:SPECI" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:trendForecast)) then(not(exists(iwxxm:trendForecast//om:result/*[name() != 'iwxxm:MeteorologicalAerodromeTrendForecastRecord']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:trendForecast)) then(not(exists(iwxxm:trendForecast//om:result/*[name() != 'iwxxm:MeteorologicalAerodromeTrendForecastRecord']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORep5: The result of a METAR/SPECI trendForecast should be a MeteorologicalAerodromeTrendForecastRecord</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M18" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M18" priority="-1" />
  <xsl:template match="@*|node()" mode="M18" priority="-2">
    <xsl:apply-templates mode="M18" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASS1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSeaState" mode="M19" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSeaState" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( exists(iwxxm:seaState) ) then ( empty(iwxxm:significantWaveHeight) ) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( exists(iwxxm:seaState) ) then ( empty(iwxxm:significantWaveHeight) ) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASS1: If the sea state is set, significantWaveHeight is not reported (one or the other)</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M19" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M19" priority="-1" />
  <xsl:template match="@*|node()" mode="M19" priority="-2">
    <xsl:apply-templates mode="M19" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASS3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSeaState" mode="M20" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSeaState" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( empty(iwxxm:seaState) ) then ( exists(iwxxm:significantWaveHeight) ) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( empty(iwxxm:seaState) ) then ( exists(iwxxm:significantWaveHeight) ) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASS3: Either seaState or significantWaveHeight must be present</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M20" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M20" priority="-1" />
  <xsl:template match="@*|node()" mode="M20" priority="-2">
    <xsl:apply-templates mode="M20" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASS4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSeaState" mode="M21" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSeaState" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:seaSurfaceTemperature) and (not(exists(iwxxm:seaSurfaceTemperature/@xsi:nil)) or iwxxm:seaSurfaceTemperature/@xsi:nil != 'true')) then (iwxxm:seaSurfaceTemperature/@uom = 'Cel') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:seaSurfaceTemperature) and (not(exists(iwxxm:seaSurfaceTemperature/@xsi:nil)) or iwxxm:seaSurfaceTemperature/@xsi:nil != 'true')) then (iwxxm:seaSurfaceTemperature/@uom = 'Cel') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASS4: seaSurfaceTemperature shall be reported in degrees Celsius (Cel).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M21" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M21" priority="-1" />
  <xsl:template match="@*|node()" mode="M21" priority="-2">
    <xsl:apply-templates mode="M21" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASS2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSeaState" mode="M22" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSeaState" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( exists(iwxxm:significantWaveHeight) ) then ( empty(iwxxm:seaState) ) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( exists(iwxxm:significantWaveHeight) ) then ( empty(iwxxm:seaState) ) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASS2: If the significantWaveHeight is set, seaState is not reported (one or the other)</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M22" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M22" priority="-1" />
  <xsl:template match="@*|node()" mode="M22" priority="-2">
    <xsl:apply-templates mode="M22" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASS5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSeaState" mode="M23" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSeaState" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:significantWaveHeight) and (not(exists(iwxxm:significantWaveHeight/@xsi:nil)) or iwxxm:significantWaveHeight/@xsi:nil != 'true')) then (iwxxm:significantWaveHeight/@uom = 'm') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:significantWaveHeight) and (not(exists(iwxxm:significantWaveHeight/@xsi:nil)) or iwxxm:significantWaveHeight/@xsi:nil != 'true')) then (iwxxm:significantWaveHeight/@uom = 'm') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASS5: significantWaveHeight shall be reported in metres (m).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M23" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M23" priority="-1" />
  <xsl:template match="@*|node()" mode="M23" priority="-2">
    <xsl:apply-templates mode="M23" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.AWS1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeWindShear" mode="M24" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeWindShear" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @allRunways eq 'true' ) then( empty(iwxxm:runway) ) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @allRunways eq 'true' ) then( empty(iwxxm:runway) ) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.AWS1: When all runways are affected by wind shear, no specific runways should be reported</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M24" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M24" priority="-1" />
  <xsl:template match="@*|node()" mode="M24" priority="-2">
    <xsl:apply-templates mode="M24" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MATFR5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" mode="M25" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if( @changeIndicator eq 'NO_SIGNIFICANT_CHANGES' ) then (empty(iwxxm:prevailingVisibility) and empty(iwxxm:prevailingVisibilityOperator) and empty(iwxxm:clouds) and empty(iwxxm:forecastWeather) and empty(iwxxm:cloudAndVisibilityOK)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if( @changeIndicator eq 'NO_SIGNIFICANT_CHANGES' ) then (empty(iwxxm:prevailingVisibility) and empty(iwxxm:prevailingVisibilityOperator) and empty(iwxxm:clouds) and empty(iwxxm:forecastWeather) and empty(iwxxm:cloudAndVisibilityOK)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MATFR5: prevailingVisibility, prevailingVisibilityOperator, clouds, forecastWeather and cloudAndVisibilityOK should be absent when changeIndicator equals 'NO_SIGNIFICANT_CHANGES'</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M25" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M25" priority="-1" />
  <xsl:template match="@*|node()" mode="M25" priority="-2">
    <xsl:apply-templates mode="M25" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MATFR1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" mode="M26" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:cloud)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:cloud)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MATFR1: clouds should be absent when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M26" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M26" priority="-1" />
  <xsl:template match="@*|node()" mode="M26" priority="-2">
    <xsl:apply-templates mode="M26" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MATFR2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" mode="M27" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:forecastWeather)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:forecastWeather)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MATFR2: forecastWeather should be absent when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M27" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M27" priority="-1" />
  <xsl:template match="@*|node()" mode="M27" priority="-2">
    <xsl:apply-templates mode="M27" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MATFR4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" mode="M28" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if( @cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:prevailingVisibility) and empty(iwxxm:prevailingVisibilityOperator)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if( @cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:prevailingVisibility) and empty(iwxxm:prevailingVisibilityOperator)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MATFR4: prevailingVisibility and prevailingVisibilityOperator should be absent when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M28" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M28" priority="-1" />
  <xsl:template match="@*|node()" mode="M28" priority="-2">
    <xsl:apply-templates mode="M28" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MATFR3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" mode="M29" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeTrendForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:prevailingVisibility) and (not(exists(iwxxm:prevailingVisibility/@xsi:nil)) or iwxxm:prevailingVisibility/@xsi:nil != 'true')) then (iwxxm:prevailingVisibility/@uom = 'm') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:prevailingVisibility) and (not(exists(iwxxm:prevailingVisibility/@xsi:nil)) or iwxxm:prevailingVisibility/@xsi:nil != 'true')) then (iwxxm:prevailingVisibility/@uom = 'm') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MATFR3: prevailingVisibility shall be reported in metres (m).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M29" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M29" priority="-1" />
  <xsl:template match="@*|node()" mode="M29" priority="-2">
    <xsl:apply-templates mode="M29" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec6-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M30" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:airTemperature) and (not(exists(iwxxm:airTemperature/@xsi:nil)) or iwxxm:airTemperature/@xsi:nil != 'true')) then (iwxxm:airTemperature/@uom = 'Cel') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:airTemperature) and (not(exists(iwxxm:airTemperature/@xsi:nil)) or iwxxm:airTemperature/@xsi:nil != 'true')) then (iwxxm:airTemperature/@uom = 'Cel') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec6: airTemperature shall be reported in degrees Celsius (Cel).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M30" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M30" priority="-1" />
  <xsl:template match="@*|node()" mode="M30" priority="-2">
    <xsl:apply-templates mode="M30" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M31" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:cloud)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@cloudAndVisibilityOK eq 'true' ) then (empty(iwxxm:cloud)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec4: clouds should be absent when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M31" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M31" priority="-1" />
  <xsl:template match="@*|node()" mode="M31" priority="-2">
    <xsl:apply-templates mode="M31" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M32" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@cloudAndVisibilityOK='true') then empty(iwxxm:presentWeather) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@cloudAndVisibilityOK='true') then empty(iwxxm:presentWeather) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec3: presentWeather should not be reported when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M32" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M32" priority="-1" />
  <xsl:template match="@*|node()" mode="M32" priority="-2">
    <xsl:apply-templates mode="M32" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M33" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@cloudAndVisibilityOK='true') then empty(iwxxm:rvr) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@cloudAndVisibilityOK='true') then empty(iwxxm:rvr) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec2: rvr should not be reported when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M33" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M33" priority="-1" />
  <xsl:template match="@*|node()" mode="M33" priority="-2">
    <xsl:apply-templates mode="M33" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M34" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@cloudAndVisibilityOK='true') then empty(iwxxm:visibility) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@cloudAndVisibilityOK='true') then empty(iwxxm:visibility) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec1: visibility should not be reported when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M34" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M34" priority="-1" />
  <xsl:template match="@*|node()" mode="M34" priority="-2">
    <xsl:apply-templates mode="M34" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec7-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M35" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:dewpointTemperature) and (not(exists(iwxxm:dewpointTemperature/@xsi:nil)) or iwxxm:dewpointTemperature/@xsi:nil != 'true')) then (iwxxm:dewpointTemperature/@uom = 'Cel') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:dewpointTemperature) and (not(exists(iwxxm:dewpointTemperature/@xsi:nil)) or iwxxm:dewpointTemperature/@xsi:nil != 'true')) then (iwxxm:dewpointTemperature/@uom = 'Cel') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec7: dewpointTemperature shall be reported in degrees Celsius (Cel).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M35" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M35" priority="-1" />
  <xsl:template match="@*|node()" mode="M35" priority="-2">
    <xsl:apply-templates mode="M35" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec8-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M36" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:qnh) and (not(exists(iwxxm:qnh/@xsi:nil)) or iwxxm:qnh/@xsi:nil != 'true')) then (iwxxm:qnh/@uom = 'hPa') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:qnh) and (not(exists(iwxxm:qnh/@xsi:nil)) or iwxxm:qnh/@xsi:nil != 'true')) then (iwxxm:qnh/@uom = 'hPa') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec8: qnh shall be reported in hectopascals (hPa).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M36" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M36" priority="-1" />
  <xsl:template match="@*|node()" mode="M36" priority="-2">
    <xsl:apply-templates mode="M36" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.MAORec5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeObservationRecord" mode="M37" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeObservationRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if((exists(iwxxm:visibility)) and (iwxxm:visibility//iwxxm:prevailingVisibility/number(text()) lt 1500) and (iwxxm:visibility//iwxxm:prevailingVisibility/@uom eq 'm')) then (exists(iwxxm:rvr)) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if((exists(iwxxm:visibility)) and (iwxxm:visibility//iwxxm:prevailingVisibility/number(text()) lt 1500) and (iwxxm:visibility//iwxxm:prevailingVisibility/@uom eq 'm')) then (exists(iwxxm:rvr)) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.MAORec5: Table A3-2 Note 7 states: "To be included if visibility or RVR &amp;lt; 1500 m; for up to a maximum of four runways". This is interpreted to mean that if the prevailing visibility is below 1500 meters, RVR should always be included</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M37" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M37" priority="-1" />
  <xsl:template match="@*|node()" mode="M37" priority="-2">
    <xsl:apply-templates mode="M37" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.AOC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeObservedClouds" mode="M38" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeObservedClouds" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( exists(iwxxm:verticalVisibility) ) then empty(iwxxm:layer) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( exists(iwxxm:verticalVisibility) ) then empty(iwxxm:layer) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.AOC1: Vertical visibility cannot be reported with cloud layers</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M38" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M38" priority="-1" />
  <xsl:template match="@*|node()" mode="M38" priority="-2">
    <xsl:apply-templates mode="M38" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.AOC2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeObservedClouds" mode="M39" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeObservedClouds" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:verticalVisibility) and (not(exists(iwxxm:verticalVisibility/@xsi:nil)) or iwxxm:verticalVisibility/@xsi:nil != 'true')) then ((iwxxm:verticalVisibility/@uom = 'm') or (iwxxm:verticalVisibility/@uom = '[ft_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:verticalVisibility) and (not(exists(iwxxm:verticalVisibility/@xsi:nil)) or iwxxm:verticalVisibility/@xsi:nil != 'true')) then ((iwxxm:verticalVisibility/@uom = 'm') or (iwxxm:verticalVisibility/@uom = '[ft_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.AOC2: verticalVisibility shall be reported in metres (m) or feet ([ft_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M39" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M39" priority="-1" />
  <xsl:template match="@*|node()" mode="M39" priority="-2">
    <xsl:apply-templates mode="M39" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASW3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWind" mode="M40" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWind" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:extremeClockwiseWindDirection) and (not(exists(iwxxm:extremeClockwiseWindDirection/@xsi:nil)) or iwxxm:extremeClockwiseWindDirection/@xsi:nil != 'true')) then (iwxxm:extremeClockwiseWindDirection/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:extremeClockwiseWindDirection) and (not(exists(iwxxm:extremeClockwiseWindDirection/@xsi:nil)) or iwxxm:extremeClockwiseWindDirection/@xsi:nil != 'true')) then (iwxxm:extremeClockwiseWindDirection/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASW3: extremeClockwiseWindDirection shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M40" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M40" priority="-1" />
  <xsl:template match="@*|node()" mode="M40" priority="-2">
    <xsl:apply-templates mode="M40" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASW4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWind" mode="M41" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWind" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:extremeCounterClockwiseWindDirection) and (not(exists(iwxxm:extremeCounterClockwiseWindDirection/@xsi:nil)) or iwxxm:extremeCounterClockwiseWindDirection/@xsi:nil != 'true')) then (iwxxm:extremeCounterClockwiseWindDirection/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:extremeCounterClockwiseWindDirection) and (not(exists(iwxxm:extremeCounterClockwiseWindDirection/@xsi:nil)) or iwxxm:extremeCounterClockwiseWindDirection/@xsi:nil != 'true')) then (iwxxm:extremeCounterClockwiseWindDirection/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASW4: extremeCounterClockwiseWindDirection shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M41" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M41" priority="-1" />
  <xsl:template match="@*|node()" mode="M41" priority="-2">
    <xsl:apply-templates mode="M41" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASW5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWind" mode="M42" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWind" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:meanWindDirection) and (not(exists(iwxxm:meanWindDirection/@xsi:nil)) or iwxxm:meanWindDirection/@xsi:nil != 'true')) then (iwxxm:meanWindDirection/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:meanWindDirection) and (not(exists(iwxxm:meanWindDirection/@xsi:nil)) or iwxxm:meanWindDirection/@xsi:nil != 'true')) then (iwxxm:meanWindDirection/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASW5: meanWindDirection shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M42" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M42" priority="-1" />
  <xsl:template match="@*|node()" mode="M42" priority="-2">
    <xsl:apply-templates mode="M42" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASW6-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWind" mode="M43" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWind" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:meanWindSpeed) and (not(exists(iwxxm:meanWindSpeed/@xsi:nil)) or iwxxm:meanWindSpeed/@xsi:nil != 'true')) then ((iwxxm:meanWindSpeed/@uom = 'm/s') or (iwxxm:meanWindSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:meanWindSpeed) and (not(exists(iwxxm:meanWindSpeed/@xsi:nil)) or iwxxm:meanWindSpeed/@xsi:nil != 'true')) then ((iwxxm:meanWindSpeed/@uom = 'm/s') or (iwxxm:meanWindSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASW6: meanWindSpeed shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M43" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M43" priority="-1" />
  <xsl:template match="@*|node()" mode="M43" priority="-2">
    <xsl:apply-templates mode="M43" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASW2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWind" mode="M44" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWind" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @variableDirection eq 'true' ) then ( empty(iwxxm:meanWindDirection) ) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @variableDirection eq 'true' ) then ( empty(iwxxm:meanWindDirection) ) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASW2: Wind direction is not reported when variable winds are indicated</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M44" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M44" priority="-1" />
  <xsl:template match="@*|node()" mode="M44" priority="-2">
    <xsl:apply-templates mode="M44" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASW7-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWind" mode="M45" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWind" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:windGustSpeed) and (not(exists(iwxxm:windGustSpeed/@xsi:nil)) or iwxxm:windGustSpeed/@xsi:nil != 'true')) then ((iwxxm:windGustSpeed/@uom = 'm/s') or (iwxxm:windGustSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:windGustSpeed) and (not(exists(iwxxm:windGustSpeed/@xsi:nil)) or iwxxm:windGustSpeed/@xsi:nil != 'true')) then ((iwxxm:windGustSpeed/@uom = 'm/s') or (iwxxm:windGustSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASW7: windGustSpeed shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M45" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M45" priority="-1" />
  <xsl:template match="@*|node()" mode="M45" priority="-2">
    <xsl:apply-templates mode="M45" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.ASW1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWind" mode="M46" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWind" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if( exists(iwxxm:meanWindDirection)and exists(iwxxm:extremeClockwiseWindDirection)and exists(iwxxm:extremeCounterClockwiseWindDirection) ) then ((iwxxm:meanWindDirection/@uom = iwxxm:extremeClockwiseWindDirection/@uom) and (iwxxm:meanWindDirection/@uom = iwxxm:extremeCounterClockwiseWindDirection/@uom)) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if( exists(iwxxm:meanWindDirection)and exists(iwxxm:extremeClockwiseWindDirection)and exists(iwxxm:extremeCounterClockwiseWindDirection) ) then ((iwxxm:meanWindDirection/@uom = iwxxm:extremeClockwiseWindDirection/@uom) and (iwxxm:meanWindDirection/@uom = iwxxm:extremeCounterClockwiseWindDirection/@uom)) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.ASW1: All wind UOMs must be the same</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M46" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M46" priority="-1" />
  <xsl:template match="@*|node()" mode="M46" priority="-2">
    <xsl:apply-templates mode="M46" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.AHV1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeHorizontalVisibility" mode="M47" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeHorizontalVisibility" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:minimumVisibility) and (not(exists(iwxxm:minimumVisibility/@xsi:nil)) or iwxxm:minimumVisibility/@xsi:nil != 'true')) then (iwxxm:minimumVisibility/@uom = 'm') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:minimumVisibility) and (not(exists(iwxxm:minimumVisibility/@xsi:nil)) or iwxxm:minimumVisibility/@xsi:nil != 'true')) then (iwxxm:minimumVisibility/@uom = 'm') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.AHV1: minimumVisibility shall be reported in metres (m).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M47" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M47" priority="-1" />
  <xsl:template match="@*|node()" mode="M47" priority="-2">
    <xsl:apply-templates mode="M47" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.AHV2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeHorizontalVisibility" mode="M48" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeHorizontalVisibility" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:minimumVisibilityDirection) and (not(exists(iwxxm:minimumVisibilityDirection/@xsi:nil)) or iwxxm:minimumVisibilityDirection/@xsi:nil != 'true')) then (iwxxm:minimumVisibilityDirection/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:minimumVisibilityDirection) and (not(exists(iwxxm:minimumVisibilityDirection/@xsi:nil)) or iwxxm:minimumVisibilityDirection/@xsi:nil != 'true')) then (iwxxm:minimumVisibilityDirection/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.AHV2: minimumVisibilityDirection shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M48" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M48" priority="-1" />
  <xsl:template match="@*|node()" mode="M48" priority="-2">
    <xsl:apply-templates mode="M48" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN METAR_SPECI.AHV3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeHorizontalVisibility" mode="M49" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeHorizontalVisibility" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:prevailingVisibility) and (not(exists(iwxxm:prevailingVisibility/@xsi:nil)) or iwxxm:prevailingVisibility/@xsi:nil != 'true')) then (iwxxm:prevailingVisibility/@uom = 'm') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:prevailingVisibility) and (not(exists(iwxxm:prevailingVisibility/@xsi:nil)) or iwxxm:prevailingVisibility/@xsi:nil != 'true')) then (iwxxm:prevailingVisibility/@uom = 'm') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>METAR_SPECI.AHV3: prevailingVisibility shall be reported in metres (m).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M49" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M49" priority="-1" />
  <xsl:template match="@*|node()" mode="M49" priority="-2">
    <xsl:apply-templates mode="M49" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.MAFR2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeForecastRecord" mode="M50" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@cloudAndVisibilityOK = 'true') then empty(iwxxm:cloud) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@cloudAndVisibilityOK = 'true') then empty(iwxxm:cloud) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.MAFR2: cloud should not be reported when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M50" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M50" priority="-1" />
  <xsl:template match="@*|node()" mode="M50" priority="-2">
    <xsl:apply-templates mode="M50" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.MAFR1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeForecastRecord" mode="M51" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@cloudAndVisibilityOK = 'true') then empty(iwxxm:prevailingVisibility) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@cloudAndVisibilityOK = 'true') then empty(iwxxm:prevailingVisibility) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.MAFR1: prevailingVisibility should not be reported when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M51" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M51" priority="-1" />
  <xsl:template match="@*|node()" mode="M51" priority="-2">
    <xsl:apply-templates mode="M51" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.MAFR3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeForecastRecord" mode="M52" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@cloudAndVisibilityOK = 'true') then empty(iwxxm:weather) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@cloudAndVisibilityOK = 'true') then empty(iwxxm:weather) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.MAFR3: weather should not be reported when cloudAndVisibilityOK is true</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M52" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M52" priority="-1" />
  <xsl:template match="@*|node()" mode="M52" priority="-2">
    <xsl:apply-templates mode="M52" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.MAFR4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:MeteorologicalAerodromeForecastRecord" mode="M53" priority="1000">
    <svrl:fired-rule context="//iwxxm:MeteorologicalAerodromeForecastRecord" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:prevailingVisibility) and (not(exists(iwxxm:prevailingVisibility/@xsi:nil)) or iwxxm:prevailingVisibility/@xsi:nil != 'true')) then (iwxxm:prevailingVisibility/@uom = 'm') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:prevailingVisibility) and (not(exists(iwxxm:prevailingVisibility/@xsi:nil)) or iwxxm:prevailingVisibility/@xsi:nil != 'true')) then (iwxxm:prevailingVisibility/@uom = 'm') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.MAFR4: prevailingVisibility shall be reported in metres (m).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M53" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M53" priority="-1" />
  <xsl:template match="@*|node()" mode="M53" priority="-2">
    <xsl:apply-templates mode="M53" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.AATF1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeAirTemperatureForecast" mode="M54" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeAirTemperatureForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:maximumTemperature) and (not(exists(iwxxm:maximumTemperature/@xsi:nil)) or iwxxm:maximumTemperature/@xsi:nil != 'true')) then (iwxxm:maximumTemperature/@uom = 'Cel') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:maximumTemperature) and (not(exists(iwxxm:maximumTemperature/@xsi:nil)) or iwxxm:maximumTemperature/@xsi:nil != 'true')) then (iwxxm:maximumTemperature/@uom = 'Cel') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.AATF1: maximumTemperature shall be reported in degrees Celsius (Cel).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M54" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M54" priority="-1" />
  <xsl:template match="@*|node()" mode="M54" priority="-2">
    <xsl:apply-templates mode="M54" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.AATF2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeAirTemperatureForecast" mode="M55" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeAirTemperatureForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:minimumTemperature) and (not(exists(iwxxm:minimumTemperature/@xsi:nil)) or iwxxm:minimumTemperature/@xsi:nil != 'true')) then (iwxxm:minimumTemperature/@uom = 'Cel') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:minimumTemperature) and (not(exists(iwxxm:minimumTemperature/@xsi:nil)) or iwxxm:minimumTemperature/@xsi:nil != 'true')) then (iwxxm:minimumTemperature/@uom = 'Cel') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.AATF2: minimumTemperature shall be reported in degrees Celsius (Cel).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M55" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M55" priority="-1" />
  <xsl:template match="@*|node()" mode="M55" priority="-2">
    <xsl:apply-templates mode="M55" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M56" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(//iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator) then(empty(iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature)) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(//iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator) then(empty(iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature)) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF1: Forecast conditions cannot include temperature information. They are otherwise identical to the prevailing conditions</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M56" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M56" priority="-1" />
  <xsl:template match="@*|node()" mode="M56" priority="-2">
    <xsl:apply-templates mode="M56" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF18-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M57" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:baseForecast//om:result/iwxxm:MeteorologicalAerodromeForecastRecord)) then((exists(iwxxm:baseForecast//om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind)) and (exists(iwxxm:baseForecast//om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:baseForecast//om:result/iwxxm:MeteorologicalAerodromeForecastRecord)) then((exists(iwxxm:baseForecast//om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind)) and (exists(iwxxm:baseForecast//om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF18: surfaceWind and cloud are mandatory in a non-empty baseForecast</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M57" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M57" priority="-1" />
  <xsl:template match="@*|node()" mode="M57" priority="-2">
    <xsl:apply-templates mode="M57" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M58" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @status = 'AMENDMENT' ) then (exists(iwxxm:previousReportValidPeriod)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @status = 'AMENDMENT' ) then (exists(iwxxm:previousReportValidPeriod)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF3: An amended report must also include the valid time of the amended report</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M58" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M58" priority="-1" />
  <xsl:template match="@*|node()" mode="M58" priority="-2">
    <xsl:apply-templates mode="M58" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M59" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @status = 'CANCELLATION' ) then (exists(iwxxm:previousReportValidPeriod)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @status = 'CANCELLATION' ) then (exists(iwxxm:previousReportValidPeriod)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF4: A cancelled report must also include the valid time of the cancelled report</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M59" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M59" priority="-1" />
  <xsl:template match="@*|node()" mode="M59" priority="-2">
    <xsl:apply-templates mode="M59" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M60" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @status = 'CORRECTION' ) then (exists(iwxxm:previousReportValidPeriod)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @status = 'CORRECTION' ) then (exists(iwxxm:previousReportValidPeriod)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF5: A corrected report must reference</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M60" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M60" priority="-1" />
  <xsl:template match="@*|node()" mode="M60" priority="-2">
    <xsl:apply-templates mode="M60" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF9-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M61" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if( @status = 'MISSING' ) then( (exists(iwxxm:baseForecast//om:result/@nilReason)) and ((empty(iwxxm:validTime)) and ((empty(iwxxm:previousReportValidPeriod)) and (empty(iwxxm:changeForecast))))) else( true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if( @status = 'MISSING' ) then( (exists(iwxxm:baseForecast//om:result/@nilReason)) and ((empty(iwxxm:validTime)) and ((empty(iwxxm:previousReportValidPeriod)) and (empty(iwxxm:changeForecast))))) else( true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF9: Missing TAF reports only include aerodrome information and issue time information</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M61" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M61" priority="-1" />
  <xsl:template match="@*|node()" mode="M61" priority="-2">
    <xsl:apply-templates mode="M61" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M62" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @status = 'NORMAL' ) then (empty(iwxxm:previousReportValidPeriod)) else (true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @status = 'NORMAL' ) then (empty(iwxxm:previousReportValidPeriod)) else (true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF2: previousReportValidPeriod must be null unless this cancels, corrects or amends a previous report</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M62" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M62" priority="-1" />
  <xsl:template match="@*|node()" mode="M62" priority="-2">
    <xsl:apply-templates mode="M62" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF11-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M63" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @status ne 'MISSING') then(exists(iwxxm:validTime)) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @status ne 'MISSING') then(exists(iwxxm:validTime)) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF11: Non-missing TAF reports must contains validTime</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M63" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M63" priority="-1" />
  <xsl:template match="@*|node()" mode="M63" priority="-2">
    <xsl:apply-templates mode="M63" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF8-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M64" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(empty(iwxxm:baseForecast//iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator) )" />
      <xsl:otherwise>
        <svrl:failed-assert test="(empty(iwxxm:baseForecast//iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator) )">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF8: Base conditions may not have a change indicator</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M64" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M64" priority="-1" />
  <xsl:template match="@*|node()" mode="M64" priority="-2">
    <xsl:apply-templates mode="M64" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF14-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M65" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="( if(exists(.//iwxxm:baseForecast/om:OM_Observation)) then ( ( (exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:AirportHeliport)) or (contains(string(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'aerodrome')) ) and ( if(exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/@xlink:href)) then (not(exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest[@xlink:href != concat( '#', current()//iwxxm:baseForecast/om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id )]))) else(true()) ) ) else(true()) )" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="( if(exists(.//iwxxm:baseForecast/om:OM_Observation)) then ( ( (exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:AirportHeliport)) or (contains(string(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'aerodrome')) ) and ( if(exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/@xlink:href)) then (not(exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest[@xlink:href != concat( '#', current()//iwxxm:baseForecast/om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id )]))) else(true()) ) ) else(true()) )">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF14: The sampled feature of baseForecast is always an aerodrome</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M65" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M65" priority="-1" />
  <xsl:template match="@*|node()" mode="M65" priority="-2">
    <xsl:apply-templates mode="M65" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF16-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M66" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:baseForecast)) then(not(exists(iwxxm:baseForecast//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:baseForecast)) then(not(exists(iwxxm:baseForecast//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF16: The procedure of a TAF baseForecast should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M66" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M66" priority="-1" />
  <xsl:template match="@*|node()" mode="M66" priority="-2">
    <xsl:apply-templates mode="M66" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF12-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M67" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if((exists(.//iwxxm:baseForecast/om:OM_Observation)) and (empty(.//iwxxm:baseForecast/om:OM_Observation/om:result/@nilReason))) then((exists(.//iwxxm:baseForecast/om:OM_Observation/om:validTime/gml:TimePeriod))or(concat( '#', current()//iwxxm:validTime/gml:TimePeriod/@gml:id ) = .//iwxxm:baseForecast/om:OM_Observation/om:validTime/@xlink:href)) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if((exists(.//iwxxm:baseForecast/om:OM_Observation)) and (empty(.//iwxxm:baseForecast/om:OM_Observation/om:result/@nilReason))) then((exists(.//iwxxm:baseForecast/om:OM_Observation/om:validTime/gml:TimePeriod))or(concat( '#', current()//iwxxm:validTime/gml:TimePeriod/@gml:id ) = .//iwxxm:baseForecast/om:OM_Observation/om:validTime/@xlink:href)) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF12: The O&amp;amp;M validTime of baseForecast must be a time period for TAF forecasts</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M67" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M67" priority="-1" />
  <xsl:template match="@*|node()" mode="M67" priority="-2">
    <xsl:apply-templates mode="M67" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF15-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M68" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="( if(exists(.//iwxxm:changeForecast/om:OM_Observation)) then ( ( (exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:AirportHeliport)) or (contains(string(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'aerodrome')) ) and ( if(exists(.//iwxxm:changeForecast/om:OM_Observation/om:featureOfInterest/@xlink:href)) then (not(exists(.//iwxxm:changeForecast/om:OM_Observation/om:featureOfInterest[@xlink:href != concat( '#', current()//iwxxm:baseForecast/om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id )]))) else(true()) ) ) else(true()) )" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="( if(exists(.//iwxxm:changeForecast/om:OM_Observation)) then ( ( (exists(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:AirportHeliport)) or (contains(string(.//iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'aerodrome')) ) and ( if(exists(.//iwxxm:changeForecast/om:OM_Observation/om:featureOfInterest/@xlink:href)) then (not(exists(.//iwxxm:changeForecast/om:OM_Observation/om:featureOfInterest[@xlink:href != concat( '#', current()//iwxxm:baseForecast/om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id )]))) else(true()) ) ) else(true()) )">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF15: The sampled feature of changeForecast is always an aerodrome</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M68" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M68" priority="-1" />
  <xsl:template match="@*|node()" mode="M68" priority="-2">
    <xsl:apply-templates mode="M68" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF17-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M69" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:changeForecast)) then(not(exists(iwxxm:changeForecast//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:changeForecast)) then(not(exists(iwxxm:changeForecast//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF17: The procedure of a TAF changeForecast should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M69" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M69" priority="-1" />
  <xsl:template match="@*|node()" mode="M69" priority="-2">
    <xsl:apply-templates mode="M69" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF13-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M70" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if((exists(.//iwxxm:changeForecast/om:OM_Observation)) and (empty(.//iwxxm:changeForecast/om:OM_Observation/om:result/@nilReason))) then((exists(.//iwxxm:changeForecast/om:OM_Observation/om:validTime/gml:TimePeriod))or(concat( '#', current()//iwxxm:validTime/gml:TimePeriod/@gml:id ) = .//iwxxm:changeForecast/om:OM_Observation/om:validTime/@xlink:href)) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if((exists(.//iwxxm:changeForecast/om:OM_Observation)) and (empty(.//iwxxm:changeForecast/om:OM_Observation/om:result/@nilReason))) then((exists(.//iwxxm:changeForecast/om:OM_Observation/om:validTime/gml:TimePeriod))or(concat( '#', current()//iwxxm:validTime/gml:TimePeriod/@gml:id ) = .//iwxxm:changeForecast/om:OM_Observation/om:validTime/@xlink:href)) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF13: The O&amp;amp;M validTime of changeForecast must be a time period for TAF forecasts</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M70" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M70" priority="-1" />
  <xsl:template match="@*|node()" mode="M70" priority="-2">
    <xsl:apply-templates mode="M70" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TAF.TAF6-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TAF" mode="M71" priority="1000">
    <svrl:fired-rule context="//iwxxm:TAF" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(empty(distinct-values(for $change-forecast in iwxxm:changeForecast return($change-forecast/om:OM_Observation/om:resultTime//gml:timePosition/text()=iwxxm:baseForecast/om:OM_Observation/om:resultTime//gml:timePosition/text())or($change-forecast/om:OM_Observation/om:resultTime/@xlink:href=iwxxm:baseForecast/om:OM_Observation/om:resultTime/@xlink:href))[.=false()]))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(empty(distinct-values(for $change-forecast in iwxxm:changeForecast return($change-forecast/om:OM_Observation/om:resultTime//gml:timePosition/text()=iwxxm:baseForecast/om:OM_Observation/om:resultTime//gml:timePosition/text())or($change-forecast/om:OM_Observation/om:resultTime/@xlink:href=iwxxm:baseForecast/om:OM_Observation/om:resultTime/@xlink:href))[.=false()]))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TAF.TAF6: resultTime for the baseForecast and the changeForecasts must match</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M71" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M71" priority="-1" />
  <xsl:template match="@*|node()" mode="M71" priority="-2">
    <xsl:apply-templates mode="M71" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET9-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M72" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:EvolvingMeteorologicalCondition/iwxxm:speedOfMotion)) and not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:EvolvingMeteorologicalCondition/iwxxm:directionOfMotion))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:EvolvingMeteorologicalCondition/iwxxm:speedOfMotion)) and not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:EvolvingMeteorologicalCondition/iwxxm:directionOfMotion))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET9: SIGMET can not have both a forecastPositionAnalysis and expected speed and/or direction of motion</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M72" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M72" priority="-1" />
  <xsl:template match="@*|node()" mode="M72" priority="-2">
    <xsl:apply-templates mode="M72" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M73" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@status = 'CANCELLATION') then exists(iwxxm:analysis//om:result/@nilReason) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@status = 'CANCELLATION') then exists(iwxxm:analysis//om:result/@nilReason) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET1: A cancelled SIGMET should only include identifying information (time and airspace) and no other information</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M73" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M73" priority="-1" />
  <xsl:template match="@*|node()" mode="M73" priority="-2">
    <xsl:apply-templates mode="M73" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M74" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@status = 'NORMAL') then ((exists(iwxxm:analysis)) and (empty(iwxxm:analysis//om:result/@nilReason))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@status = 'NORMAL') then ((exists(iwxxm:analysis)) and (empty(iwxxm:analysis//om:result/@nilReason))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET2: There must be at least one analysis when a SIGMET does not have canceled status</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M74" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M74" priority="-1" />
  <xsl:template match="@*|node()" mode="M74" priority="-2">
    <xsl:apply-templates mode="M74" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET10-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M75" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(iwxxm:volcanicAshMovedToFIR)) then(@status = 'CANCELLATION') else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(exists(iwxxm:volcanicAshMovedToFIR)) then(@status = 'CANCELLATION') else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET10: SIGMET must have a cancelled status if reporting volcanicAshMovedToFIR</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M75" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M75" priority="-1" />
  <xsl:template match="@*|node()" mode="M75" priority="-2">
    <xsl:apply-templates mode="M75" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M76" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="( if((@status ne 'CANCELLATION') and (not(@translationFailedTAC))) then((exists(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:Airspace)) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'fir')) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'uir')) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'cta')) ) and ( if(exists(.//om:OM_Observation/om:featureOfInterest/@xlink:href)) then (concat( '#', current()//om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id ) = .//om:OM_Observation/om:featureOfInterest/@xlink:href) else(true())) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="( if((@status ne 'CANCELLATION') and (not(@translationFailedTAC))) then((exists(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/aixm:Airspace)) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'fir')) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'uir')) or (contains(string(.//om:OM_Observation/om:featureOfInterest//sf:sampledFeature/@xlink:href), 'cta')) ) and ( if(exists(.//om:OM_Observation/om:featureOfInterest/@xlink:href)) then (concat( '#', current()//om:OM_Observation//sams:SF_SpatialSamplingFeature/@gml:id ) = .//om:OM_Observation/om:featureOfInterest/@xlink:href) else(true())) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET4: Sampled feature in analysis and forecastPositionAnalysis must be an FIR, UIR, or CTA</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M76" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M76" priority="-1" />
  <xsl:template match="@*|node()" mode="M76" priority="-2">
    <xsl:apply-templates mode="M76" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET7-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M77" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET7: The procedure of a SIGMET analysis should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M77" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M77" priority="-1" />
  <xsl:template match="@*|node()" mode="M77" priority="-2">
    <xsl:apply-templates mode="M77" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M78" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if((@status ne 'CANCELLATION') and exists(//iwxxm:analysis/om:OM_Observation)) then(exists(//iwxxm:analysis/om:OM_Observation/om:result/iwxxm:SIGMETEvolvingConditionCollection)) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if((@status ne 'CANCELLATION') and exists(//iwxxm:analysis/om:OM_Observation)) then(exists(//iwxxm:analysis/om:OM_Observation/om:result/iwxxm:SIGMETEvolvingConditionCollection)) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET3: OBS and FCST analyses must have a result type of SIGMETEvolvingConditionCollection</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M78" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M78" priority="-1" />
  <xsl:template match="@*|node()" mode="M78" priority="-2">
    <xsl:apply-templates mode="M78" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET8-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M79" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:forecastPositionAnalysis//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:forecastPositionAnalysis//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET8: The procedure of a SIGMET forecastPositionAnalysis should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M79" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M79" priority="-1" />
  <xsl:template match="@*|node()" mode="M79" priority="-2">
    <xsl:apply-templates mode="M79" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SIGMET5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" mode="M80" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if((@status ne 'CANCELLATION') and exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:forecastPositionAnalysis//om:result/*[name() != 'iwxxm:SIGMETPositionCollection']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if((@status ne 'CANCELLATION') and exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:forecastPositionAnalysis//om:result/*[name() != 'iwxxm:SIGMETPositionCollection']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SIGMET5: The result of a forecastPositionAnalysis should be a SIGMETPositionCollection</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M80" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M80" priority="-1" />
  <xsl:template match="@*|node()" mode="M80" priority="-2">
    <xsl:apply-templates mode="M80" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SEC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETEvolvingCondition" mode="M81" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:directionOfMotion) and (not(exists(iwxxm:directionOfMotion/@xsi:nil)) or iwxxm:directionOfMotion/@xsi:nil != 'true')) then (iwxxm:directionOfMotion/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:directionOfMotion) and (not(exists(iwxxm:directionOfMotion/@xsi:nil)) or iwxxm:directionOfMotion/@xsi:nil != 'true')) then (iwxxm:directionOfMotion/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SEC1: directionOfMotion shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M81" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M81" priority="-1" />
  <xsl:template match="@*|node()" mode="M81" priority="-2">
    <xsl:apply-templates mode="M81" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SEC3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETEvolvingCondition" mode="M82" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(iwxxm:geometryLowerLimitOperator)) then (iwxxm:geometryLowerLimitOperator = 'BELOW') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(exists(iwxxm:geometryLowerLimitOperator)) then (iwxxm:geometryLowerLimitOperator = 'BELOW') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SEC3: geometryLowerLimitOperator can either be NULL or BELOW.</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M82" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M82" priority="-1" />
  <xsl:template match="@*|node()" mode="M82" priority="-2">
    <xsl:apply-templates mode="M82" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SEC4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETEvolvingCondition" mode="M83" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(iwxxm:geometryUpperLimitOperator)) then (iwxxm:geometryUpperLimitOperator = 'ABOVE') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(exists(iwxxm:geometryUpperLimitOperator)) then (iwxxm:geometryUpperLimitOperator = 'ABOVE') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SEC4: geometryUpperLimitOperator can either be NULL or ABOVE</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M83" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M83" priority="-1" />
  <xsl:template match="@*|node()" mode="M83" priority="-2">
    <xsl:apply-templates mode="M83" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SEC2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETEvolvingCondition" mode="M84" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:speedOfMotion) and (not(exists(iwxxm:speedOfMotion/@xsi:nil)) or iwxxm:speedOfMotion/@xsi:nil != 'true')) then ((iwxxm:speedOfMotion/@uom = 'km/h') or (iwxxm:speedOfMotion/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:speedOfMotion) and (not(exists(iwxxm:speedOfMotion/@xsi:nil)) or iwxxm:speedOfMotion/@xsi:nil != 'true')) then ((iwxxm:speedOfMotion/@uom = 'km/h') or (iwxxm:speedOfMotion/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SEC2: speedOfMotion shall be reported in kilometres per hour (km/h) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M84" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M84" priority="-1" />
  <xsl:template match="@*|node()" mode="M84" priority="-2">
    <xsl:apply-templates mode="M84" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SECC3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETEvolvingConditionCollection" mode="M85" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETEvolvingConditionCollection" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(/iwxxm:SIGMET)) then(count(iwxxm:member) eq 1) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(exists(/iwxxm:SIGMET)) then(count(iwxxm:member) eq 1) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SECC3: The number of SIGMETEvolvingConditionCollection member should be 1 for non-Tropical Cyclone/Volcanic Ash SIGMETs</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M85" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M85" priority="-1" />
  <xsl:template match="@*|node()" mode="M85" priority="-2">
    <xsl:apply-templates mode="M85" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SECC2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETEvolvingConditionCollection" mode="M86" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETEvolvingConditionCollection" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(@timeIndicator='FORECAST' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') ge translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(@timeIndicator='FORECAST' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') ge translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SECC2: When SIGMETEvolvingConditionCollection timeIndicator is a forecast, the phenomenonTime must be later than or equal to the beginning of the validPeriod of the report.</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M86" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M86" priority="-1" />
  <xsl:template match="@*|node()" mode="M86" priority="-2">
    <xsl:apply-templates mode="M86" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SECC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETEvolvingConditionCollection" mode="M87" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETEvolvingConditionCollection" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(@timeIndicator='OBSERVATION' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') le translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(@timeIndicator='OBSERVATION' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') le translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SECC1: When SIGMETEvolvingConditionCollection timeIndicator is an observation, the phenomenonTime must be earlier than or equal to the beginning of the validPeriod of the report.</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M87" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M87" priority="-1" />
  <xsl:template match="@*|node()" mode="M87" priority="-2">
    <xsl:apply-templates mode="M87" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN SIGMET.SPC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:SIGMETPositionCollection" mode="M88" priority="1000">
    <svrl:fired-rule context="//iwxxm:SIGMETPositionCollection" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(/iwxxm:SIGMET)) then(count(iwxxm:member) eq 1) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(exists(/iwxxm:SIGMET)) then(count(iwxxm:member) eq 1) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>SIGMET.SPC1: The number of SIGMETPositionCollection member should be 1 for non-Tropical Cyclone/Volcanic Ash SIGMETs</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M88" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M88" priority="-1" />
  <xsl:template match="@*|node()" mode="M88" priority="-2">
    <xsl:apply-templates mode="M88" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AECC2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingConditionCollection" mode="M89" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingConditionCollection" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(@timeIndicator='FORECAST' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') ge translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(@timeIndicator='FORECAST' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') ge translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AECC2: When AIRMETEvolvingConditionCollection timeIndicator is a forecast, the phenomenonTime must be later than or equal to the beginning of the validPeriod of the report.</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M89" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M89" priority="-1" />
  <xsl:template match="@*|node()" mode="M89" priority="-2">
    <xsl:apply-templates mode="M89" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AECC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingConditionCollection" mode="M90" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingConditionCollection" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(@timeIndicator='OBSERVATION' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') le translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(@timeIndicator='OBSERVATION' and ../../om:phenomenonTime/gml:TimeInstant/gml:timePosition) then (translate(../../om:phenomenonTime/gml:TimeInstant/gml:timePosition,'-T:Z','') le translate(../../../../iwxxm:validPeriod/gml:TimePeriod/gml:beginPosition,'-T:Z','')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AECC1: When AIRMETEvolvingConditionCollection timeIndicator is an observation, the phenomenonTime must be earlier than or equal to the beginning of the validPeriod of the report.</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M90" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M90" priority="-1" />
  <xsl:template match="@*|node()" mode="M90" priority="-2">
    <xsl:apply-templates mode="M90" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M91" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:cloudBase) and (not(exists(iwxxm:cloudBase/@xsi:nil)) or iwxxm:cloudBase/@xsi:nil != 'true')) then ((iwxxm:cloudBase/@uom = 'm') or (iwxxm:cloudBase/@uom = '[ft_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:cloudBase) and (not(exists(iwxxm:cloudBase/@xsi:nil)) or iwxxm:cloudBase/@xsi:nil != 'true')) then ((iwxxm:cloudBase/@uom = 'm') or (iwxxm:cloudBase/@uom = '[ft_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC1: cloudBase shall be reported in metres (m) or feet ([ft_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M91" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M91" priority="-1" />
  <xsl:template match="@*|node()" mode="M91" priority="-2">
    <xsl:apply-templates mode="M91" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M92" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:cloudTop) and (not(exists(iwxxm:cloudTop/@xsi:nil)) or iwxxm:cloudTop/@xsi:nil != 'true')) then ((iwxxm:cloudTop/@uom = 'm') or (iwxxm:cloudTop/@uom = '[ft_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:cloudTop) and (not(exists(iwxxm:cloudTop/@xsi:nil)) or iwxxm:cloudTop/@xsi:nil != 'true')) then ((iwxxm:cloudTop/@uom = 'm') or (iwxxm:cloudTop/@uom = '[ft_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC2: cloudTop shall be reported in metres (m) or feet ([ft_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M92" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M92" priority="-1" />
  <xsl:template match="@*|node()" mode="M92" priority="-2">
    <xsl:apply-templates mode="M92" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M93" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:directionOfMotion) and (not(exists(iwxxm:directionOfMotion/@xsi:nil)) or iwxxm:directionOfMotion/@xsi:nil != 'true')) then (iwxxm:directionOfMotion/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:directionOfMotion) and (not(exists(iwxxm:directionOfMotion/@xsi:nil)) or iwxxm:directionOfMotion/@xsi:nil != 'true')) then (iwxxm:directionOfMotion/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC3: directionOfMotion shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M93" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M93" priority="-1" />
  <xsl:template match="@*|node()" mode="M93" priority="-2">
    <xsl:apply-templates mode="M93" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC9-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M94" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(iwxxm:geometryLowerLimitOperator)) then (iwxxm:geometryLowerLimitOperator = 'BELOW') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(exists(iwxxm:geometryLowerLimitOperator)) then (iwxxm:geometryLowerLimitOperator = 'BELOW') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC9: geometryLowerLimitOperator can either be NULL or BELOW.</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M94" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M94" priority="-1" />
  <xsl:template match="@*|node()" mode="M94" priority="-2">
    <xsl:apply-templates mode="M94" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC10-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M95" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(iwxxm:geometryUpperLimitOperator)) then (iwxxm:geometryUpperLimitOperator = 'ABOVE') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(exists(iwxxm:geometryUpperLimitOperator)) then (iwxxm:geometryUpperLimitOperator = 'ABOVE') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC10: geometryUpperLimitOperator can either be NULL or ABOVE</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M95" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M95" priority="-1" />
  <xsl:template match="@*|node()" mode="M95" priority="-2">
    <xsl:apply-templates mode="M95" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M96" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:speedOfMotion) and (not(exists(iwxxm:speedOfMotion/@xsi:nil)) or iwxxm:speedOfMotion/@xsi:nil != 'true')) then ((iwxxm:speedOfMotion/@uom = 'km/h') or (iwxxm:speedOfMotion/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:speedOfMotion) and (not(exists(iwxxm:speedOfMotion/@xsi:nil)) or iwxxm:speedOfMotion/@xsi:nil != 'true')) then ((iwxxm:speedOfMotion/@uom = 'km/h') or (iwxxm:speedOfMotion/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC4: speedOfMotion shall be reported in kilometres per hour (km/h) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M96" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M96" priority="-1" />
  <xsl:template match="@*|node()" mode="M96" priority="-2">
    <xsl:apply-templates mode="M96" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M97" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:surfaceVisibility) and (not(exists(iwxxm:surfaceVisibility/@xsi:nil)) or iwxxm:surfaceVisibility/@xsi:nil != 'true')) then (iwxxm:surfaceVisibility/@uom = 'm') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:surfaceVisibility) and (not(exists(iwxxm:surfaceVisibility/@xsi:nil)) or iwxxm:surfaceVisibility/@xsi:nil != 'true')) then (iwxxm:surfaceVisibility/@uom = 'm') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC5: surfaceVisibility shall be reported in metres (m).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M97" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M97" priority="-1" />
  <xsl:template match="@*|node()" mode="M97" priority="-2">
    <xsl:apply-templates mode="M97" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC7-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M98" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:surfaceWindDirection) and (not(exists(iwxxm:surfaceWindDirection/@xsi:nil)) or iwxxm:surfaceWindDirection/@xsi:nil != 'true')) then ((iwxxm:surfaceWindDirection/@uom = 'deg')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:surfaceWindDirection) and (not(exists(iwxxm:surfaceWindDirection/@xsi:nil)) or iwxxm:surfaceWindDirection/@xsi:nil != 'true')) then ((iwxxm:surfaceWindDirection/@uom = 'deg')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC7: surfaceWindDirection shall be reported in the degrees unit of measure ('deg').</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M98" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M98" priority="-1" />
  <xsl:template match="@*|node()" mode="M98" priority="-2">
    <xsl:apply-templates mode="M98" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC6-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M99" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:surfaceWindSpeed) and (not(exists(iwxxm:surfaceWindSpeed/@xsi:nil)) or iwxxm:surfaceWindSpeed/@xsi:nil != 'true')) then ((iwxxm:surfaceWindSpeed/@uom = 'm/s') or (iwxxm:surfaceWindSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:surfaceWindSpeed) and (not(exists(iwxxm:surfaceWindSpeed/@xsi:nil)) or iwxxm:surfaceWindSpeed/@xsi:nil != 'true')) then ((iwxxm:surfaceWindSpeed/@uom = 'm/s') or (iwxxm:surfaceWindSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC6: surfaceWindSpeed shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M99" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M99" priority="-1" />
  <xsl:template match="@*|node()" mode="M99" priority="-2">
    <xsl:apply-templates mode="M99" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AEC8-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMETEvolvingCondition" mode="M100" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMETEvolvingCondition" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:surfaceWindDirection) or exists(iwxxm:surfaceWindSpeed)) then (exists(iwxxm:surfaceWindDirection) and exists(iwxxm:surfaceWindSpeed)) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:surfaceWindDirection) or exists(iwxxm:surfaceWindSpeed)) then (exists(iwxxm:surfaceWindDirection) and exists(iwxxm:surfaceWindSpeed)) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AEC8: surfaceWindDirection and surfaceWindSpeed must be reported together</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M100" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M100" priority="-1" />
  <xsl:template match="@*|node()" mode="M100" priority="-2">
    <xsl:apply-templates mode="M100" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AIRMET5-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMET" mode="M101" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:AIRMETEvolvingMeteorologicalCondition/iwxxm:speedOfMotion)) and not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:AIRMETEvolvingMeteorologicalCondition/iwxxm:directionOfMotion))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:forecastPositionAnalysis)) then(not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:AIRMETEvolvingMeteorologicalCondition/iwxxm:speedOfMotion)) and not(exists(iwxxm:analysis/om:OM_Observation/om:result/iwxxm:AIRMETEvolvingMeteorologicalCondition/iwxxm:directionOfMotion))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AIRMET5: AIRMET can not have both a forecastPositionAnalysis and expected speed and/or direction of motion</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M101" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M101" priority="-1" />
  <xsl:template match="@*|node()" mode="M101" priority="-2">
    <xsl:apply-templates mode="M101" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AIRMET2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMET" mode="M102" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@status = 'CANCELLATION') then exists(iwxxm:analysis//om:result/@nilReason) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@status = 'CANCELLATION') then exists(iwxxm:analysis//om:result/@nilReason) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AIRMET2: A canceled AIRMET only include identifying information (time and airspace) and no other information</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M102" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M102" priority="-1" />
  <xsl:template match="@*|node()" mode="M102" priority="-2">
    <xsl:apply-templates mode="M102" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AIRMET3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMET" mode="M103" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@status = 'NORMAL') then ((exists(iwxxm:analysis)) and (empty(iwxxm:analysis//om:result/@nilReason))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@status = 'NORMAL') then ((exists(iwxxm:analysis)) and (empty(iwxxm:analysis//om:result/@nilReason))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AIRMET3: There must be at least one analysis when a AIRMET does not have canceled status</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M103" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M103" priority="-1" />
  <xsl:template match="@*|node()" mode="M103" priority="-2">
    <xsl:apply-templates mode="M103" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AIRMET4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMET" mode="M104" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AIRMET4: The procedure of an AIRMET analysis should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M104" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M104" priority="-1" />
  <xsl:template match="@*|node()" mode="M104" priority="-2">
    <xsl:apply-templates mode="M104" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN AIRMET.AIRMET1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AIRMET" mode="M105" priority="1000">
    <svrl:fired-rule context="//iwxxm:AIRMET" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if((@status ne 'CANCELLATION') and exists(//iwxxm:analysis/om:OM_Observation)) then(exists(//iwxxm:analysis/om:OM_Observation/om:result/iwxxm:AIRMETEvolvingConditionCollection)) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if((@status ne 'CANCELLATION') and exists(//iwxxm:analysis/om:OM_Observation)) then(exists(//iwxxm:analysis/om:OM_Observation/om:result/iwxxm:AIRMETEvolvingConditionCollection)) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>AIRMET.AIRMET1: OBS and FCST classifications must have a result type of AIRMETEvolvingConditionCollection</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M105" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M105" priority="-1" />
  <xsl:template match="@*|node()" mode="M105" priority="-2">
    <xsl:apply-templates mode="M105" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCFC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneForecastConditions" mode="M106" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneForecastConditions" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:maximumSurfaceWindSpeed) and (not(exists(iwxxm:maximumSurfaceWindSpeed/@xsi:nil)) or iwxxm:maximumSurfaceWindSpeed/@xsi:nil != 'true')) then ((iwxxm:maximumSurfaceWindSpeed/@uom = 'm/s') or (iwxxm:maximumSurfaceWindSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:maximumSurfaceWindSpeed) and (not(exists(iwxxm:maximumSurfaceWindSpeed/@xsi:nil)) or iwxxm:maximumSurfaceWindSpeed/@xsi:nil != 'true')) then ((iwxxm:maximumSurfaceWindSpeed/@uom = 'm/s') or (iwxxm:maximumSurfaceWindSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCFC1: maximumSurfaceWindSpeed shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M106" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M106" priority="-1" />
  <xsl:template match="@*|node()" mode="M106" priority="-2">
    <xsl:apply-templates mode="M106" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCA4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneAdvisory" mode="M107" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:forecast)) then(not(exists(iwxxm:forecast//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:forecast)) then(not(exists(iwxxm:forecast//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCA4: The procedure of a TCA forecast should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M107" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M107" priority="-1" />
  <xsl:template match="@*|node()" mode="M107" priority="-2">
    <xsl:apply-templates mode="M107" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCA2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneAdvisory" mode="M108" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:forecast)) then(not(exists(iwxxm:forecast//om:result/*[name() != 'iwxxm:TropicalCycloneForecastConditions']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:forecast)) then(not(exists(iwxxm:forecast//om:result/*[name() != 'iwxxm:TropicalCycloneForecastConditions']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCA2: The result of a TCA forecast should be a TropicalCycloneForecastConditions</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M108" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M108" priority="-1" />
  <xsl:template match="@*|node()" mode="M108" priority="-2">
    <xsl:apply-templates mode="M108" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCA3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneAdvisory" mode="M109" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCA3: The procedure of a TCA observation should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M109" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M109" priority="-1" />
  <xsl:template match="@*|node()" mode="M109" priority="-2">
    <xsl:apply-templates mode="M109" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCA1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneAdvisory" mode="M110" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:result/*[name() != 'iwxxm:TropicalCycloneObservedConditions']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:observation)) then(not(exists(iwxxm:observation//om:result/*[name() != 'iwxxm:TropicalCycloneObservedConditions']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCA1: The result of a TCA observation should be a TropicalCycloneObservedConditions</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M110" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M110" priority="-1" />
  <xsl:template match="@*|node()" mode="M110" priority="-2">
    <xsl:apply-templates mode="M110" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCOC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneObservedConditions" mode="M111" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneObservedConditions" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:centralPressure) and (not(exists(iwxxm:centralPressure/@xsi:nil)) or iwxxm:centralPressure/@xsi:nil != 'true')) then (iwxxm:centralPressure/@uom = 'hPa') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:centralPressure) and (not(exists(iwxxm:centralPressure/@xsi:nil)) or iwxxm:centralPressure/@xsi:nil != 'true')) then (iwxxm:centralPressure/@uom = 'hPa') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCOC1: centralPressure shall be reported in hectopascals (hPa).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M111" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M111" priority="-1" />
  <xsl:template match="@*|node()" mode="M111" priority="-2">
    <xsl:apply-templates mode="M111" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCOC2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneObservedConditions" mode="M112" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneObservedConditions" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:meanMaxSurfaceWind) and (not(exists(iwxxm:meanMaxSurfaceWind/@xsi:nil)) or iwxxm:meanMaxSurfaceWind/@xsi:nil != 'true')) then ((iwxxm:meanMaxSurfaceWind/@uom = 'm/s') or (iwxxm:meanMaxSurfaceWind/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:meanMaxSurfaceWind) and (not(exists(iwxxm:meanMaxSurfaceWind/@xsi:nil)) or iwxxm:meanMaxSurfaceWind/@xsi:nil != 'true')) then ((iwxxm:meanMaxSurfaceWind/@uom = 'm/s') or (iwxxm:meanMaxSurfaceWind/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCOC2: meanMaxSurfaceWind shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M112" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M112" priority="-1" />
  <xsl:template match="@*|node()" mode="M112" priority="-2">
    <xsl:apply-templates mode="M112" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCOC3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneObservedConditions" mode="M113" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneObservedConditions" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:movementDirection) and (not(exists(iwxxm:movementDirection/@xsi:nil)) or iwxxm:movementDirection/@xsi:nil != 'true')) then (iwxxm:movementDirection/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:movementDirection) and (not(exists(iwxxm:movementDirection/@xsi:nil)) or iwxxm:movementDirection/@xsi:nil != 'true')) then (iwxxm:movementDirection/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCOC3: movementDirection shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M113" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M113" priority="-1" />
  <xsl:template match="@*|node()" mode="M113" priority="-2">
    <xsl:apply-templates mode="M113" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN TCA.TCOC4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:TropicalCycloneObservedConditions" mode="M114" priority="1000">
    <svrl:fired-rule context="//iwxxm:TropicalCycloneObservedConditions" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:movementSpeed) and (not(exists(iwxxm:movementSpeed/@xsi:nil)) or iwxxm:movementSpeed/@xsi:nil != 'true')) then ((iwxxm:movementSpeed/@uom = 'km/h') or (iwxxm:movementSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:movementSpeed) and (not(exists(iwxxm:movementSpeed/@xsi:nil)) or iwxxm:movementSpeed/@xsi:nil != 'true')) then ((iwxxm:movementSpeed/@uom = 'km/h') or (iwxxm:movementSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>TCA.TCOC4: movementSpeed shall be reported in kilometres per hour (km/h) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M114" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M114" priority="-1" />
  <xsl:template match="@*|node()" mode="M114" priority="-2">
    <xsl:apply-templates mode="M114" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN VAA.VAC1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:VolcanicAshCloud" mode="M115" priority="1000">
    <svrl:fired-rule context="//iwxxm:VolcanicAshCloud" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:directionOfMotion) and (not(exists(iwxxm:directionOfMotion/@xsi:nil)) or iwxxm:directionOfMotion/@xsi:nil != 'true')) then (iwxxm:directionOfMotion/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:directionOfMotion) and (not(exists(iwxxm:directionOfMotion/@xsi:nil)) or iwxxm:directionOfMotion/@xsi:nil != 'true')) then (iwxxm:directionOfMotion/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>VAA.VAC1: directionOfMotion shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M115" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M115" priority="-1" />
  <xsl:template match="@*|node()" mode="M115" priority="-2">
    <xsl:apply-templates mode="M115" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN VAA.VAC2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:VolcanicAshCloud" mode="M116" priority="1000">
    <svrl:fired-rule context="//iwxxm:VolcanicAshCloud" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:speedOfMotion) and (not(exists(iwxxm:speedOfMotion/@xsi:nil)) or iwxxm:speedOfMotion/@xsi:nil != 'true')) then ((iwxxm:speedOfMotion/@uom = 'km/h') or (iwxxm:speedOfMotion/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:speedOfMotion) and (not(exists(iwxxm:speedOfMotion/@xsi:nil)) or iwxxm:speedOfMotion/@xsi:nil != 'true')) then ((iwxxm:speedOfMotion/@uom = 'km/h') or (iwxxm:speedOfMotion/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>VAA.VAC2: speedOfMotion shall be reported in kilometres per hour (km/h) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M116" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M116" priority="-1" />
  <xsl:template match="@*|node()" mode="M116" priority="-2">
    <xsl:apply-templates mode="M116" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN VAA.VAC3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:VolcanicAshCloud" mode="M117" priority="1000">
    <svrl:fired-rule context="//iwxxm:VolcanicAshCloud" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:windDirection) and (not(exists(iwxxm:windDirection/@xsi:nil)) or iwxxm:windDirection/@xsi:nil != 'true')) then (iwxxm:windDirection/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:windDirection) and (not(exists(iwxxm:windDirection/@xsi:nil)) or iwxxm:windDirection/@xsi:nil != 'true')) then (iwxxm:windDirection/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>VAA.VAC3: windDirection shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M117" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M117" priority="-1" />
  <xsl:template match="@*|node()" mode="M117" priority="-2">
    <xsl:apply-templates mode="M117" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN VAA.VAC4-->


  <!--RULE -->
  <xsl:template match="//iwxxm:VolcanicAshCloud" mode="M118" priority="1000">
    <svrl:fired-rule context="//iwxxm:VolcanicAshCloud" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:windSpeed) and (not(exists(iwxxm:windSpeed/@xsi:nil)) or iwxxm:windSpeed/@xsi:nil != 'true')) then ((iwxxm:windSpeed/@uom = 'm/s') or (iwxxm:windSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:windSpeed) and (not(exists(iwxxm:windSpeed/@xsi:nil)) or iwxxm:windSpeed/@xsi:nil != 'true')) then ((iwxxm:windSpeed/@uom = 'm/s') or (iwxxm:windSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>VAA.VAC4: windSpeed shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M118" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M118" priority="-1" />
  <xsl:template match="@*|node()" mode="M118" priority="-2">
    <xsl:apply-templates mode="M118" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN VAA.VAA2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:VolcanicAshAdvisory" mode="M119" priority="1000">
    <svrl:fired-rule context="//iwxxm:VolcanicAshAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(empty(om:result/@nilReason) and exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:procedure/*[name() != 'metce:Process']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(empty(om:result/@nilReason) and exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:procedure/*[name() != 'metce:Process']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>VAA.VAA2: The procedure of a VAA analysis should be a metce:Process</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M119" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M119" priority="-1" />
  <xsl:template match="@*|node()" mode="M119" priority="-2">
    <xsl:apply-templates mode="M119" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN VAA.VAA1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:VolcanicAshAdvisory" mode="M120" priority="1000">
    <svrl:fired-rule context="//iwxxm:VolcanicAshAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:result/*[name() != 'iwxxm:VolcanicAshConditions']))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:analysis)) then(not(exists(iwxxm:analysis//om:result/*[name() != 'iwxxm:VolcanicAshConditions']))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>VAA.VAA1: The result of a VAA analysis should be a VolcanicAshConditions</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M120" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M120" priority="-1" />
  <xsl:template match="@*|node()" mode="M120" priority="-2">
    <xsl:apply-templates mode="M120" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.CL1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:CloudLayer" mode="M121" priority="1000">
    <svrl:fired-rule context="//iwxxm:CloudLayer" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:base) and (not(exists(iwxxm:base/@xsi:nil)) or iwxxm:base/@xsi:nil != 'true')) then ((iwxxm:base/@uom = 'm') or (iwxxm:base/@uom = '[ft_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:base) and (not(exists(iwxxm:base/@xsi:nil)) or iwxxm:base/@xsi:nil != 'true')) then ((iwxxm:base/@uom = 'm') or (iwxxm:base/@uom = '[ft_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.CL1: base shall be reported in metres (m) or feet ([ft_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M121" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M121" priority="-1" />
  <xsl:template match="@*|node()" mode="M121" priority="-2">
    <xsl:apply-templates mode="M121" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.Report4-->


  <!--RULE -->
  <xsl:template
    match="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory"
    mode="M122" priority="1000">
    <svrl:fired-rule
      context="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="sum( //iwxxm:extension/.//text()/string-length(.) ) +sum( //iwxxm:extension/.//element()/( (string-length( name() ) * 2 ) + 5 ) ) +sum( //iwxxm:extension/.//@*/( 1 + string-length(name()) + 3 + string-length(.) ) ) +sum( //iwxxm:extension/.//comment()/( string-length( . ) + 7 ) ) lt 5000" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="sum( //iwxxm:extension/.//text()/string-length(.) ) +sum( //iwxxm:extension/.//element()/( (string-length( name() ) * 2 ) + 5 ) ) +sum( //iwxxm:extension/.//@*/( 1 + string-length(name()) + 3 + string-length(.) ) ) +sum( //iwxxm:extension/.//comment()/( string-length( . ) + 7 ) ) lt 5000">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.Report4: Total size of extension content must not exceed 5000 characters per report</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M122" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M122" priority="-1" />
  <xsl:template match="@*|node()" mode="M122" priority="-2">
    <xsl:apply-templates mode="M122" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.Report2-->


  <!--RULE -->
  <xsl:template
    match="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory"
    mode="M123" priority="1000">
    <svrl:fired-rule
      context="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@permissibleUsage eq 'OPERATIONAL') then( not( exists(@permissibleUsageReason))) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@permissibleUsage eq 'OPERATIONAL') then( not( exists(@permissibleUsageReason))) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.Report2: Operational reports should not include a permissibleUsageReason</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M123" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M123" priority="-1" />
  <xsl:template match="@*|node()" mode="M123" priority="-2">
    <xsl:apply-templates mode="M123" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.Report1-->


  <!--RULE -->
  <xsl:template
    match="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory"
    mode="M124" priority="1000">
    <svrl:fired-rule
      context="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if(@permissibleUsage eq 'NON-OPERATIONAL') then( exists(@permissibleUsageReason) ) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if(@permissibleUsage eq 'NON-OPERATIONAL') then( exists(@permissibleUsageReason) ) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.Report1: Non-operational reports must include a permissibleUsageReason</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M124" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M124" priority="-1" />
  <xsl:template match="@*|node()" mode="M124" priority="-2">
    <xsl:apply-templates mode="M124" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.Report3-->


  <!--RULE -->
  <xsl:template
    match="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory"
    mode="M125" priority="1000">
    <svrl:fired-rule
      context="//iwxxm:METAR|//iwxxm:SPECI|//iwxxm:TAF|//iwxxm:SIGMET|//iwxxm:VolcanicAshSIGMET|//iwxxm:TropicalCycloneSIGMET|//iwxxm:AIRMET|//iwxxm:TropicalCycloneAdvisory|//iwxxm:VolcanicAshAdvisory" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if( exists(@translatedBulletinID) or exists(@translatedBulletinReceptionTime) or exists(@translationCentreDesignator) or exists(@translationCentreName) or exists(@translationTime) or exists(@translationFailedTAC)) then( exists(@translatedBulletinID) and exists(@translatedBulletinReceptionTime) and exists(@translationCentreDesignator) and exists(@translationCentreName) and exists(@translationTime)) else(true()))" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if( exists(@translatedBulletinID) or exists(@translatedBulletinReceptionTime) or exists(@translationCentreDesignator) or exists(@translationCentreName) or exists(@translationTime) or exists(@translationFailedTAC)) then( exists(@translatedBulletinID) and exists(@translatedBulletinReceptionTime) and exists(@translationCentreDesignator) and exists(@translationCentreName) and exists(@translationTime)) else(true()))">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.Report3: Translated reports must include translatedBulletinID, translatedBulletinReceptionTime, translationCentreDesignator, translationCentreName, translationTime and optionally translationFailedTAC if translation failed</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M125" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M125" priority="-1" />
  <xsl:template match="@*|node()" mode="M125" priority="-2">
    <xsl:apply-templates mode="M125" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.ACF1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeCloudForecast" mode="M126" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeCloudForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( exists(iwxxm:verticalVisibility) ) then empty(iwxxm:layer) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( exists(iwxxm:verticalVisibility) ) then empty(iwxxm:layer) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.ACF1: Vertical visibility cannot be reported together with cloud layers</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M126" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M126" priority="-1" />
  <xsl:template match="@*|node()" mode="M126" priority="-2">
    <xsl:apply-templates mode="M126" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.ACF2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeCloudForecast" mode="M127" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeCloudForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:verticalVisibility) and (not(exists(iwxxm:verticalVisibility/@xsi:nil)) or iwxxm:verticalVisibility/xsi:nil != 'true')) then ((iwxxm:verticalVisibility/@uom = 'm') or (iwxxm:verticalVisibility/@uom = '[ft_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:verticalVisibility) and (not(exists(iwxxm:verticalVisibility/@xsi:nil)) or iwxxm:verticalVisibility/xsi:nil != 'true')) then ((iwxxm:verticalVisibility/@uom = 'm') or (iwxxm:verticalVisibility/@uom = '[ft_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.ACF2: verticalVisibility shall be reported in metres (m) or feet ([ft_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M127" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M127" priority="-1" />
  <xsl:template match="@*|node()" mode="M127" priority="-2">
    <xsl:apply-templates mode="M127" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.ASWF1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWindForecast" mode="M128" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWindForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="(if( @variableDirection eq 'true' ) then ( empty(iwxxm:meanWindDirection) ) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert test="(if( @variableDirection eq 'true' ) then ( empty(iwxxm:meanWindDirection) ) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.ASWF1: Wind direction is not reported when variable winds are indicated</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M128" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M128" priority="-1" />
  <xsl:template match="@*|node()" mode="M128" priority="-2">
    <xsl:apply-templates mode="M128" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.ASWTF1-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWindTrendForecast|//iwxxm:AerodromeSurfaceWindForecast" mode="M129" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWindTrendForecast|//iwxxm:AerodromeSurfaceWindForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:meanWindDirection) and (not(exists(iwxxm:meanWindDirection/@xsi:nil)) or iwxxm:meanWindDirection/@xsi:nil != 'true')) then (iwxxm:meanWindDirection/@uom = 'deg') else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:meanWindDirection) and (not(exists(iwxxm:meanWindDirection/@xsi:nil)) or iwxxm:meanWindDirection/@xsi:nil != 'true')) then (iwxxm:meanWindDirection/@uom = 'deg') else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.ASWTF1: meanWindDirection shall be reported in degrees (deg).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M129" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M129" priority="-1" />
  <xsl:template match="@*|node()" mode="M129" priority="-2">
    <xsl:apply-templates mode="M129" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.ASWTF2-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWindTrendForecast|//iwxxm:AerodromeSurfaceWindForecast" mode="M130" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWindTrendForecast|//iwxxm:AerodromeSurfaceWindForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:meanWindSpeed) and (not(exists(iwxxm:meanWindSpeed/@xsi:nil)) or iwxxm:meanWindSpeed/@xsi:nil != 'true')) then ((iwxxm:meanWindSpeed/@uom = 'm/s') or (iwxxm:meanWindSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:meanWindSpeed) and (not(exists(iwxxm:meanWindSpeed/@xsi:nil)) or iwxxm:meanWindSpeed/@xsi:nil != 'true')) then ((iwxxm:meanWindSpeed/@uom = 'm/s') or (iwxxm:meanWindSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.ASWTF2: meanWindSpeed shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M130" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M130" priority="-1" />
  <xsl:template match="@*|node()" mode="M130" priority="-2">
    <xsl:apply-templates mode="M130" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN COMMON.ASWTF3-->


  <!--RULE -->
  <xsl:template match="//iwxxm:AerodromeSurfaceWindTrendForecast|//iwxxm:AerodromeSurfaceWindForecast" mode="M131" priority="1000">
    <svrl:fired-rule context="//iwxxm:AerodromeSurfaceWindTrendForecast|//iwxxm:AerodromeSurfaceWindForecast" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when
        test="(if(exists(iwxxm:windGustSpeed) and (not(exists(iwxxm:windGustSpeed/@xsi:nil)) or iwxxm:windGustSpeed/@xsi:nil != 'true')) then ((iwxxm:windGustSpeed/@uom = 'm/s') or (iwxxm:windGustSpeed/@uom = '[kn_i]')) else true())" />
      <xsl:otherwise>
        <svrl:failed-assert
          test="(if(exists(iwxxm:windGustSpeed) and (not(exists(iwxxm:windGustSpeed/@xsi:nil)) or iwxxm:windGustSpeed/@xsi:nil != 'true')) then ((iwxxm:windGustSpeed/@uom = 'm/s') or (iwxxm:windGustSpeed/@uom = '[kn_i]')) else true())">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>COMMON.ASWTF3: windGustSpeed shall be reported in metres per second (m/s) or knots ([kn_i]).</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M131" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M131" priority="-1" />
  <xsl:template match="@*|node()" mode="M131" priority="-2">
    <xsl:apply-templates mode="M131" select="*|comment()|processing-instruction()" />
  </xsl:template>

  <!--PATTERN IWXXM.ExtensionAlwaysLast-->


  <!--RULE -->
  <xsl:template match="//iwxxm:extension" mode="M132" priority="1000">
    <svrl:fired-rule context="//iwxxm:extension" />

    <!--ASSERT -->
    <xsl:choose>
      <xsl:when test="following-sibling::*[1][self::iwxxm:extension] or not(following-sibling::*)" />
      <xsl:otherwise>
        <svrl:failed-assert test="following-sibling::*[1][self::iwxxm:extension] or not(following-sibling::*)">
          <xsl:attribute name="location">
            <xsl:apply-templates mode="schematron-select-full-path" select="." />
          </xsl:attribute>
          <svrl:text>IWXXM.ExtensionAlwaysLast: Extension elements should be the last elements in their parents</svrl:text>
        </svrl:failed-assert>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates mode="M132" select="*|comment()|processing-instruction()" />
  </xsl:template>
  <xsl:template match="text()" mode="M132" priority="-1" />
  <xsl:template match="@*|node()" mode="M132" priority="-2">
    <xsl:apply-templates mode="M132" select="*|comment()|processing-instruction()" />
  </xsl:template>
</xsl:stylesheet>

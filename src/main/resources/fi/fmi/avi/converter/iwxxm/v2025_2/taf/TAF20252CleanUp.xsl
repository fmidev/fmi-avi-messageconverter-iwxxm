<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:gml="http://www.opengis.net/gml/3.2"
  xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
  xmlns:iwxxm="http://icao.int/iwxxm/2025-2"
  version="2.0">
  <xsl:template match="@*">
    <xsl:copy>
      <xsl:apply-templates select="../@*" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="*">
    <xsl:element name="{name()}" namespace="{namespace-uri()}">
      <xsl:variable name="vtheElem" select="." />

      <xsl:for-each select="namespace::*">
        <xsl:variable name="vPrefix" select="name()" />

        <xsl:if test=
          "$vtheElem/descendant::*
              [(namespace-uri()=current()
             and
              substring-before(name(),':') = $vPrefix)
             or
              @*[substring-before(name(),':') = $vPrefix]
              ]
        ">
          <xsl:copy-of select="." />
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates select="node()|@*" />
    </xsl:element>
  </xsl:template>
  <xsl:template match="//gml:boundedBy[@xsi:nil='true']" />

  <!-- Full AIXM/JAXB emits nil placeholders for absent optional properties. -->
  <xsl:template match="aixm:*[@xsi:nil='true' and not(@nilReason)]"/>
  <xsl:template match="iwxxm:cloudType[@xsi:nil='true' and not(@nilReason)]"/>
  <xsl:template match="iwxxm:verticalVisibility[@xsi:nil='true' and not(@nilReason)]"/>
  <xsl:template match="iwxxm:verticalVisibility[@nilReason]">
    <xsl:copy copy-namespaces="no">
      <xsl:apply-templates select="@*[not(local-name()='nil' and namespace-uri()='http://www.w3.org/2001/XMLSchema-instance')]"/>
      <xsl:attribute name="xsi:nil">true</xsl:attribute>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
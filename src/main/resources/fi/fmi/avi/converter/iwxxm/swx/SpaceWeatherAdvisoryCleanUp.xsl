<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:gml="http://www.opengis.net/gml/3.2"
  xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
  version="2.0">
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>
  <!--qqq
  <xsl:template match="*">
    <xsl:element name="{name()}" namespace="{namespace-uri()}">
      <xsl:variable name="vtheElem" select="."/>

      <xsl:for-each select="namespace::*">
        <xsl:variable name="vPrefix" select="name()"/>

        <xsl:if test=
          "$vtheElem/descendant::*
              [(namespace-uri()=current()
             and
              substring-before(name(),':') = $vPrefix)
             or
              @*[substring-before(name(),':') = $vPrefix]
              ]
        ">
          <xsl:copy-of select="."/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:element>
  </xsl:template>-->

  <xsl:template match="//gml:boundedBy[@xsi:nil='true']"/>
  <xsl:template match="//aixm:upperLimit[@xsi:nil='true']"/>
  <xsl:template match="//aixm:upperLimitReference[@xsi:nil='true']"/>
  <xsl:template match="//aixm:maximumLimit[@xsi:nil='true']"/>
  <xsl:template match="//aixm:maximumLimitReference[@xsi:nil='true']"/>
  <xsl:template match="//aixm:lowerLimit[@xsi:nil='true']"/>
  <xsl:template match="//aixm:lowerLimitReference[@xsi:nil='true']"/>
  <xsl:template match="//aixm:minimumLimit[@xsi:nil='true']"/>
  <xsl:template match="//aixm:minimumLimitReference[@xsi:nil='true']"/>
  <xsl:template match="//aixm:width[@xsi:nil='true']"/>
  <xsl:template match="//aixm:horizontalAccuracy[@xsi:nil='true']"/>
  <xsl:template match="//aixm:centreline[@xsi:nil='true']"/>
  <xsl:template match="//aixm:type[@xsi:nil='true']"/>
  <xsl:template match="//aixm:designator[@xsi:nil='true']"/>


</xsl:stylesheet>
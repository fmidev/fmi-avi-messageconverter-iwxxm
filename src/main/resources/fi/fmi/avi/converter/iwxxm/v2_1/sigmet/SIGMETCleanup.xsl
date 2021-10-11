<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
  xmlns:gml="http://www.opengis.net/gml/3.2"
  xmlns:om="http://www.opengis.net/om/2.0"
  xmlns:iwxxm="http://icao.int/iwxxm/2.1">
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
  <xsl:template match="//aixm:upperLimit[@xsi:nil='true']" />
  <xsl:template match="//aixm:upperLimitReference[@xsi:nil='true']" />
  <xsl:template match="//aixm:lowerLimit[@xsi:nil='true']" />
  <xsl:template match="//aixm:lowerLimitReference[@xsi:nil='true']" />
  <xsl:template match="//aixm:designatorICAO[@xsi:nil='true']" />
  <xsl:template match="//aixm:horizontalAccuracy[@xsi:nil='true']" />
  <xsl:template match="//aixm:minimumLimit[@xsi:nil='true']" />
  <xsl:template match="//aixm:minimumLimitReference[@xsi:nil='true']" />
  <xsl:template match="//aixm:maximumLimit[@xsi:nil='true']" />
  <xsl:template match="//aixm:maximumLimitReference[@xsi:nil='true']" />
  <xsl:template match="//aixm:centreline[@xsi:nil='true']" />
  <xsl:template match="//aixm:width[@xsi:nil='true']" />

  <xsl:template match="//om:result[@xsi:type='iwxxm:MeteorologicalAerodromeForecastRecordPropertyType']/@xsi:type" />

  <xsl:template match="//iwxxm:SIGMETEvolvingCondition">
        <iwxxm:SIGMETEvolvingCondition>
            <xsl:apply-templates select = "@*"/>
            <xsl:if test="not(iwxxm:directionOfMotion)">
                <xsl:if test="iwxxm:speedOfMotion">
                    <iwxxm:directionOfMotion uom="deg" xsi:nil="true" nilReason="http://codes.wmo.int/common/nil/missing"/>
                </xsl:if>
            </xsl:if>
            <xsl:apply-templates select = "node()"/>
        </iwxxm:SIGMETEvolvingCondition>
    </xsl:template>
</xsl:stylesheet>
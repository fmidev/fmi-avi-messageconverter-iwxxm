<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
  xmlns:gml="http://www.opengis.net/gml/3.2"
  xmlns:om="http://www.opengis.net/om/2.0"
  xmlns:iwxxm="http://icao.int/iwxxm/2.1"
  version="2.0">
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="//gml:boundedBy[@xsi:nil='true']"/>
    <xsl:template match="//aixm:upperLimit[@xsi:nil='true']"/>
    <xsl:template match="//aixm:upperLimitReference[@xsi:nil='true']"/>
    <xsl:template match="//iwxxm:movingSpeed[@xsi:nil='true']"/>
    <xsl:template match="//iwxxm:movingDirection[@xsi:nil='true']"/>
    <xsl:template match="//aixm:lowerLimit[@xsi:nil='true']"/>
    <xsl:template match="//aixm:lowerLimitReference[@xsi:nil='true']"/>
    <xsl:template match="//aixm:designatorICAO[@xsi:nil='true']"/>
    <xsl:template match="//aixm:horizontalAccuracy[@xsi:nil='true']"/>
    <xsl:template match="//om:result[@xsi:type='iwxxm:MeteorologicalAerodromeForecastRecordPropertyType']/@xsi:type"/>
    <xsl:template match="//iwxxm:AIRMETEvolvingCondition">
        <iwxxm:AIRMETEvolvingCondition>
            <xsl:apply-templates select = "@*"/>
            <xsl:if test="not(iwxxm:directionOfMotion)">
                <xsl:if test="iwxxm:speedOfMotion">
                    <iwxxm:directionOfMotion uom="deg" xsi:nil="true" nilReason="http://codes.wmo.int/common/nil/missing"/>
                </xsl:if>
            </xsl:if>
            <xsl:apply-templates select = "node()"/>
        </iwxxm:AIRMETEvolvingCondition>
    </xsl:template>
</xsl:stylesheet>
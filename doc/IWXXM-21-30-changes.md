#  Changes IWXXM XML Schemas between versions 2.1.1 and 3.0.0

Support for handling both IWXXM 2.1.x and 3.0.x IWXXM messages at the same time will be gradually added to 
the fmi-avi-messageconverter-iwxxm library. This document lists all the schema changes to be taken into 
account when adding support for IWXXM 3.0.

## Common elements and types
TODO

## TAF
1. ```TAFType```:
   1. ```status``` attribute removed, status moved to base ReportType
   1. new element ```aerodrome```
   1. new element ```cancelledReportValidPeriod```
   1. ```validTime``` element renamed to ```validPeriod``` 
   1. ```baseForecast``` element now of type ```iwxxm:MeteorologicalAerodromeForecastPropertyType``` instead of ```om:OM_ObservationPropertyType```
   1. ```changeForecast``` element now of type ```iwxxm:MeteorologicalAerodromeForecastPropertyType``` instead of ```om:OM_ObservationPropertyType```
   1. ```previousReportAerodrome``` element removed
   1. ```previousReportValidPeriod``` element removed
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
   1. new attribute ```isCancelReport```
1. ```MeteorologicalAerodromeForecastRecord``` element renamed to ```MeteorologicalAerodromeForecast```
1. ```MeteorologicalAerodromeTrendForecastRecordPropertyType``` element renamed to ```MeteorologicalAerodromeTrendForecastPropertyType```
1. ```MeteorologicalAerodromeForecastRecordType``` type renamed to ```MeteorologicalAerodromeForecastType```
1. ```MeteorologicalAerodromeForecastType```:
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
   1. new element ```phenomenonTime```
1. ```AerodromeAirTemperatureForecastType```: ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```TAFReportStatusType``` removed

## METAR / SPECI

1. ```MeteorologicalAerodromeObservationReportType```:
   1. ```observation``` element is now nillable and of type ```iwxxm:MeteorologicalAerodromeObservationPropertyType``` instead of ```om:OM_ObservationPropertyType```
   1. ```trendForeast``` element now of type ```iwxxm:MeteorologicalAerodromeTrendForecastPropertyType``` instead of ```om:OM_ObservationPropertyType```
   1. ```status``` attribute removed, status moved to base ReportType
   1. new element ```aerodrome```
   1. new element ```issueTime```
   1. new element ```observationTime```
1. ```SPECIType```: ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```METARType```: ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```MeteorologicalAerodromeTrendForecastRecord``` element renamed to ```MeteorologicalAerodromeTrendForecast```
1. ```MeteorologicalAerodromeTrendForecastRecordType``` type renamed to ```MeteorologicalAerodromeTrendForecastType```
1. ```MeteorologicalAerodromeTrendForecastRecordPropertyType``` type renamed to ```MeteorologicalAerodromeTrendForecastPropertyType```
1. ```MeteorologicalAerodromeTrendForecastType```:
   1. ```forecastWeather``` element renamed to ```weather```
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
   1. new element ```phenomenonTime```
   1. new element ```timeIndicator```
1. ```MeteorologicalAerodromeObservationRecord``` element renamed to ```MeteorologicalAerodromeObservation```
1. ```MeteorologicalAerodromeObservationRecordType``` type renamed to ```MeteorologicalAerodromeObservationType```
1. ```MeteorologicalAerodromeObservationRecordPropertyType``` type renamed to ```MeteorologicalAerodromeObservationPropertyType```
1. ```MeteorologicalAerodromeObservationType```:
   1. ```airTemperature``` element now nillable and of type ```"iwxxm:MeasureWithNilReasonType``` instead of ```gml:MeasureType```
   1. ```dewpointTemperature``` element now nillable and of type ```"iwxxm:MeasureWithNilReasonType``` instead of ```gml:MeasureType```
   1. ```qnh``` element now nillable and of type ```"iwxxm:MeasureWithNilReasonType``` instead of ```gml:MeasureType```
   1. ```surfaceWind``` element now nillable and of anonymous complex type extending ```iwxxm:AerodromeSurfaceWindPropertyType``` (adds attribute nilReason) 
   instead of ```iwxxm:AerodromeSurfaceWindPropertyType```
   1. ```visibility``` element now nillable and of anonymous complex type extending ```iwxxm:AerodromeHorizontalVisibilityPropertyType``` (adds attribute 
   nilReason) instead of ```iwxxm:AerodromeHorizontalVisibilityPropertyType```
   1. ```rvr``` element now nillable and of anonymous complex type extending ```iwxxm:AerodromeRunwayVisualRangePropertyType``` (adds attribute 
      nilReason) instead of ```iwxxm:AerodromeRunwayVisualRangePropertyType```
   1. ```cloud``` now of anonymous complex type extending ```iwxxm:AerodromeCloudPropertyType``` instead of anynymous complex type ```"iwxxm:AerodromeObservedCloudsPropertyType```
   1. ```recentWeather``` element is now nillable
   1.  ```windShear``` element now nillable and of anonymous complex type extending ```iwxxm:AerodromeWindShearPropertyType``` (adds attribute 
            nilReason) instead of ```iwxxm:AerodromeWindShearPropertyType```
   1. ```seaState``` element renamed to ```seaCondition```, is nillable, and now of anonymous complex type extending 
   ```iwxxm:AerodromeSeaConditionPropertyType``` (adds attribute nilReason) instead of ```iwxxm:AerodromeSeaStatePropertyType```
  1. ```runwayState``` element now nillable and of anonymous complex type extending ```iwxxm:AerodromeRunwayStatePropertyType``` (adds attribute 
                nilReason) instead of ```iwxxm:AerodromeRunwayStatePropertyType```
  1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```MeteorologicalAerodromeObservationRecordPropertyType``` renamed to ```MeteorologicalAerodromeObservationPropertyType```
1. ```AerodromeRunwayStateType```:
   1. ```runway``` now nillable
   1. ```depositType``` now nillable
   1. ```contamination``` now nillable
   1. ```estimatedSurfaceFrictionOrBrakingAction``` now nillable
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
   1. attribute ```snowClosure``` removed
   1. new attribute ```fromPreviousReport```
1. ```AerodromeRunwayVisualRangeType```:
   1. ```runway``` now nillable
   1. ```meanRVR``` now nillable and of type ```iwxxm:DistanceWithNilReasonType``` instead of ```gml:LengthType```
   1. ```meanRVROperator``` now nillable
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```AerodromeSeaState``` renamed to ```AerodromeSeaCondition```
1. ```AerodromeSeaStateType``` renamed to ```AerodromeSeaConditionType```
1. ```AerodromeSeaStatePropertyType``` renamed to ```AerodromeSeaConditionPropertyType```
1. ```AerodromeSeaConditionType```:
   1. ```significantWaveHeight``` now nillable and of type ```iwxxm:DistanceWithNilReasonType``` instead of ```gml:LengthType```
   1. ```seaState``` now nillable
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```AerodromeWindShearType```: ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```AerodromeObservedClouds``` renamed to ```AerodromeCloud```
1. ```AerodromeObservedCloudsType``` renamed to ```AerodromeCloudType```
1. ```AerodromeObservedCloudsPropertyType``` renamed to ```AerodromeCloudPropertyType```
1. ```AerodromeCloudType```: ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```AerodromeSurfaceWindType```:
   1. ```meanWindDirection``` now nillable and of type ```iwxxm:AngleWithNilReasonType``` instead of ```gml:AngleType```
   1. ```meanWindSpeed``` now nillable and of type ```iwxxm:VelocityWithNilReasonType``` instead of ```gml:SpeedType```
   1. ```meanWindSpeedOperator``` now nillable
   1. ```windGustSpeed``` now nillable and of type ```iwxxm:VelocityWithNilReasonType``` instead of ```gml:SpeedType```
   1. ```windGustSpeedOperator``` now nillable
   1. ```extremeClockwiseWindDirection``` now nillable and of type ```iwxxm:AngleWithNilReasonType``` instead of ```gml:AngleType```
   1. ```extremeCounterClockwiseWindDirection``` now nillable and of type ```iwxxm:AngleWithNilReasonType``` instead of ```gml:AngleType```
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```AerodromeHorizontalVisibilityType```:
   1. ```prevailingVisibility``` now nillable and of type ```iwxxm:DistanceWithNilReasonType``` instead of ```gml:LengthType```
   1. ```prevailingVisibilityOperator``` now nillable
   1. ```minimumVisibility``` now nillable and of type ```iwxxm:DistanceWithNilReasonType``` instead of ```gml:LengthType```
   1. ```minimumVisibilityDirection``` now nillable and of type ```iwxxm:AngleWithNilReasonType``` instead of ```gml:AngleType```
   1. ```extension``` element now of type ```iwxxm:ExtensionType``` instead of ```anyType```
1. ```MeteorologicalAerodromeReportStatusType``` removed
1. new simple type ```TrendForecastTimeIndicatorType```   
   
## SIGMET
TODO
## AIRMET
TODO
## Bulletins (IWXXM Collect)
TODO

## Space Weather Advisories
Space weather advisories are new in IWXXM 3

## Tropical cyclone advisories
TODO

## Volcanic Ash Advisories
TODO

# Changelog - fmi-avi-messageconverter-iwxxm

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- ...

### Changed

- ...

### Deprecated

- ...

### Removed

- ...

### Fixed

- ...

### Security

- ...

## [v6.1.0] - 2025-12-08

### Added

- Added support for Space Weather Advisory IWXXM 2025-2 serialization [#137]

## [v6.0.0] - 2025-11-21

### Added

- Added generic IWXXM 2025-2 parsing for Space Weather Advisories [#136]

### Changed

- Adapted to Annex 3 Amendment 82 Space Weather Advisory model changes [#132]

## [v5.0.0-beta4] - 2024-02-02

### Added

- Added generic IWXXM 2023-1 parsing for AIRMETs and SIGMETs [#130]

## [v5.0.0-beta3] - 2023-11-14

### Added

- Added support for minimal test SIGMETs [#118], [#128]

## [v5.0.0-beta2] - 2023-10-23

### Added

- Experimental support for SIGMET/AIRMET IWXXM 2023-1 serialization [#121]

## [v5.0.0-beta1] - 2023-09-27

### Added

- Experimental support for SIGMET/AIRMET IWXXM 3.0 serialization [#114]

## [v4.2.0] - 2022-08-24

### Changed

- Depend on fmi-avi-messageconverter:6.2.0

## [v4.1.0] - 2022-06-06

### Changed

- Depend on fmi-avi-messageconverter:6.1.0

## [v4.0.0] - 2022-02-22

### Added

- Added support for IWXXM 3.0.0 messages withing COLLECT documents. [#80]
- Added parsing of all SIGMET location indicators to GenericAviationWeatherMessage. [#84]
- Added parsing of message translation status from IWXXM Message [#91]
- Added parsing of xmlNamespace in GenericAviationWeatherMessage [#92]
- Added parsing of generic IWXXM 3.0 METAR messages [#94]
- Added parsing of generic IWXXM 3.0 SPECI messages [#96]
- Added parsing of generic IWXXM 3.0 SPACE WEATHER ADVISORY messages [#97]
- Added parsing of generic IWXXM 3.0 AIRMET messages [#101]
- Added parsing of generic IWXXM 3.0 Tropical cyclone advisory messages [#106]
- Added parsing of generic IWXXM 3.0 Volcanic ash advisory messages [#107]

### Changed

- Adapted to location indicator model changes in GenericAviationMessage. [#82]
- Separated generic message parsing from generic bulletin parsing. [#83]
- Split GenericAviationWeatherMessageScanner into IWXXM version- and message-specific scanners. [#88]
- Depend on fmi-avi-messageconverter:6.0.0

### Fixed
- Declare all required namespaces in root element of message document extracted from a COLLECT document [#110]

## [v3.0.0] - 2021-04-13

### Added

- Created overview documentation for developers. [#64]
- Added support for TAF POJO to IWXXM 3.0.0 conversion. [#51]
- Added support for TAF IWXXM 3.0.0 to POJO conversion. [#52]
- Added support for TAF IWXXM 3.0.0 bulletin (COLLECT) to and from POJO conversion. [#71]

### Changed

- Reorganized bulletin parsers and serializers in the package structure. [#72]
- Adapted to `AviationWeatherMessage.getReportStatus()` being non-`Optional`. [#60]
- Adapted TAF IWXXM 2.1.1 support to model changes for IWXXM 3. [#61]
- Code quality enhancements. [#74]

## Past Changelog

Previous changelog entries are available
on [GitHub releases page](https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases) in a more freeform format.

[Unreleased]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/compare/fmi-avi-messageconverter-iwxxm-6.1.0...HEAD

[v6.1.0]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-6.1.0

[v6.0.0]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-6.0.0

[v5.0.0-beta4]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-5.0.0-beta4

[v5.0.0-beta3]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-5.0.0-beta3

[v5.0.0-beta2]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-5.0.0-beta2

[v5.0.0-beta1]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-5.0.0-beta1

[v4.2.0]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-4.2.0

[v4.1.0]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-4.1.0

[v4.0.0]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-4.0.0

[v3.0.0]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-3.0.0

[#51]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/51

[#52]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/52

[#60]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/60

[#61]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/61

[#64]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/64

[#71]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/71

[#72]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/72

[#74]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/74

[#80]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/80

[#82]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/82

[#83]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/83

[#84]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/84

[#88]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/88

[#91]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/91

[#92]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/92

[#94]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/94

[#96]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/96

[#97]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/97

[#101]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/101

[#106]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/106

[#107]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/107

[#110]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/110

[#114]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/114

[#118]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/118

[#121]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/121

[#128]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/128

[#130]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/130

[#132]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/132

[#136]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/136

[#137]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/pull/137
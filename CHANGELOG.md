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

## [v4.0.0] - 2022-02-22

### Added

- Added generic POJO conversion for IWXXM 3.0.0 messages. [#80]
- Added parsing of all SIGMET location indicators to GenericAviationWeatherMessage. [#84]

### Changed

- Adapted to location indicator model changes in GenericAviationMessage. [#82]
- Separated generic message parsing from generic bulletin parsing. [#83]
- Split GenericAviationWeatherMessageScanner into IWXXM version- and message-specific scanners. [#88]

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

Previous changelog entries are available on [GitHub releases page](https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases) in a more freeform format.

[Unreleased]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/compare/fmi-avi-messageconverter-iwxxm-4.0.0...HEAD

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
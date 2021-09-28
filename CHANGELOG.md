# Changelog - fmi-avi-messageconverter-iwxxm

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v3.0.0] - 2014-04-13

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

[Unreleased]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/compare/fmi-avi-messageconverter-iwxxm-3.0.0...HEAD

[v3.0.0]: https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/releases/tag/fmi-avi-messageconverter-iwxxm-3.0.0

[#51]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/51

[#52]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/52

[#60]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/60

[#61]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/61

[#64]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/64

[#71]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/71

[#72]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/72

[#74]:https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/issues/74

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.5]

### Added
- Mass-fill containers: Shift + Click a fluid inside the Fluid Terminal to fill all compatible empty containers in the player's inventory with that fluid.
- New `FluidTerminalActionHelper` to better handle fluid pickup and storage logic.

### Changed
- Refactored fluid terminal mixins and helpers for consistency and improved fluid handling logic.
- Restructured mixin package hierarchy for better code organization, moving client-side mixins to a dedicated `client` sub-package.

## [1.1.2]

### Changed
- Wireless fluid drain status messages now use translation keys instead of hardcoded text.

## [1.1.0]

### Added
- Bucket-style pickup for the Wireless Fluid Terminal: Shift + Right Click a fluid source block to store 1 bucket (1000 mB) directly into the ME network.
- Compatibility with Universal Wireless Terminal (WUT): fluid pickup works on the WUT as long as it includes the Wireless Fluid Terminal mode installed, regardless of which WUT mode you're currently using.

## [1.0.5]

### Fixed
- Fixed the Portable Fluid Cell container mixin not working on obfuscated clients by avoiding a direct shadow of `detectAndSendChanges` (vanilla `func_75142_b`) and calling it through the base container instead.

## [1.0.0]

### Added
- Initial publish.

[Unreleased]: https://github.com/NuanKi/AE2FluidTerminals-Rework/compare/v1.1.5...HEAD
[1.1.5]: https://github.com/NuanKi/AE2FluidTerminals-Rework/tree/v1.1.5
[1.1.2]: https://github.com/NuanKi/AE2FluidTerminals-Rework/tree/v1.1.2
[1.1.0]: https://github.com/NuanKi/AE2FluidTerminals-Rework/tree/v1.1.0
[1.0.5]: https://github.com/NuanKi/AE2FluidTerminals-Rework/tree/v1.0.5
[1.0.0]: https://github.com/NuanKi/AE2FluidTerminals-Rework/tree/v1.0.0
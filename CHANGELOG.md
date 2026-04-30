# Changelog

## 1.1.1 - 2026-04-30

### Fixed
- iOS: hardened camera launch flow to reduce `EXC_BAD_ACCESS` risk in `launchCamera`.
- iOS: added explicit camera permission gate before presenting the camera picker.
- iOS: added in-flight request guard to prevent overlapping picker calls.
- iOS: module now requires main queue setup for safer UI lifecycle initialization.

### Compatibility
- Supports both React Native architectures dynamically:
  - `new_arch_enabled => true`
  - `new_arch_enabled => false`
- JS bridge now falls back from TurboModule to `NativeModules` when needed, preserving the same public API.


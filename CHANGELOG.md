# Changelog

## 1.1.3 - 2026-05-04

### Fixed
- iOS (New Architecture): fixed critical type signature mismatch in TurboModule bridge layer causing `EXC_BAD_ACCESS` crash.
  - Changed `launchCamera` and `launchImageLibrary` method signatures to accept `JS::NativeReactNativeImagePicker::LaunchOptions &` (C++ struct reference) instead of `NSDictionary *`.
  - Implemented proper parameter marshaling by extracting values from C++ struct via public getter methods.
  - This resolves invalid memory access that occurred when passing parameters across the Objective-C++/C++ boundary.
- iOS: added thread-safe callback access in `ImagePickerService.swift` using serial `DispatchQueue` to prevent race conditions when accessing resolve/reject promises from multiple async contexts.
- iOS: added Codegen-generated spec header import (`ReactNativeImagePickerSpec.h`) to ensure TurboModule protocol compliance.

## 1.1.2 - 2026-05-04

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


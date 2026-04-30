#import "ReactNativeImagePicker.h"
#import <React/RCTBridgeModule.h>
#import "ReactNativeImagePicker-Swift.h"
#ifdef RCT_NEW_ARCH_ENABLED
#import <ReactNativeImagePickerSpec/ReactNativeImagePickerSpec.h>
#endif

@interface ReactNativeImagePicker ()
@property(nonatomic, strong) RNImagePickerService *service;
@end

@implementation ReactNativeImagePicker
RCT_EXPORT_MODULE()

- (BOOL)requiresMainQueueSetup
{
  return YES;
}

- (instancetype)init
{
  if (self = [super init]) {
    _service = [RNImagePickerService new];
  }
  return self;
}

#if RCT_NEW_ARCH_ENABLED
- (NSNumber *)multiply:(double)a b:(double)b {
  NSNumber *result = @(a * b);
  return result;
}
#else
RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(multiply:(double)a
                                      b:(double)b)
{
  return @(a * b);
}
#endif

#if RCT_NEW_ARCH_ENABLED
- (void)launchImageLibrary:(JS::NativeReactNativeImagePicker::LaunchOptions &)options
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject
{
  NSMutableDictionary *optionsDict = [NSMutableDictionary new];
  if (options.selectionLimit()) {
    optionsDict[@"selectionLimit"] = @(options.selectionLimit().value());
  }
  if (options.mediaType()) {
    optionsDict[@"mediaType"] = options.mediaType();
  }
  if (options.maxWidth()) {
    optionsDict[@"maxWidth"] = @(options.maxWidth().value());
  }
  if (options.maxHeight()) {
    optionsDict[@"maxHeight"] = @(options.maxHeight().value());
  }
  if (options.quality()) {
    optionsDict[@"quality"] = @(options.quality().value());
  }
  if (options.includeBase64()) {
    optionsDict[@"includeBase64"] = @(options.includeBase64().value());
  }
  if (options.includeExtra()) {
    optionsDict[@"includeExtra"] = @(options.includeExtra().value());
  }
  if (options.saveToPhotos()) {
    optionsDict[@"saveToPhotos"] = @(options.saveToPhotos().value());
  }
  [self.service launchImageLibraryWithOptions:optionsDict resolve:resolve reject:reject];
}
#else
RCT_REMAP_METHOD(launchImageLibrary,
                 launchImageLibraryLegacy:(NSDictionary *)options
                 resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject)
{
  [self.service launchImageLibraryWithOptions:options resolve:resolve reject:reject];
}
#endif

#if RCT_NEW_ARCH_ENABLED
- (void)launchCamera:(JS::NativeReactNativeImagePicker::LaunchOptions &)options
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
  NSMutableDictionary *optionsDict = [NSMutableDictionary new];
  if (options.selectionLimit()) {
    optionsDict[@"selectionLimit"] = @(options.selectionLimit().value());
  }
  if (options.mediaType()) {
    optionsDict[@"mediaType"] = options.mediaType();
  }
  if (options.maxWidth()) {
    optionsDict[@"maxWidth"] = @(options.maxWidth().value());
  }
  if (options.maxHeight()) {
    optionsDict[@"maxHeight"] = @(options.maxHeight().value());
  }
  if (options.quality()) {
    optionsDict[@"quality"] = @(options.quality().value());
  }
  if (options.includeBase64()) {
    optionsDict[@"includeBase64"] = @(options.includeBase64().value());
  }
  if (options.includeExtra()) {
    optionsDict[@"includeExtra"] = @(options.includeExtra().value());
  }
  if (options.durationLimit()) {
    optionsDict[@"durationLimit"] = @(options.durationLimit().value());
  }
  if (options.videoQuality()) {
    optionsDict[@"videoQuality"] = options.videoQuality();
  }
  if (options.cameraType()) {
    optionsDict[@"cameraType"] = options.cameraType();
  }
  [self.service launchCameraWithOptions:optionsDict resolve:resolve reject:reject];
}
#else
RCT_REMAP_METHOD(launchCamera,
                 launchCameraLegacy:(NSDictionary *)options
                 resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject)
{
  [self.service launchCameraWithOptions:options resolve:resolve reject:reject];
}
#endif

#if RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeReactNativeImagePickerSpecJSI>(params);
}
#endif

@end

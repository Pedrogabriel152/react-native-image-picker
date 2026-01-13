#import "ReactNativeImagePicker.h"
#import <React/RCTBridgeModule.h>
#import "ReactNativeImagePicker-Swift.h"

@interface ReactNativeImagePicker ()
@property(nonatomic, strong) RNImagePickerService *service;
@end

@implementation ReactNativeImagePicker
RCT_EXPORT_MODULE()

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
- (void)launchImageLibrary:(NSDictionary *)options
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject
{
  [self.service launchImageLibraryWithOptions:options resolve:resolve reject:reject];
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
- (void)launchCamera:(NSDictionary *)options
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
  [self.service launchCameraWithOptions:options resolve:resolve reject:reject];
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

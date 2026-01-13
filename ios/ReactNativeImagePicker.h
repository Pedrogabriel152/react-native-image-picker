#import <React/RCTBridgeModule.h>
#if RCT_NEW_ARCH_ENABLED
#import <ReactNativeImagePickerSpec/ReactNativeImagePickerSpec.h>
#endif

@interface ReactNativeImagePicker : NSObject <RCTBridgeModule
#if RCT_NEW_ARCH_ENABLED
, NativeReactNativeImagePickerSpec
#endif
>

@end
